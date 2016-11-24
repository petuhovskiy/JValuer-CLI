import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.MySHA
import com.petukhovsky.jvaluer.cli.hashOf
import com.petukhovsky.jvaluer.cli.objectMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class HashTest {
    @Test fun test() {
        val string = "123456 wow, much test"
        val hash = hashOf(string)
        val json = objectMapper.writeValueAsString(hash)
        println(json)
        val hash2 = objectMapper.readValue<MySHA>(json)
        assertEquals(hash, hash2)
    }
}