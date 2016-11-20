package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.cli.cmd.readLineBack
import com.petukhovsky.jvaluer.commons.compiler.CloneCompiler
import com.petukhovsky.jvaluer.commons.compiler.Compiler
import com.petukhovsky.jvaluer.commons.compiler.RunnableCompiler
import com.petukhovsky.jvaluer.commons.invoker.DefaultInvoker
import com.petukhovsky.jvaluer.commons.invoker.Invoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.commons.lang.Languages
import com.petukhovsky.jvaluer.invoker.CustomInvoker
import com.petukhovsky.jvaluer.lang.LanguagesBuilder
import java.util.*

data class Lang(val name: String,
                val id: String,
                val exts: Array<String>,
                val compiler: Map<String, Any>?,
                val invoker: Map<String, Any>?) {
    fun toLanguage(): Language = Language(name, createCompiler(), createInvoker())

    private fun createCompiler(): Compiler = if (compiler == null) CloneCompiler() else
        when (compiler["type"]) {
            "clone" -> CloneCompiler()
            "runnable" ->
                if ("timeout" in compiler) RunnableCompiler(compiler["exe"] as String, compiler["pattern"] as String, compiler["timeout"] as Int)
                else RunnableCompiler(compiler["exe"] as String, compiler["pattern"] as String)
            else -> throw RuntimeException("unknown compiler type")
        }

    private fun createInvoker(): Invoker = if (invoker == null) DefaultInvoker() else
        when (invoker["type"]) {
            "default" -> DefaultInvoker()
            "custom" -> CustomInvoker(invoker["exe"] as String, invoker["pattern"] as String)
            else -> throw RuntimeException("unknown invoker type")
        }

    override fun equals(other: Any?): Boolean = other is Lang
            && name == other.name
            && id == other.id
            && Arrays.equals(exts, other.exts)
            && compiler == other.compiler
            && invoker == other.invoker

    override fun hashCode(): Int {
        return Objects.hash(name, id, exts, compiler, invoker)
    }
}

fun List<Lang>.toLanguages() : Languages {
    val builder = LanguagesBuilder()
    for (it in this) {
        builder.addLanguage(it.toLanguage(), it.exts, arrayOf(it.id))
    }
    return builder.build()
}

fun readLang(nextLine: () -> String = {readLine()!!}): Lang {
    print("Language full name: ")
    val name = nextLine()
    print("Language id: ")
    val id = nextLine()
    print("Language sources extentions(separate with spaces): ")
    val exts = nextLine().split(" ").toTypedArray()
    val compiler = readCompiler(nextLine)
    val invoker = readInvoker(nextLine)
    return Lang(name, id, exts, compiler, invoker)
}

fun readCompiler(nextLine: () -> String = {readLine()!!}): Map<String, Any>? {
    while (true) {
        print("Compiler type[Clone/runnable]: ")
        val line = nextLine()
        if (line.isEmpty()) return null
        if (line.equals("clone", true)) return mapOf(Pair("type", "clone"))
        if (line.equals("runnable", true)) break
        println("Unknown compiler type. Type 'clone' or 'runnable'")
    }
    print("Compiler executable: ")
    val executable = nextLine()
    print("Compiler pattern: ")
    val pattern = nextLine()
    print("Compiler timeout in seconds: ")
    val timeout = nextLine()
    val result = mutableMapOf<String, Any>(Pair("exe", executable), Pair("pattern", pattern), Pair("type", "runnable"))
    try {
        result.put("timeout", Integer.parseInt(timeout))
    } catch (e: NumberFormatException) {}
    return result
}

fun readInvoker(nextLine: () -> String = {readLine()!!}): Map<String, Any>? {
    while (true) {
        print("Invoker type[Default/custom]: ")
        val line = nextLine()
        if (line.isEmpty()) return null
        if (line.equals("default", true)) return mapOf(Pair("type", "default"))
        if (line.equals("custom", true)) break
        println("Unknown invoker type. Type 'default' or 'custom'")
    }
    print("Invoker executable: ")
    val executable = nextLine()
    print("Invoker pattern: ")
    val pattern = nextLine()
    return mapOf(Pair("exe", executable), Pair("pattern", pattern), Pair("type", "custom"))
}
