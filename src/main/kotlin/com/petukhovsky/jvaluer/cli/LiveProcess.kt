package com.petukhovsky.jvaluer.cli

/**
 * Created by petuh on 13.12.2016.
 */
abstract class LiveProcess<R>(val update: Long = ui.defaultUpdatePeriod) {

    var started: Long = -1
    var ended: Long? = null

    var result: R? = null

    abstract fun update()
    abstract fun run(): R

    fun execute(): R {
        result = null
        val thread = Thread { result = run() }.apply { priority = Thread.MAX_PRIORITY }
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