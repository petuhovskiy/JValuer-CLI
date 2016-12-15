package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.commons.checker.CheckResult
import com.petukhovsky.jvaluer.commons.checker.TokenChecker
import com.petukhovsky.jvaluer.commons.exe.Executable
import com.petukhovsky.jvaluer.commons.run.InvocationResult
import com.petukhovsky.jvaluer.commons.run.RunInOut
import com.petukhovsky.jvaluer.commons.run.RunLimits

data class RunAndCheckResult(
        val invocation: InvocationResult?,
        val check: CheckResult?
) {
    fun isCorrect() = isSuccess() && check?.isCorrect ?: false
    fun isSuccess() = invocation?.isSuccess() ?: false
    fun notSuccess() = !isSuccess()
    fun notCorrect() = !isCorrect()
}

fun runAndCheck(exe: Executable,
           test: Test,
           checker: MyChecker = MyChainChecker(TokenChecker()),
           limits: RunLimits = RunLimits.unlimited(),
           io: RunInOut = RunInOut.std(),
           args: String? = null,
           liveProgress: Boolean = true,
           prefix: String = ""
): RunAndCheckResult {
    val runner = createRunnerBuilder().inOut(io).limits(limits).buildSafe(exe)
    val invocation = runner.runLive(test.`in`, args, liveProgress, prefix = prefix, ln = false)
    if (invocation.notSuccess()) {
        println()
        return RunAndCheckResult(invocation, null)
    }
    val check = checker.checkLive(test.`in`, test.out, invocation.out, prefix = prefix, suffix = invocation.run.shortInfo)
    return RunAndCheckResult(invocation, check)
}

fun runAndCheck(exe: ExeInfo,
           test: Test,
           checker: MyChecker = MyChainChecker(TokenChecker()),
           args: String? = null,
           liveProgress: Boolean = true,
           allInfo: Boolean = true,
           prefix: String = ""
) =
        runAndCheck(
                exe.toExecutable(allInfo = allInfo, liveProgress = liveProgress)!!,
                test,
                checker,
                exe.createLimits(),
                exe.io,
                args,
                liveProgress,
                prefix = prefix
        )