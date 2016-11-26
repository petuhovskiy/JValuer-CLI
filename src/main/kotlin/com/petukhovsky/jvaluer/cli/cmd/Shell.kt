package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.commandByName
import java.util.*

object Shell : Command {
    override fun command(args: Array<String>) {
        while (true) {
            print("> ")
            val s = readLine() ?: return
            if (s.trim() == "exit") return
            val arr = parseArgs(s)

            println(Arrays.toString(arr))
            if (arr.isEmpty()) return Help.command(arr)
            if (arr[0] == "init") {
                println("Can't invoke 'init' from shell")
                continue
            }
            commandByName(arr[0]).command(arr)
        }
    }

    fun parseArgs(s: String): Array<String> {
        val list = mutableListOf<String>()
        var i = 0
        val buf = StringBuffer()
        var escape = 1
        while (i < s.length) {
            val slash = buf.isNotEmpty() && buf.endsWith("\\")
            if (s[i] == '"') {
                if (slash) {
                    buf.deleteCharAt(buf.length - 1)
                    buf.append(s[i])
                } else escape = escape xor 1 or 2
                i++
                continue
            }
            if (s[i] == ' ') {
                if (escape and 1 == 1) {
                    list.add(buf.toString())
                    buf.setLength(0)
                    escape = 1
                } else buf.append(s[i])
                i++
                continue
            }
            buf.append(s[i++])
        }
        if (escape != 1 || buf.isNotEmpty()) list.add(buf.toString())
        return list.toTypedArray()
    }

    override fun printHelp() {
        println("interactive shell")
    }
}