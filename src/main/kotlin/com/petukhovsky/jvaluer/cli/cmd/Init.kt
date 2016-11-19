package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.cli.db.dbObject
import java.nio.file.Files
import java.nio.file.Paths

object Init : Command {
    override fun command(args: Array<String>) {
        val base = Paths.get(".jv")
        if (Files.exists(base)) {
            while (true) {
                print("Store already exists. Overwrite? [Y/n] ")
                if (readYN() ?: continue) break else return
            }
        }
        for (i in args.indices.filter { it > 0 }) {
            val arg = args[i]
            if (!arg.startsWith("--file=")) {
                println("Unknown argument: " + arg)
                return printHelp()
            }
            val file = arg.removePrefix("--file=")
            val path = Paths.get(file)
            if (!Files.exists(path)) return println("File $file doesn't exist")
            val backup = objectMapper.readValue<ConfigBackup>(path.toFile())
            throw UnsupportedOperationException("not implemented") //TODO
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
                println("There is a error in languages. Choose one to delete")
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
        dbObject<Array<Lang>>("lang").save(langs.toTypedArray())
        println("Store successfully initialized")
    }
}

fun readLineBack(): String{
    val line = readLine()!!
    if (line == "~back") throw SkipException()
    return line
}

class SkipException : Exception()

data class ConfigBackup(val lang: Array<Lang>)