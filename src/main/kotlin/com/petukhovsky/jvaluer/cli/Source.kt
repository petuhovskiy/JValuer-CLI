package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.commons.source.Source
import java.time.Instant

fun hashOf(source: Source) = hashOf(source.language) + hashOf(source.path)

data class SourceInfo(
        val objectHash: MySHA,
        var exe: MySHA? = null,
        var compiled: Instant? = null,
        val created: Instant = Instant.now()
)

fun dbSource(source: Source): DbObject<SourceInfo> {
    val hash = hashOf(source)
    val dbObject = db<SourceInfo>("source/$hash")
    if (!dbObject.exists()) dbObject.save(SourceInfo(saveObject(source.path)))
    return dbObject
}