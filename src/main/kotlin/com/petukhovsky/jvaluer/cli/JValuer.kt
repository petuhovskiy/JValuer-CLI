package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.JValuer
import com.petukhovsky.jvaluer.commons.compiler.CompilationResult
import com.petukhovsky.jvaluer.commons.data.StringData
import com.petukhovsky.jvaluer.commons.data.TestData
import com.petukhovsky.jvaluer.commons.exe.Executable
import com.petukhovsky.jvaluer.commons.invoker.Invoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.commons.run.InvocationResult
import com.petukhovsky.jvaluer.commons.run.RunInOut
import com.petukhovsky.jvaluer.commons.run.RunLimits
import com.petukhovsky.jvaluer.commons.run.RunVerdict
import com.petukhovsky.jvaluer.commons.source.Source
import com.petukhovsky.jvaluer.impl.JValuerImpl
import com.petukhovsky.jvaluer.run.RunnerBuilder
import com.petukhovsky.jvaluer.run.SafeRunner
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

fun compileSrc(src: Source, liveProgress: Boolean = true): MyExecutable {
    val result: CompilationResult
    if (!liveProgress) {
        println("Compiling...")
        result = jValuer.compile(src)
    } else {
        result = object : LiveProcess<CompilationResult>() {
            override fun update() {
                val passed = (ended ?: now()) - started
                val seconds = passed / 1000
                val ms = passed % 1000
                print("\rCompil")
                if (ended == null) {
                    print("ing")
                    for (i in 0..2) print(if (i < seconds % 4) '.' else ' ')
                } else {
                    print("ed!   ")
                }
                print("  ")
                print("[${seconds}s ${String.format("%03d", ms)}ms]")
            }

            override fun run(): CompilationResult = jValuer.compile(src)
        }.execute()
        println()
    }
    return MyExecutable(result.exe, src.language.invoker(), result)
}

fun runExe(exe: Executable,
           test: TestData = StringData(""),
           limits: RunLimits = RunLimits.unlimited(),
           io: RunInOut = RunInOut.std(),
           liveProgress: Boolean = true
): InvocationResult {
    val runner = createRunnerBuilder().inOut(io).limits(limits).buildSafe(exe)
    return runner.runLive(test, liveProgress)
}

fun SafeRunner.runLive(test: TestData, liveProgress: Boolean = true): InvocationResult {
    fun verdictSign(verdict: RunVerdict): String =
            if (verdict == RunVerdict.SUCCESS) "*" else "X" //TODO: use unicode symbols ✓❌

    fun verdictString(verdict: RunVerdict): String =
            if (verdict == RunVerdict.SUCCESS) "Ok" else verdict.toString()


    val result: InvocationResult
    if (!liveProgress) {
        result = this.run(test)
    } else {
        result = object : LiveProcess<InvocationResult>() {

            val running = listOf("\\", "|", "/", "-")

            override fun update() {
                val time: Long
                val message: String
                print("\r")
                if (ended == null) {
                    time = now() - started
                    print("[${running[(time / 300).toInt() % 4]}]")
                    message = "Running..."
                } else {
                    time = this.result!!.run.userTime
                    print("[${verdictSign(this.result!!.run.runVerdict)}")
                    message = verdictString(this.result!!.run.runVerdict)
                }
                print(String.format(" %-13s ", message))
                print("[${RunLimits.timeString(time)}]")
            }

            override fun run(): InvocationResult = this@runLive.run(test)

        }.execute()
    }
    val run = result.run
    println(String.format(
            "\r[%s] %-13s [%s; %s] %s",
            verdictSign(run.runVerdict),
            verdictString(run.runVerdict),
            run.timeString,
            run.memoryString,
            run.comment
    ))
    return result
}

class MyExecutable(path: Path?, invoker: Invoker?, val compilation: CompilationResult?) : Executable(path, invoker) {
    fun exists(): Boolean = path != null && !Files.exists(path)
    fun printLog() = compilation?.printLog()
}

fun CompilationResult.printLog() = println("Compilation log: $comment")