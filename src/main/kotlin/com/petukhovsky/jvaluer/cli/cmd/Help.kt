package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.commandByName

object Help : Command {
    override fun command(args: Array<String>) {
        if (args.size <= 1) println("""
            |usage: jv <command> [<args>]
            |
            |jv commands:
            |
            |   init    Create a jvaluer storage
            |   run     Execute a binary
            |   printHelp    Display this message
            """.trimMargin())
        else {
            if (args.size == 2 && commandByName(args[1]) != UnknownCommand) {
                commandByName(args[1]).printHelp()
            } else printHelp()
        }
    }

    override fun printHelp() {
        println("""
            |usage: jv help [<command>]
            """.trimMargin())
    }
}
