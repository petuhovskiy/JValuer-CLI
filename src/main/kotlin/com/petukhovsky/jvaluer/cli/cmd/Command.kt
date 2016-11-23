package com.petukhovsky.jvaluer.cli.cmd

/**
 * Created by petuh on 19.11.2016.
 */
interface Command {
    fun command(args: Array<String>)
    fun printHelp() {
        throw UnsupportedOperationException("not implemented")
    }
}