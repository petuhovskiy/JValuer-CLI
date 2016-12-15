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
            |   init        Create a jvaluer storage
            |   backup      Backup config
            |   help        Display this message
            |   run         Run program
            |   script      Execute script
            |   gen         Generate test
            |   check       Check <in> <answer> <out>
            |   checker     Check/compare two or more solutions
            |   tests-check Run all tests in specified directory
            |
            |Use jv help <command> to display help for command
            """.trimMargin())
    }
}
