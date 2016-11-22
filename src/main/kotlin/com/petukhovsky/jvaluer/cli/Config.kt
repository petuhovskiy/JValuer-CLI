package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.cli.dbObject

/**
 * Created by petuh on 22.11.2016.
 */
data class ConfigBackup(val lang: Array<Lang>, val jValuer: JValuerConfig)
data class JValuerConfig(val trusted: Boolean)

val jValuerConfig = dbObject<JValuerConfig>("jvaluer")
val langConfig = dbObject<Array<Lang>>("lang")

fun backupFromConfig() = ConfigBackup(langConfig.get()!!, jValuerConfig.get()!!)
