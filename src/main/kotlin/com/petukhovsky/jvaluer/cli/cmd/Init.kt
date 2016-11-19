package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.Lang
import com.petukhovsky.jvaluer.cli.db.dbObject
import com.petukhovsky.jvaluer.cli.objectMapper
import com.petukhovsky.jvaluer.cli.readLang
import com.petukhovsky.jvaluer.cli.readYN
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
        while (true) {
            print("Want to add ${if (langs.isEmpty()) "language" else "more languages"}? [Y/n] ")
            if (!(readYN() ?: continue)) break
            try {
                langs.add(readLang { readLineBack() })
            } catch (e: SkipException) {continue}
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