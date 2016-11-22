package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.compiler.CompilationResult
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.commons.run.RunLimits
import java.nio.file.Path
import java.nio.file.Paths

object Run : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramFlagOf("-auto"),
                paramFlagOf("-exe"),
                paramOf("-lang"),
                paramOf("-tl"),
                paramOf("-ml"),
                paramOf("-i"),
                paramOf("-o")
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

        val exe: Path
        if (lang != null) {
            val result = jValuer.compile(lang, file)
            println("Compilation log: " + result.comment)
            if (!result.isSuccess) {
                println("Compilation failed.")
                return
            }
            exe = result.exe
        }

        val limits = RunLimits.of(cmd.getOne("-tl"), cmd.getOne("-ml"))
        var inr = parseColon(cmd.getOne("-i")) ?: Pair("stdin", "input.txt")
        var outr = parseColon(cmd.getOne("-o")) ?: Pair("stdout", "output.txt")

        if (inr.second == "stdin") inr = Pair(inr.first, "input.txt") //TODO
        
        //TODO run
    }

    override fun printHelp() {
        println("Usage:")
        println(" jv <file> ( [-auto] | [-exe] | [-lang <lang_id>] ) \\")
        println("[-tl <time-limit>] [-ml <memory-limit>] \\")
        println("[-i <exe:in>] [-o <exe:out>] [-copy-exe <dest>]")
        println()
        println()
    }
}

fun parseColon(s: String?): Pair<String, String>? {
    if (s == null) return null
    if (':' !in s) return Pair(s, s)
    val index = s.indexOf(":")
    return Pair(s.substring(0..(index - 1)), s.substring(index + 1))
}