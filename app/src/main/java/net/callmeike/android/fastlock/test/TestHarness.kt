/* $Id: $
   Copyright 2017, G. Blake Meike

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package net.callmeike.android.fastlock.test

import net.callmeike.android.fastlock.FastLock
import net.callmeike.android.fastlock.db.StoreResults
import net.callmeike.android.fastlock.model.Results
import net.callmeike.android.fastlock.model.TestType
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import java.util.concurrent.CyclicBarrier


const val ITERATIONS = 100000


abstract class Test<T> {
    abstract val type: TestType
    abstract fun get(newVal: T): T
}


class TestOne(private val nThreads: Int, testFactory: (v0: Any) -> Test<Any>) {
    private val initVal = Object()

    private val test0 = testFactory(initVal)
    private val test1 = testFactory(initVal)

    private var iterations = 0L
    private var t0Total = 0L
    private var t1Total = 0L

    var value: Any? = null

    fun results(ts: Long) = Results(ts, test0.type.type, nThreads, t0Total / iterations, t1Total / iterations)

    fun runOnce() {
        var t0 = -SystemClock.elapsedRealtimeNanos()
        value = test0.get(initVal)
        t0 += SystemClock.elapsedRealtimeNanos()

        val newVal = Object()
        var t1 = -SystemClock.elapsedRealtimeNanos()
        value = test1.get(newVal)
        t1 += SystemClock.elapsedRealtimeNanos()

        update(t0, t1)
    }

    @Synchronized
    private fun update(t0: Long, t1: Long) {
        iterations++
        t0Total += t0
        t1Total += t1
    }
}


class LockTest(ctxt: Context, threads: Int?, private val resultsListener: (results: List<Results>) -> Unit) : Runnable {
    val nThreads: Int = if (threads == null) 1 else if ((threads <= 0) || (threads > 100)) 1 else threads

    private val startBarrier: CyclicBarrier = CyclicBarrier(nThreads)
    private val endBarrier: CyclicBarrier = CyclicBarrier(nThreads) { finishTest() }

    private val handler = Handler()

    private val db = (ctxt.applicationContext as FastLock).db

    private val tests = listOf(
            TestOne(nThreads, { v0 -> NullTest(v0) }),
            TestOne(nThreads, { v0 -> SynchronizedTest(v0) }),
            TestOne(nThreads, { v0 -> DoubleCheckTest(v0) }),
            TestOne(nThreads, { v0 -> SpinTest(v0) }))

    fun runTest() {
        for (i in 0..(nThreads - 1)) {
            Thread(this).start()
        }
    }

    override fun run() {
        val iterations = ITERATIONS / nThreads

        startBarrier.await()

        for (i in 0..iterations) {
            for (test in tests.shuffled()) {
                test.runOnce()
            }
        }

        endBarrier.await()
    }

    private fun finishTest() {
        val ts = System.currentTimeMillis()
        val results = tests.map({ test -> test.results(ts) }).sortedBy { it.t0 }
        handler.post { resultsListener(results) }
        StoreResults(db, results).execute()
    }
}

