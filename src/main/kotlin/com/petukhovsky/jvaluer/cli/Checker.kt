package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.commons.checker.CheckResult
import com.petukhovsky.jvaluer.commons.checker.Checker
import com.petukhovsky.jvaluer.commons.data.StringData
import com.petukhovsky.jvaluer.commons.data.TestData
import com.petukhovsky.jvaluer.commons.run.RunLimits

abstract class MyChecker {

    abstract fun check(`in`: TestData, answer: TestData, out: TestData): CheckResult

    fun checkLive(`in`: TestData, answer: TestData, out: TestData): CheckResult {
        fun resultSign(result: CheckResult): String =
                if (result.isCorrect) "*" else "X" //TODO: use unicode symbols ✓❌

        fun resultString(result: CheckResult): String =
                if (result.isCorrect) "Correct(${result.result})" else "Wrong(${result.result})"

        val result = object : LiveProcess<CheckResult>() {

            val running = listOf("\\", "|", "/", "-")

            override fun update() {
                val time: Long
                val message: String
                print("\r")
                if (ended == null) {
                    time = now() - started
                    print("[${running[(time / 300).toInt() % 4]}]")
                    message = "Checking..."
                } else {
                    time = ended!! - started
                    print("[${resultSign(this.result!!)}]")
                    message = resultString(this.result!!)
                }
                print(String.format(" %-13s ", message))
                print("[${RunLimits.timeString(time)}]")
                if (ended != null) print(" ${result!!.comment}")
            }

            override fun run(): CheckResult = this@MyChecker.check(`in`, answer, out)

        }.execute()
        println()
        return result
    }
}

class MyChainChecker(
        val chain: Checker
) : MyChecker() {
    override fun check(`in`: TestData, answer: TestData, out: TestData): CheckResult
            = chain.check(`in`, answer, out)
}

class MyRunnableChecker(
        exe: ExeInfo
) : MyChecker() {

    val runner = createRunnerBuilder().inOut(exe.io).limits(exe.createLimits()).buildSafe(exe.toExecutable())!!

    override fun check(`in`: TestData, answer: TestData, out: TestData): CheckResult {
        val result = runner.run(StringData(""), `in`.path.toString(), out.path.toString(), answer.path.toString())
        return CheckResult(result.run.exitCode == 0L, result.out.string)
    }
}