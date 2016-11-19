import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.Lang
import com.petukhovsky.jvaluer.cli.objectMapper
import org.junit.Test

open class LangTest {

    @Test
    fun test() {
        val string = """
        {
            "name": "GNU C++",
            "exts": ["cpp", "h"],
            "id": "cpp",
            "compiler": {
                "type": "runnable",
                "exe": "g++",
                "pattern": "{defines} -O2 -std=c++11 {source} -o {output}",
                "timeout": 60000
            },
            "invoker": {
                "type": "default"
            }
        }"""
        val lang = objectMapper.readValue<Lang>(string)
        println(lang)
    }
}