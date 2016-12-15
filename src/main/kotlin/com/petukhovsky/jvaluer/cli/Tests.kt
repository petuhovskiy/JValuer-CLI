package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.commons.data.PathData
import com.petukhovsky.jvaluer.commons.data.StringData
import com.petukhovsky.jvaluer.commons.data.TestData
import java.nio.file.Files
import java.nio.file.Path

data class Test(
        val name: String?,
        val `in`: TestData = StringData(""),
        val out: TestData = StringData("")
)

fun findAllTests(dir: Path): Array<Test> {
    val list = mutableListOf<Test>()
    Files.newDirectoryStream(dir).use {
        it.forEach { inFile ->
            run {
                val name = inFile.fileName.toString()
                dir.resolve(name + ".a").let {
                    if (Files.exists(it)) list.add(Test(name, PathData(inFile), PathData(it)))
                }
            }
        }
    }
    return list.toTypedArray()
}
