package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.cli.cmd.Command

fun Command.parseArgs(args: Array<String>, vararg params: ParserParam, skipFirst: Boolean = true): PrettyArgs {
    var i = if (skipFirst) 1 else 0
    val map = params.associate { Pair(it.name, it) }
    val result = PrettyArgs(map)
    while (i < args.size) {
        val arg = args[i++]
        if (arg.startsWith("-")) {
            if (arg !in map) {
                println("""Unknown argument: "$arg"""")
                printHelp()
                System.exit(0)
            }
            val param = map[arg]!!
            if (param.flag) param.enabled = true
            else {
                if (i >= args.size) {
                    println("Value of argument $arg isn't specified")
                    printHelp()
                    System.exit(0)
                }
                if (!param.stack && param.list.isNotEmpty()) {
                    println("""Too many "$arg" arguments""")
                    printHelp()
                    System.exit(0)
                }
                param.list.add(args[i++])
            }
            continue
        } else result.list.add(arg)
    }
    val bad = params.find { it.require && it.list.isEmpty() }
    if (bad != null) {
        println("""Argument "${bad.name}" is required""")
        printHelp()
        System.exit(0)
    }
    return result
}

fun paramOf(name: String, stack: Boolean = false, require: Boolean = false)
        = ParserParam(name, stack = stack, require = require)

fun paramFlagOf(name: String) = ParserParam(name, flag = true)

class PrettyArgs(val map: Map<String, ParserParam>) {
    val list = mutableListOf<String>()

    fun has(key: String): Boolean {
        return map[key]?.list?.isNotEmpty() ?: false
    }

    fun get(key: String): String {
        return map[key]!!.list[0]
    }

    fun getOne(key: String): String? {
        if (key in map && map[key]!!.list.isNotEmpty()) return map[key]!!.list[0] else return null
    }

    fun getAll(key: String): List<String> {
        return map[key]!!.list
    }

    fun enabled(key: String): Boolean {
        return map[key]?.enabled ?: false
    }
}

class ParserParam(
        val name: String,
        val flag: Boolean = false,
        val stack: Boolean = false,
        val require: Boolean = false
) {

    var enabled = false
    val list = mutableListOf<String>()
}