package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.data.PathData
import com.petukhovsky.jvaluer.commons.invoker.DefaultInvoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.commons.run.RunInOut
import com.petukhovsky.jvaluer.commons.run.RunLimits
import com.petukhovsky.jvaluer.commons.source.Source
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object Run : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramOf("-type"),
                paramOf("-lang"),
                paramOf("-tl"),
                paramOf("-ml"),
                paramOf("-i"),
                paramOf("-o"),
                paramFlagOf("-script")
        )

        val script = buildRunScript(cmd) ?: return
        if (cmd.enabled("-script")) {
            println(objectMapper.writeValueAsString(script))
            return
        }
        script.execute()
    }

    override fun printHelp() {
        println("usage:")
        println("   jv run <file> [-type <type>] [-lang <lang_id>] [-tl <time-limit>] [-ml <memory-limit>] " +
                "[-i <exe:in>] [-o <exe:out>] [-script]")
        println()
        println("Args:")
        println("  -type    'auto', 'exe', 'src'")
        println("  -lang    Find language by id")
        println()
        println("  -tl      Time limit. 1s(second), 2m(minutes), 123ms(milliseconds)")
        println("  -ml      Memory limit. bytes(b), kilobytes(k, kb), mebibytes(m, mib, mb)")
        println()
        println("  -i       Input source. For example, (default) stdin:input.txt means pipe input.txt > stdin")
        println("  -o       Output destination. For example, (default) stdout:output.txt means pipe stdout > output.txt")
        println()
        println("  -script  Produces script without executing")
    }
}

fun parseColon(s: String?): Pair<String, String>? {
    if (s == null) return null
    if (':' !in s) return Pair(s, s)
    val index = s.indexOf(":")
    return Pair(s.substring(0..(index - 1)), s.substring(index + 1))
}

data class RunScript(
        val exe: ExeInfo,
        val `in`: String = "input.txt",
        val out: String? = "output.txt"
) : Script {

    override fun execute() {
        val myExe = exe.toExecutable() ?: return
        val limits = exe.createLimits()

        val result = runExe(
                myExe,
                PathData(exe.path.resolveSibling(this.`in`)),
                limits,
                exe.io
        )
        if (this.out == "stdout") { //TODO
            println("Out:")
            println(result.out.string)
        } else result.out.copyIfNotNull(exe.path.resolveSibling(this.out))
    }
}

fun buildRunScript(cmd: PrettyArgs): RunScript? {
    if (cmd.list.size != 1) {
        println("Enter exactly one file")
        return null
    }
    val file = cmd.list[0]
    val i = parseColon(cmd.getOne("-i"))
    val o = parseColon(cmd.getOne("-o"))
    if (i?.second == "stdin") {
        println("Can't read input data from stdin yet..")
        return null
    }
    return RunScript(
            ExeInfo(
                    file,
                    FileType.valueOf(cmd.getOne("-type") ?: "auto"),
                    cmd.getOne("-lang"),
                    cmd.getOne("-tl"),
                    cmd.getOne("-ml"),
                    i?.first ?: "stdin",
                    o?.first ?: "stdout"
            ),
            i?.second ?: "input.txt",
            o?.second ?: "output.txt"
    )
}