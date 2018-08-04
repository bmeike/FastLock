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

import android.content.Context
import android.os.Handler
import net.callmeike.android.fastlock.FastLock
import net.callmeike.android.fastlock.db.StoreResults
import net.callmeike.android.fastlock.model.Results
import net.callmeike.android.fastlock.model.TestType
import java.util.concurrent.CyclicBarrier
import kotlin.system.measureNanoTime


const val ITERATIONS = 300000


abstract class Test<T> {
    abstract val type: TestType
    abstract fun get(newVal: T): T
}


class TestOne(private val nThreads: Int, testFactory: (v0: Any) -> Test<Any>) {
    private val initVal = Object()

    private val readTest = testFactory(initVal)
    private val writeTest = testFactory(initVal)

    private var iterations = 0L
    private var tReadTotal = 0L
    private var tWriteTotal = 0L

    var value: Any? = null

    fun results(ts: Long)
            = Results(ts, readTest.type.type, nThreads, tReadTotal / iterations, tWriteTotal / iterations)

    fun runOnce() {
        val oldVal = initVal
        val newVal = Object()
        var tRead = 0L
        var tWrite = 0L
        listOf(
            { tRead = measureNanoTime { value = readTest.get(oldVal) } },
            { tWrite = measureNanoTime { value = writeTest.get(newVal) } })
                .shuffled()
                .map { it.invoke() }
        update(tRead, tWrite)
    }

    @Synchronized
    private fun update(tRead: Long, tWrite: Long) {
        iterations++
        tReadTotal += tRead
        tWriteTotal += tWrite
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
        val results = tests.map({ test -> test.results(ts) }).sortedBy { it.tRead }
        handler.post { resultsListener(results) }
        StoreResults(db, results).execute()
    }
}

