package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.annotation.JsonIgnore
import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.checker.TokenChecker
import java.nio.file.Path

object TestsCheck : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramOf("-check"),
                paramOf("-dir", require = true),
                paramFlagOf("-script")
        )

        val script = TestsCheckScript(
                cmd.get("-dir"),
                cmd.getOne("-check")?.let { readScript<ExeInfo>(it) },
                cmd.list.map { readScript<ExeInfo>(it) }.toTypedArray()
        )
        if (cmd.enabled("-script")) {
            println(objectMapper.writeValueAsString(script))
            return
        }
        script.execute()
    }

    override fun printHelp() {
        println("Usage: jv tests-checker -dir <tests-dir> [-check <checker-exe>]")
    }
}

class TestsCheckScript(
        val dir: String,
        val check: ExeInfo?,
        val solutions: Array<ExeInfo>
) : Script() {

    val dirPath: Path
        @JsonIgnore get() = resolve(dir)

    override fun execute() {
        val checker = if (check == null) MyChainChecker(TokenChecker()) else MyRunnableChecker(check)
        val tests = findAllTests(dirPath)
        for (solution in solutions) {
            println("${solution.name}:")
            var passed = 0
            var all = 0
            for (test in tests) {
                all++
                val name = String.format("%-10s", "Test " + (test.name ?: "???"))
                val result = runAndCheck(solution, test, checker, allInfo = false, prefix = name)
                if (result.isCorrect()) passed++
            }
            println("Passed $passed/$all (${passed * 100.0 / all}%)")
            println()
        }
    }

    override fun applyLocation(dir: Path?) {
        super.applyLocation(dir)
        check?.applyLocation(dir)
        solutions.forEach { it.applyLocation(dir) }
    }
}