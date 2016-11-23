package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.JValuer
import com.petukhovsky.jvaluer.commons.compiler.CompilationResult
import com.petukhovsky.jvaluer.commons.exe.Executable
import com.petukhovsky.jvaluer.commons.invoker.Invoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.commons.source.Source
import com.petukhovsky.jvaluer.impl.JValuerImpl
import com.petukhovsky.jvaluer.run.RunnerBuilder
import java.nio.file.Files
import java.nio.file.Path

val jValuer by lazy {
    try {
        JValuerImpl(langConfig.get()!!.toLanguages(), configDir.resolve("jvaluer/"), null)
    } catch (e: Exception) {
        println("Can't initialize jValuer. Check your config, run 'jv init' if you still haven't")
        throw e
    }
}

fun createRunnerBuilder(): RunnerBuilder = RunnerBuilder(jValuer).trusted(jValuerConfig.get()!!.trusted)

fun RunnerBuilder.buildSafe(path: Path, lang: Language) = this.buildSafe(path, lang.invoker())
fun RunnerBuilder.buildSafe(path: Path, invoker: Invoker) = this.buildSafe(Executable(path, invoker))!!

fun compileSrc(src: Source, liveProgress: Boolean = true, jvaluer: JValuer = jValuer): MyExecutable {
    println("Compiling...")
    val result = jvaluer.compile(src) //TODO: live progress, watch thread
    return MyExecutable(result.exe, src.language.invoker(), result)
}

class MyExecutable(path: Path?, invoker: Invoker?, val compilation: CompilationResult?) : Executable(path, invoker) {
    fun exists(): Boolean = path != null && !Files.exists(path)
    fun printLog() = compilation?.printLog()
}

fun CompilationResult.printLog() = println("Compilation log: $comment")