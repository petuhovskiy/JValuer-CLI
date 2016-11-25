package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.checker.TokenChecker
import com.petukhovsky.jvaluer.commons.run.RunVerdict
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

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

        val outTest = if (cmd.getOne("-out-test") == null) null else Paths.get(cmd.getOne("-out-test"))
        val outAns = if (cmd.getOne("-out-ans") == null) null else Paths.get(cmd.getOne("-out-ans"))
        val outWrong = if (cmd.getOne("-out-wrong") == null) null else Paths.get(cmd.getOne("-out-wrong"))

        val genFile = pathJSON(cmd.getOne("-gen")!!)
        if (genFile == null) {
            println("Gen file ${cmd.getOne("-gen")} not found")
            return
        }
        val gen = Files.newInputStream(genFile).use {
            objectMapper.readValue<GenScript>(it)
        }

        val exeList = mutableListOf<ExeInfo>()
        for (s in cmd.list) {
            val file = pathJSON(s)
            if (file == null) {
                println("File $s not found")
                return
            }
            val exe = Files.newInputStream(file).use {
                objectMapper.readValue<ExeInfo>(it)
            }
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
            val exe = Files.newInputStream(file).use {
                objectMapper.readValue<ExeInfo>(it)
            }
            checker = MyRunnableChecker(exe)
        }
        var test: Long = 0

        loop@while (true) {
            test++
            println("Test #$test")
            val genArgs = gen.generateArgs()
            println("Generator:")
            val genResult = gen.generate(genArgs, allInfo = false)
            val testData = genResult.out
            if (genResult.run.runVerdict != RunVerdict.SUCCESS) {
                if (outTest != null) Files.copy(testData.path, outTest, StandardCopyOption.REPLACE_EXISTING)
                println("Args: $genArgs")
                return
            }
            println("Model solution:")
            val answerResult = runExe(exeList[0], testData, allInfo = false)
            val answer = answerResult.out
            if (answerResult.run.runVerdict != RunVerdict.SUCCESS) {
                if (outTest != null) Files.copy(testData.path, outTest, StandardCopyOption.REPLACE_EXISTING)
                if (outAns != null) Files.copy(answer.path, outAns, StandardCopyOption.REPLACE_EXISTING)
                println("Args: $genArgs")
                return
            }
            for (i in 1..exeList.size - 1) {
                println("Solution $i:")
                val resultX = runExe(exeList[i], testData, allInfo = false)
                val out = resultX.out
                if (resultX.run.runVerdict != RunVerdict.SUCCESS) {
                    if (outTest != null) Files.copy(testData.path, outTest, StandardCopyOption.REPLACE_EXISTING)
                    if (outAns != null) Files.copy(answer.path, outAns, StandardCopyOption.REPLACE_EXISTING)
                    if (outWrong != null) Files.copy(out.path, outWrong, StandardCopyOption.REPLACE_EXISTING)
                    println("Args: $genArgs")
                    return
                }
                if (!checker.checkLive(testData, answer, out).isCorrect) {
                    if (outTest != null) Files.copy(testData.path, outTest, StandardCopyOption.REPLACE_EXISTING)
                    if (outAns != null) Files.copy(answer.path, outAns, StandardCopyOption.REPLACE_EXISTING)
                    if (outWrong != null) Files.copy(out.path, outWrong, StandardCopyOption.REPLACE_EXISTING)
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
