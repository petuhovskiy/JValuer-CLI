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
                paramFlagOf("-auto"),
                paramFlagOf("-exe"),
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

object RunScriptCmd : Command {
    override fun command(args: Array<String>) {
        if (args.size != 2) return printHelp()
        val file = Paths.get(args[1])
        val script = Files.newInputStream(file).use { objectMapper.readValue<RunScript>(it) }
        script.execute()
    }

    override fun printHelp() {
        println("Usage: jv run-script <script-to-execute>")
        println()
        println("You can generate script with command jv run ... -script")
        println("Enter 'jv help run' to learn more about it")
    }
}

fun parseColon(s: String?): Pair<String, String>? {
    if (s == null) return null
    if (':' !in s) return Pair(s, s)
    val index = s.indexOf(":")
    return Pair(s.substring(0..(index - 1)), s.substring(index + 1))
}

enum class FileType {
    src,
    exe,
    auto
}

data class MyPipe(
        val exe: String,
        val file: String?
)

data class RunScript(
        val file: String,
        val type: FileType,
        val lang: String?,
        val tl: String?,
        val ml: String?,
        val `in`: MyPipe,
        val out: MyPipe
) : Script {

    fun createLimits(): RunLimits = RunLimits.of(tl, ml)

    override fun execute() {
        val file = Paths.get(this.file)

        val type: FileType
        val lang: Language?
        when (this.type) {
            FileType.auto -> {
                lang = jValuer.languages.findByName(this.lang) ?: jValuer.languages.findByPath(file)
                if (lang != null) {
                    println("Detected language: ${lang.name()}")
                    type = FileType.src
                } else {
                    println("Language not found. Assuming file is exe")
                    type = FileType.exe
                }
            }
            FileType.exe -> {
                lang = jValuer.languages.findByName(this.lang)
                type = FileType.exe
            }
            FileType.src -> {
                type = FileType.src
                lang = jValuer.languages.findByName(this.lang)
                if (lang == null) {
                    println("Language ${this.lang} not found")
                    println("Available languages: " + langConfig.get()!!.map(Lang::id).toString())
                    return
                }
            }
        }

        assert(type != FileType.auto) { "Wat, report to GitHub." }

        val exe: MyExecutable
        if (type == FileType.src) {
            exe = compileSrc(Source(file, lang))
            exe.printLog()
            if (exe.compilation == null || !exe.compilation.isSuccess) {
                println("Compilation failed")
                return
            }
        } else {
            exe = MyExecutable(file, if (lang == null) DefaultInvoker() else lang.invoker(), null)
        }

        val limits = this.createLimits()

        val result = runExe(
                exe,
                PathData(file.resolveSibling(this.`in`.file)),
                limits,
                RunInOut(this.`in`.exe, this.out.exe)
        )
        if (this.out.file == "stdout") { //TODO
            println("Out:")
            println(result.out.string)
        } else {
            Files.copy(result.out.path, file.resolveSibling(this.out.file), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

fun buildRunScript(cmd: PrettyArgs): RunScript? {
    if (cmd.list.size != 1) {
        println("Enter exactly one file")
        return null
    }
    val file = cmd.list[0]
    val autoFlag = cmd.enabled("-auto")
    val exeFlag = cmd.enabled("-exe")
    val lang = cmd.getOne("-lang")
    if (autoFlag && exeFlag) {
        println("-auto or -exe is redundant")
        return null
    }
    val type = if (lang != null) FileType.src else if (exeFlag) FileType.exe else FileType.auto
    val tl = cmd.getOne("-tl")
    val ml = cmd.getOne("-ml")
    val i = parseColon(cmd.getOne("-i"))
    val o = parseColon(cmd.getOne("-o"))
    if (i?.second == "stdin") {
        println("Can't read input data from stdin yet..")
        return null
    }
    val `in` = if (i == null) MyPipe("stdin", "input.txt") else MyPipe(i.first, i.second)
    val out = if (o == null) MyPipe("stdout", "output.txt") else MyPipe(o.first, o.second)
    return RunScript(file, type, lang, tl, ml, `in`, out)
}