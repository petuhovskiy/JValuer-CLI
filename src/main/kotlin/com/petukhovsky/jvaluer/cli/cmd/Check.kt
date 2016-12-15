package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.data.PathData
import java.nio.file.Paths

object Check : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args)
        val checker = MyRunnableChecker(readScript<ExeInfo>(cmd.list[0]))
        val dataList = cmd.list.drop(1).map { PathData(Paths.get(it)) }
        checker.checkLive(dataList[0], dataList[1], dataList[2])
    }

    override fun printHelp() {
        println("Usage: jv check <check-exe> <in> <answer> <out>")
    }

}