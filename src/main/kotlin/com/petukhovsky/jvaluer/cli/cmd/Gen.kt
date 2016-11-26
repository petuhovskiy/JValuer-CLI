package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.*
import com.petukhovsky.jvaluer.commons.run.InvocationResult
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

object Gen : Command {
    override fun command(args: Array<String>) {
        val cmd = parseArgs(args,
                paramOf("-t"),
                paramOf("-out"),
                paramOf("-D", stack = true),
                paramOf("-Dl", stack = true),
                paramFlagOf("-script")
        )
        if (cmd.list.size != 1) {
            println("Where's exe-script?")
            printHelp()
            return
        }
        val file = pathJSON(cmd.list[0])
        if (file == null) {
            println("File not found")
            return
        }
        val exe = readJSON<ExeInfo>(file)
        val map = mutableMapOf<String, Any>()
        for (s in cmd.getAll("-D")) {
            val index = s.indexOf(":")
            if (index == -1) {
                println("Unrecognized key:value(missing colon) => $s")
                return
            }
            val key = s.substring(0, index)
            val value = s.substring(index + 1)
            if (key in map) {
                println("WARN: $key key overridden")
            }
            map[key] = value
        }
        for (s in cmd.getAll("-Dl")) {
            val index = s.indexOf(":")
            if (index == -1) {
                println("Unrecognized key:value(missing colon) => $s")
                return
            }
            val key = s.substring(0, index)
            val value = s.substring(index + 1).toLong()
            if (key in map) {
                println("WARN: $key key overridden")
            }
            map[key] = value
        }
        val script = GenScript(
                exe,
                cmd.getOne("-t") ?: """${'$'}{test} ${'$'}{time}""",
                cmd.getOne("-out"),
                map
        )
        if (cmd.enabled("-script")) {
            println(objectMapper.writeValueAsString(script))
            return
        }
        script.execute()
    }

    override fun printHelp() {
        println("usage: jv gen <exe-script> [-t <template>] [-out <file>] [-D(l) <key:value>]... [-script]")
        println()
        println("   -D      Stackable. example: -D key1:abacaba -Dl key2:42 -D p:qwe")
        println("   -Dl     As D, but long type")
    }

}

data class GenScript(
        val exe: ExeInfo,
        val template: String = """${'$'}{test} ${'$'}{time}""",
        val out: String? = null,
        val map: Map<String, Any> = mapOf()
) : Script {
    override fun execute() {
        val result = generate()
        if (out == "stdout") {
            println("Out: ")
            println(result.out.string)
        } else result.out.copyIfNotNull(Paths.get(out))
    }

    fun generateArgs(test: Long = 1, pushTime: Boolean = true): String = processTemplate(
            template,
            HashMap(map).apply {
                if ("test" !in this) this["test"] = test
                if (pushTime && "time" !in this) this["time"] = System.currentTimeMillis()
            }
    )

    fun generate(
            args: String = generateArgs(),
            allInfo: Boolean = true,
            prefix: String = ""
    ): InvocationResult {
        return runExe(exe, args = args, allInfo = allInfo, prefix = prefix)
    }

}