package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.JValuerBuilder
import com.petukhovsky.jvaluer.cli.dbObject
import com.petukhovsky.jvaluer.commons.invoker.Invoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.impl.JValuerImpl
import com.petukhovsky.jvaluer.run.RunnerBuilder
import java.nio.file.Path
import java.nio.file.Paths
import com.petukhovsky.jvaluer.commons.exe.Executable

val configDir = Paths.get(".jv/")

data class ConfigBackup(val lang: Array<Lang>, val jvaluer: JValuerConfig)
data class JValuerConfig(val trusted: Boolean)

val jValuerConfig = dbObject<JValuerConfig>("jvaluer")
val langConfig = dbObject<Array<Lang>>("lang")

fun backupFromConfig() = ConfigBackup(langConfig.get()!!, jValuerConfig.get()!!)

val jValuer by lazy { JValuerImpl(langConfig.get()!!.toLanguages(), configDir.resolve("jvaluer/"), null) }

fun createRunnerBuilder(): RunnerBuilder = RunnerBuilder(jValuer).trusted(jValuerConfig.get()!!.trusted)

fun RunnerBuilder.buildSafe(path: Path, lang: Language) = this.buildSafe(path, lang.invoker())
fun RunnerBuilder.buildSafe(path: Path, invoker: Invoker) = this.buildSafe(Executable(path, invoker))!!