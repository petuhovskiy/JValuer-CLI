package com.petukhovsky.jvaluer.cli

interface Script {
    fun execute()
}

abstract class LiveProcess<R>(val update: Long = 100) {

    var started: Long = -1
    var ended: Long? = null

    var result: R? = null

    abstract fun update()
    abstract fun run(): R

    fun execute(): R {
        result = null
        val thread = Thread { result = run() }
        started = now()
        ended = null
        thread.start()
        while (thread.isAlive) {
            update()
            thread.join(update)
        }
        ended = now()
        update()
        return result!!
    }

    fun now(): Long = System.currentTimeMillis()
}