package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.*
import java.nio.file.Files
import java.nio.file.Paths

object Init : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args, paramOf("--file"))
        if (cmd.list.isNotEmpty()) {
            printHelp()
            return
        }
        val base = Paths.get(".jv")
        if (Files.exists(base)) {
            while (true) {
                print("Store already exists. Overwrite? [Y/n] ")
                if (readYN() ?: continue) break else return
            }
        }
        if (cmd.has("--file")) {
            val path = Paths.get(cmd.get("--file"))
            if (!Files.exists(path)) {
                println("File ${cmd.get("--file")} not found")
                return
            }
            val backup = Files.newInputStream(path).use {
                objectMapper.readValue<ConfigBackup>(it)
            }
            jValuerConfig.save(backup.jValuer)
            langConfig.save(backup.lang)
            println("Success")
            return
        }
        val langs = mutableListOf<Lang>()
        println("Hint: Type ~back if you made a mistake.")
        loop0@ while (true) {
            while (true) {
                print("Do you want to add ${if (langs.isEmpty()) "new language" else "more languages"}? [Y/n] ")
                if (!(readYN() ?: continue)) break
                try {
                    val lang = readLang { readLineBack() }
                    lang.toLanguage()
                    langs.add(lang)
                } catch (e: SkipException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            var wasError = false
            while (true) {
                try {
                    langs.toLanguages()
                    if (!wasError) break@loop0
                    else break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                wasError = true
                println("There is an error in languages. Choose one to delete")
                for (i in langs.indices) {
                    println("[$i] ${objectMapper.writeValueAsString(langs[i])}")
                }
                while (true) {
                    try {
                        print("Enter number to delete: ")
                        val num = readLine()!!.toInt()
                        if (num !in langs.indices) continue
                        langs.removeAt(num)
                        break
                    } catch (e: NumberFormatException) {
                        continue;
                    }
                }
            }
        }
        langConfig.save(langs.toTypedArray())
        jValuerConfig.save(JValuerConfig(true))
        println("Store successfully initialized")
    }

    override fun printHelp() {
        println("Usage:")
        println("jv init [--file <file>]")
        println()
        println("Init config. If the file is specified, then config will be imported from it.")
    }
}

fun readLineBack(): String{
    val line = readLine()!!
    if (line == "~back") throw SkipException()
    return line
}

class SkipException : Exception()