package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.annotation.JsonIgnore
import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.checker.TokenChecker
import java.nio.file.Path

object Checker : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramOf("-gen", require = true),
                paramOf("-check"),
                paramOf("-out-test"),
                paramOf("-out-wrong"),
                paramOf("-out-ans"),
                paramFlagOf("-script")
        )
        if (cmd.list.size < 1) {
            println("Must be at least one exe")
        }

        val model = readScript<ExeInfo>(cmd.list[0])
        val gen = readScript<GenScript>(cmd.get("-gen"))

        val arr = (1..cmd.list.size - 1).mapTo(mutableListOf<ExeInfo>()) {
            readScript<ExeInfo>(cmd.list[it])
        }.toTypedArray()

        val check = cmd.getOne("-check")?.let { readScript<ExeInfo>(it) }

        val script = CheckerScript(
                model, arr, gen, check,
                OutInfo(
                        cmd.getOne("-out-test"),
                        cmd.getOne("-out-wrong"),
                        cmd.getOne("-out-ans")
                )
        )

        if (cmd.enabled("-script")) {
            println(objectMapper.writeValueAsString(script))
            return
        }

        script.execute()
    }

    override fun printHelp() {
        println("usage: jv checker <exe-script.1> <exe-script.2>... -gen <gen-script> [-check <exe-script>] "
                + "[-out-test <file>] [-out-ans <file>] [-out-wrong <file>] [-script]")
        println()
        println("   -out-test       Copy found test to file")
        println("   -out-ans        Copy test answer to file")
        println("   -out-wrong      Copy wrong test answer to file")
    }

}

class CheckerScript(
        val model: ExeInfo,
        val exe: Array<ExeInfo>,
        val gen: GenScript,
        val check: ExeInfo?,
        val out: OutInfo
) : Script() {
    override fun execute() {
        val checker: MyChecker =
                if (check != null) MyRunnableChecker(check)
                else MyChainChecker(TokenChecker())
        var test: Long = 0

        loop@while (true) {
            test++
            println()
            println("Test #$test")
            val genArgs = gen.generateArgs()
            val genResult = gen.generate(genArgs, allInfo = false, prefix = "Generator    ")
            val testData = genResult.out
            if (genResult.notSuccess()) {
                testData.copyIfNotNull(out.path.test)
                println("Args: $genArgs")
                return
            }
            val answerResult = runExe(model, testData, allInfo = false, prefix = "Model        ")
            val answer = answerResult.out
            if (answerResult.notSuccess()) {
                testData.copyIfNotNull(out.path.test)
                answer.copyIfNotNull(out.path.ans)
                println("Args: $genArgs")
                return
            }
            for (i in exe.indices) {
                val result = runAndCheck(exe[i], Test(null, testData, answer), checker, allInfo = false, prefix = String.format("%-13s", "Solution $i"))
                val out = result.invocation?.out
                if (result.notCorrect()) {
                    testData.copyIfNotNull(this.out.path.test)
                    answer.copyIfNotNull(this.out.path.ans)
                    out?.copyIfNotNull(this.out.path.wrong)
                    println("Args: $genArgs")
                    return
                }
            }
        }
    }

    override fun applyLocation(dir: Path?) {
        super.applyLocation(dir)
        model.applyLocation(dir)
        exe.forEach { it.applyLocation(dir) }
        gen.applyLocation(dir)
        check?.applyLocation(dir)
    }
}

data class OutInfo(
        val test: String?,
        val wrong: String?,
        val ans: String?
) {
    val path: OutPath
        @JsonIgnore get() = OutPath(
            getNullablePath(test),
            getNullablePath(wrong),
            getNullablePath(ans)
        )
}

data class OutPath(
        val test: Path?,
        val wrong: Path?,
        val ans: Path?
)
