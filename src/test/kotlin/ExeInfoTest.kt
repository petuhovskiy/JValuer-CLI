import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.ExeInfo
import com.petukhovsky.jvaluer.cli.FileType
import com.petukhovsky.jvaluer.cli.objectMapper
import org.junit.Test
import org.junit.Assert.assertEquals

class ExeInfoTest {
    @Test fun test() {
        val exe = ExeInfo("abc.cpp", FileType.auto, null, null, null, "stdin", "stdout")
        val string = objectMapper.writeValueAsString(exe)
        println(string)
        val exe2 = objectMapper.readValue<ExeInfo>(string)
        assertEquals(exe, exe2)
    }
}