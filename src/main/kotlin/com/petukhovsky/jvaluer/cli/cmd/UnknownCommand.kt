package com.petukhovsky.jvaluer.cli.cmd

object UnknownCommand : Command {
    override fun command(args: Array<String>) {
        println("Unknown command. Type 'jv help' to display help")
    }

    override fun printHelp() {
        Help.printHelp()
    }
}
