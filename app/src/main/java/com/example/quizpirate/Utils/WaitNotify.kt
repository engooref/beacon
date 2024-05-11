package com.example.quizpirate.Utils

import android.util.Log
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WaitNotify {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var wasSignalled = false

    fun doWait() {
        lock.withLock {
            while (!wasSignalled) {
                try {
                    condition.await()
                } catch (e: InterruptedException) {
                    Log.e("Signal", "éxécution interompu", e)
                }
            }
            //clear signal and continue running.
            wasSignalled = false
        }
    }

    fun doNotify() {
        lock.withLock {
            wasSignalled = true
            condition.signal()
        }
    }
}