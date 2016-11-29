package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.*
import java.nio.file.Paths

object Exe : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramOf("-type"),
                paramOf("-lang"),
                paramOf("-tl"),
                paramOf("-ml"),
                paramOf("-i"),
                paramOf("-o")
        )

        if (cmd.list.size != 1) {
            println("Where's the file?")
            printHelp()
            return
        }

        val file = cmd.list[0]

        val info = ExeInfo(
                file,
                FileType.valueOf(cmd.getOne("-type") ?: "auto"),
                cmd.getOne("-lang"),
                cmd.getOne("-tl"),
                cmd.getOne("-ml"),
                cmd.getOne("-i") ?: "stdin",
                cmd.getOne("-o") ?: "stdout"
        )
        println(objectMapper.writeValueAsString(info))
    }

    override fun printHelp() {
        println("usage:")
        println("   jv exe <file> [-type <type>] [-lang <lang_id>] [-tl <time-limit>] [-ml <memory-limit>] " +
                "[-i <in>] [-o <out>]")
        println()
        println("result: ExeInfo json")
    }
}
