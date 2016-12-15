package com.petukhovsky.jvaluer.cli

import com.fasterxml.jackson.annotation.JsonIgnore
import com.petukhovsky.jvaluer.JValuer
import com.petukhovsky.jvaluer.commons.builtin.JValuerBuiltin
import com.petukhovsky.jvaluer.commons.checker.Checker
import com.petukhovsky.jvaluer.commons.compiler.CompilationResult
import com.petukhovsky.jvaluer.commons.data.StringData
import com.petukhovsky.jvaluer.commons.data.TestData
import com.petukhovsky.jvaluer.commons.exe.Executable
import com.petukhovsky.jvaluer.commons.gen.Generator
import com.petukhovsky.jvaluer.commons.invoker.DefaultInvoker
import com.petukhovsky.jvaluer.commons.invoker.Invoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.commons.lang.Languages
import com.petukhovsky.jvaluer.commons.run.*
import com.petukhovsky.jvaluer.commons.source.Source
import com.petukhovsky.jvaluer.impl.JValuerImpl
import com.petukhovsky.jvaluer.lang.LanguagesImpl
import com.petukhovsky.jvaluer.run.RunnerBuilder
import com.petukhovsky.jvaluer.run.SafeRunner
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Instant

class DefInvokerBuiltin(
        val invoker: Invoker,
        val chain: JValuerBuiltin
) : JValuerBuiltin {
    override fun checker(s: String?): Checker = chain.checker(s)

    override fun generator(s: String?): Generator = chain.generator(s)

    override fun invoker(s: String?): Invoker = if (s == "default") invoker else chain.invoker(s)

}

class MyJValuer(
        languages: Languages,
        path: Path,
        forceInvoker: String?
) : JValuerImpl(languages, path, null) {

    val myBuiltin by lazy {
        if (forceInvoker == null) super.builtin()
        else {
            DefInvokerBuiltin(
                    super.builtin()
                            .invoker(forceInvoker) ?: throw NullPointerException("runexe invoker not supported"),
                    super.builtin()
            )
        }
    }

    override fun builtin(): JValuerBuiltin = myBuiltin

    override fun invokeDefault(runOptions: RunOptions?): RunInfo {
        val def = myBuiltin.invoker("default") ?: throw UnsupportedOperationException("can't find default invoker")
        return invoke(def, runOptions)
    }
}

val jValuer by lazy {
    try {
        val config = jValuerConfig.get()!!
        if (config.forceInvoker == null) {
            JValuerImpl(langConfig.get()!!.toLanguages(), configDir.resolve("jvaluer/"), null)
        } else {
            MyJValuer(langConfig.get()!!.toLanguages(), configDir.resolve("jvaluer/"), config.forceInvoker)
        }
    } catch (e: Exception) {
        println("Can't initialize jValuer. Check your config, run 'jv init' if you still haven't")
        throw e
    }
}

fun createRunnerBuilder(): RunnerBuilder = RunnerBuilder(jValuer).trusted(jValuerConfig.get()!!.trusted)

fun RunnerBuilder.buildSafe(path: Path, lang: Language) = this.buildSafe(path, lang.invoker())
fun RunnerBuilder.buildSafe(path: Path, invoker: Invoker) = this.buildSafe(Executable(path, invoker))!!

fun compileSrc(src: Source, liveProgress: Boolean = true, allInfo: Boolean = true): MyExecutable {
    val dbSrc = dbSource(src)
    val srcInfo = dbSrc.get()!!
    if (srcInfo.exe != null) {
        if (allInfo) println("Already compiled at ${srcInfo.compiled}")
        return MyExecutable(getObject(srcInfo.exe!!), src.language.invoker(), null, true)
    }
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
    if (result.isSuccess) {
        srcInfo.compiled = Instant.now()
        srcInfo.exe = saveObject(result.exe)
        dbSrc.save(srcInfo)
    }
    return MyExecutable(result.exe, src.language.invoker(), result, result.isSuccess)
}

fun runExe(exe: Executable,
           test: TestData = StringData(""),
           limits: RunLimits = RunLimits.unlimited(),
           io: RunInOut = RunInOut.std(),
           args: String? = null,
           liveProgress: Boolean = true,
           prefix: String = ""
): InvocationResult {
    val runner = createRunnerBuilder().inOut(io).limits(limits).buildSafe(exe)
    return runner.runLive(test, args, liveProgress, prefix = prefix)
}

fun runExe(exe: ExeInfo,
           test: TestData = StringData(""),
           args: String? = null,
           liveProgress: Boolean = true,
           allInfo: Boolean = true,
           prefix: String = ""
) =
    runExe(
            exe.toExecutable(allInfo = allInfo, liveProgress = liveProgress)!!,
            test,
            exe.createLimits(),
            exe.io,
            args,
            liveProgress,
            prefix = prefix
    )

fun SafeRunner.runLive(
        test: TestData,
        args: String? = null,
        liveProgress: Boolean = true,
        prefix: String = "",
        ln: Boolean = true
): InvocationResult {
    fun verdictSign(verdict: RunVerdict): String =
            if (verdict == RunVerdict.SUCCESS) ui.okSign else ui.wrongSign

    fun verdictString(verdict: RunVerdict): String =
            if (verdict == RunVerdict.SUCCESS) "Ok" else verdict.toString()


    val result: InvocationResult
    val argsArr = if (args == null) arrayOf() else arrayOf(args)
    if (!liveProgress) {
        result = this.run(test, *argsArr)
    } else {
        result = object : LiveProcess<InvocationResult>() {

            val running = listOf("\\", "|", "/", "-")

            override fun update() {
                val time: Long
                val message: String
                print("\r$prefix")
                if (ended == null) {
                    time = now() - started
                    print("[${running[(time / 300).toInt() % 4]}]")
                    message = "Running..."
                } else {
                    time = this.result!!.run.userTime
                    print("[${verdictSign(this.result!!.run.runVerdict)}]")
                    message = verdictString(this.result!!.run.runVerdict)
                }
                print(String.format(" %-13s ", message))
                if (ended == null) {
                    print("[${RunLimits.timeString(time)}]")
                } else {
                    val run = this.result!!.run
                    print(run.shortInfo)
                }
            }

            override fun run(): InvocationResult = this@runLive.run(test, *argsArr)

        }.execute()
    }
    if (ln) println()
    return result
}

class MyExecutable(path: Path?, invoker: Invoker?, val compilation: CompilationResult?, val compilationSuccess: Boolean) : Executable(path, invoker) {
    fun exists(): Boolean = path != null && !Files.exists(path)
    fun printLog() = compilation?.printLog()
}

fun CompilationResult.printLog() = println("Compilation log: $comment")

enum class FileType {
    src,
    exe,
    auto
}

class ExeInfo(
        val file: String,
        val type: FileType,
        val lang: String?,
        val tl: String?,
        val ml: String?,
        val `in`: String,
        val out: String
) : Script() {
    fun createLimits(): RunLimits = RunLimits.of(tl, ml)

    val path: Path
        @JsonIgnore get() = resolve(file)

    val io: RunInOut
        @JsonIgnore get() = RunInOut(`in`, out)

    val name: String
        @JsonIgnore get() = path.fileName.toString()

    fun toExecutable(
            liveProgress: Boolean = true,
            allInfo: Boolean = true
    ): MyExecutable? {
        val type: FileType
        val lang: Language?
        when (this.type) {
            FileType.auto -> {
                lang = jValuer.languages.findByName(this.lang) ?: jValuer.languages.findByPath(path)
                if (lang != null) {
                    if (allInfo) println("Detected language: ${lang.name()}")
                    type = FileType.src
                } else {
                    println("Language not found. Assuming file is exe")
                    type = FileType.exe
                }
            }
            FileType.exe -> {
                lang = jValuer.languages.findByName(this.lang)
                type = FileType.exe
            }
            FileType.src -> {
                type = FileType.src
                lang = jValuer.languages.findByName(this.lang)
                if (lang == null) {
                    println("Language ${this.lang} not found")
                    println("Available languages: " + langConfig.get()!!.map(Lang::id).toString())
                    return null
                }
            }
        }

        assert(type != FileType.auto) { "Wat, report to GitHub." }

        val exe: MyExecutable
        if (type == FileType.src) {
            exe = compileSrc(Source(path, lang), allInfo = allInfo, liveProgress = liveProgress)
            exe.printLog()
            if (!exe.compilationSuccess) {
                println("Compilation failed")
                return null
            }
        } else {
            exe = MyExecutable(path, if (lang == null) DefaultInvoker() else lang.invoker(), null, true)
        }
        return exe
    }

    override fun execute() {
        toExecutable()
    }
}

fun TestData.copyIfNotNull(path: Path?) {
    Files.copy(this.path, path ?: return, StandardCopyOption.REPLACE_EXISTING)
}

fun InvocationResult.isSuccess() = this.run.runVerdict == RunVerdict.SUCCESS
fun InvocationResult.notSuccess() = !this.isSuccess()

val RunInfo.shortInfo: String
        get() = "[$timeString; $memoryString]"