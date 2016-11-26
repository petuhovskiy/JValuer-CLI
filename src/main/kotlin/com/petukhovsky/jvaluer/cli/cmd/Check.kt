package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.checker.TokenChecker

object Check : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramOf("-gen", require = true),
                paramOf("-check"),
                paramOf("-out-test"),
                paramOf("-out-wrong"),
                paramOf("-out-ans")
        )
        if (cmd.list.size < 1) {
            println("Must be at least one exe")
        }

        val outTest = getNullablePath(cmd.getOne("-out-test"))
        val outAns = getNullablePath(cmd.getOne("-out-ans"))
        val outWrong = getNullablePath(cmd.getOne("-out-wrong"))

        val genFile = pathJSON(cmd.getOne("-gen")!!)
        if (genFile == null) {
            println("Gen file ${cmd.getOne("-gen")} not found")
            return
        }
        val gen = readJSON<GenScript>(genFile)

        val exeList = mutableListOf<ExeInfo>()
        for (s in cmd.list) {
            val file = pathJSON(s)
            if (file == null) {
                println("File $s not found")
                return
            }
            val exe = readJSON<ExeInfo>(file)
            exeList.add(exe)
        }
        val checker: MyChecker
        val checkerFile = cmd.getOne("-check")
        if (checkerFile == null) {
            checker = MyChainChecker(TokenChecker())
        } else {
            val file = pathJSON(checkerFile)
            if (file == null) {
                println("File $checkerFile not found")
                return
            }
            val exe = readJSON<ExeInfo>(file)
            checker = MyRunnableChecker(exe)
        }
        var test: Long = 0

        loop@while (true) {
            test++
            println()
            println("Test #$test")
            val genArgs = gen.generateArgs()
            val genResult = gen.generate(genArgs, allInfo = false, prefix = "Generator    ")
            val testData = genResult.out
            if (genResult.notSuccess()) {
                testData.copyIfNotNull(outTest)
                println("Args: $genArgs")
                return
            }
            val answerResult = runExe(exeList[0], testData, allInfo = false, prefix = "Model        ")
            val answer = answerResult.out
            if (answerResult.notSuccess()) {
                testData.copyIfNotNull(outTest)
                answer.copyIfNotNull(outAns)
                println("Args: $genArgs")
                return
            }
            for (i in 1..exeList.size - 1) {
                val resultX = runExe(
                        exeList[i], testData, allInfo = false, prefix = String.format("%-13s", "Solution $i")
                )
                val out = resultX.out
                if (resultX.notSuccess()) {
                    testData.copyIfNotNull(outTest)
                    answer.copyIfNotNull(outAns)
                    out.copyIfNotNull(outWrong)
                    println("Args: $genArgs")
                    return
                }
                if (!checker.checkLive(testData, answer, out, prefix = "Checker      ").isCorrect) {
                    testData.copyIfNotNull(outTest)
                    answer.copyIfNotNull(outAns)
                    out.copyIfNotNull(outWrong)
                    println("Args: $genArgs")
                    return
                }
            }
        }
    }

    override fun printHelp() {
        println("usage: jv check <exe-script.1> <exe-script.2>... -gen <gen-script> [-check <exe-script>] "
                + "[-out-test <file>] [-out-ans <file>] [-out-wrong <file>]")
        println()
        println("   -out-test       Copy found test to file")
        println("   -out-ans        Copy test answer to file")
        println("   -out-wrong      Copy wrong test answer to file")
    }

}
