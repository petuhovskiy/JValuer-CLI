package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.commandByName

object Help : Command {
    override fun command(args: Array<String>) {
        if (args.size == 2) {
            commandByName(args[1]).printHelp()
        } else printHelp()
    }

    override fun printHelp() {
        println("""
            |usage: jv <command> [<args>]
            |
            |jv commands:
            |
            |   init    Create a jvaluer storage
            |   run     Execute a binary
            |   help    Display this message
            |   run     Run program
            |
            |Use jv help <command> to display help for command
            """.trimMargin())
    }
}
