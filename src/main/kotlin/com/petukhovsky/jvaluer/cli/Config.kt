package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.JValuer
import com.petukhovsky.jvaluer.JValuerBuilder
import com.petukhovsky.jvaluer.cli.db
import com.petukhovsky.jvaluer.commons.invoker.Invoker
import com.petukhovsky.jvaluer.commons.lang.Language
import com.petukhovsky.jvaluer.impl.JValuerImpl
import com.petukhovsky.jvaluer.run.RunnerBuilder
import java.nio.file.Path
import java.nio.file.Paths
import com.petukhovsky.jvaluer.commons.exe.Executable

val configDir = Paths.get(".jv/")

data class ConfigBackup(
        val lang: Array<Lang>,
        val jvaluer: JValuerConfig = JValuerConfig()
)
data class JValuerConfig(
        val trusted: Boolean = true,
        val forceInvoker: String? = null
)

val jValuerConfig = db<JValuerConfig>("jvaluer")
val langConfig = db<Array<Lang>>("lang")

fun backupFromConfig() = ConfigBackup(langConfig.get()!!, jValuerConfig.get()!!)