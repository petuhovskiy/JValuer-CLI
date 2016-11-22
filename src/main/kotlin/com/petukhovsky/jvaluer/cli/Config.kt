package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.JValuerBuilder
import com.petukhovsky.jvaluer.cli.dbObject
import com.petukhovsky.jvaluer.impl.JValuerImpl
import java.nio.file.Paths

val configDir = Paths.get(".jv/")

data class ConfigBackup(val lang: Array<Lang>, val jvaluer: JValuerConfig)
data class JValuerConfig(val trusted: Boolean)

val jValuerConfig = dbObject<JValuerConfig>("jvaluer")
val langConfig = dbObject<Array<Lang>>("lang")

fun backupFromConfig() = ConfigBackup(langConfig.get()!!, jValuerConfig.get()!!)

val jValuer by lazy { JValuerImpl(langConfig.get()!!.toLanguages(), configDir, null) }