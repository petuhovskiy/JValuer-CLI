package com.petukhovsky.jvaluer.cli

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

interface HashableSHA {
    fun hashSHA(): MySHA
}

class MySHA internal constructor(bytes: ByteArray) {

    val string: String = hexAdapter.marshal(bytes)

    companion object {
        @JsonCreator @JvmStatic fun byString(value: String): MySHA {
            return MySHA(hexAdapter.unmarshal(value))
        }
    }

    override @JsonValue fun toString(): String = string

    override fun equals(other: Any?): Boolean {
        return other is MySHA && other.toString() == toString()
    }

    override fun hashCode(): Int {
        return Objects.hashCode(toString())
    }

    operator fun plus(sha: MySHA): MySHA {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(hexAdapter.unmarshal(string))
        return MySHA(digest.digest(hexAdapter.unmarshal(sha.string)))
    }
}

val hexAdapter = HexBinaryAdapter()

fun hashOf(bytes: ByteArray): MySHA = MySHA(MessageDigest.getInstance("SHA-1").digest(bytes))

fun hashOf(string: String): MySHA = hashOf(string.toByteArray(Charset.forName("UTF-8")))

fun hashOf(path: Path): MySHA {
    val digest = MessageDigest.getInstance("SHA-1")
    Files.newInputStream(path).use {
        val buf = ByteArray(1024 * 4)
        var cnt: Int
        while (true) {
            cnt = it.read(buf)
            if (cnt == -1) break
            digest.update(buf, 0, cnt)
        }
    }
    return MySHA(digest.digest())
}

fun hashOf(o: HashableSHA) = o.hashSHA()
