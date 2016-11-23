package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.compiler.CompilationResult
import com.petukhovsky.jvaluer.commons.data.PathData
import com.petukhovsky.jvaluer.commons.exe.Executable
import com.petukhovsky.jvaluer.commons.invoker.DefaultInvoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.commons.run.RunInOut
import com.petukhovsky.jvaluer.commons.run.RunLimits
import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object Run : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramFlagOf("-auto"),
                paramFlagOf("-exe"),
                paramOf("-lang"),
                paramOf("-tl"),
                paramOf("-ml"),
                paramOf("-i"),
                paramOf("-o"),
                paramOf("-script")
        )

        if (cmd.list.size != 1) {
            println("Enter exactly one file")
            return
        }
        val file = Paths.get(cmd.list[0])

        var mask = 0
        val lang: Language?
        if (cmd.enabled("-auto")) mask = mask or 1
        if (cmd.enabled("-exe")) mask = mask or 2
        if (cmd.has("-lang")) mask = mask or 4
        when (mask) {
            0, 1 -> {
                lang = jValuer.languages.findByPath(file)
                if (lang != null) {
                    println("Detected language: ${lang.name()}")
                } else {
                    println("Language not found. Assuming file is exe")
                }
            }
            2 -> lang = null
            4 -> {
                lang = jValuer.languages.findByName(cmd.get("-lang"))
                if (lang == null) {
                    println("Language ${cmd.get("-lang")} not found")
                    println("All languages: " + langConfig.get()!!.map(Lang::id).toString())
                    return
                }
            }
            else -> {
                println("Use -auto OR -exe OR -lang OR neither")
                printHelp()
                return
            }
        }

        val exe: Executable
        if (lang != null) {
            val result = jValuer.compile(lang, file)
            println("Compilation log: " + result.comment)
            if (!result.isSuccess) {
                println("Compilation failed.")
                return
            }
            exe = lang.createExecutable(result.exe)
        } else {
            exe = Executable(file, DefaultInvoker())
        }

        val limits = RunLimits.of(cmd.getOne("-tl"), cmd.getOne("-ml"))
        var inr = parseColon(cmd.getOne("-i")) ?: Pair("stdin", "input.txt")
        var outr = parseColon(cmd.getOne("-o")) ?: Pair("stdout", "output.txt")

        if (inr.second == "stdin") inr = Pair(inr.first, "input.txt") //TODO

        val runner = createRunnerBuilder()
                .limits(limits).inOut(RunInOut(inr.first, outr.first)).buildSafe(exe)

        val result = runner.run(PathData(file.resolveSibling(inr.second)))
        if (outr.second == "stdout") {
            println(result.out.string)
        } else {
            Files.copy(result.out.path, file.resolveSibling(outr.second), StandardCopyOption.REPLACE_EXISTING)
        }
        println(result.run)
    }

    override fun printHelp() {
        println("Usage:")
        println("   jv run <file> ( [-auto] | [-exe] | [-lang <lang_id>] ) [-tl <time-limit>] [-ml <memory-limit>] " +
                "[-i <exe:in>] [-o <exe:out>] [-script]")
        println()
        println("Args:")
        println("  -auto    Try to resolve language by source extension. On by default")
        println("  -exe     Execute file as compiled binary")
        println("  -lang    Find language by id and compile source")
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