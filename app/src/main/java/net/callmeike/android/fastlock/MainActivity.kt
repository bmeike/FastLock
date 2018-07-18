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
package net.callmeike.android.fastlock

import net.callmeike.android.fastlock.model.Results
import net.callmeike.android.fastlock.test.LockTest
import net.callmeike.android.fastlock.ui.ResultsAdapter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*


private const val DELAY = 1000 * 60L
private val NTHREADS = listOf(1, 5, 10, 20)

class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var adapter: ResultsAdapter

    private var running: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler()

        setContentView(R.layout.activity_main)

        test_results.layoutManager = LinearLayoutManager(this)
        test_results.setHasFixedSize(true)

        adapter = ResultsAdapter(null)
        test_results.adapter = adapter

        run_tests.setOnClickListener { runTest() }
        start_tests.setOnClickListener { startTesting() }
        stop_tests.setOnClickListener { stopTesting() }
    }

    private fun runTest() {
        var nThreads: Int? = null
        try {
            nThreads = Integer.parseInt(num_threads.text.toString())
        } catch (ignore: Exception) {
        }

        beginTest(LockTest(this, nThreads, { results -> displayResults(results) }))
    }

    private fun startTesting() {
        running = true
        nextTest()
    }

    private fun stopTesting() {
        running = false
    }

    private fun beginTest(test: LockTest) {
        adapter.results = null

        num_threads.setText(test.nThreads.toString())

        root.setBackgroundColor(resources.getColor(R.color.inProgress))

        test.runTest()
    }

    private fun displayResults(results: List<Results>) {
        adapter.results = results
        root.setBackgroundColor(Color.WHITE)
    }

    private fun nextTest() {
        beginTest(LockTest(this, NTHREADS.shuffled()[0], { results -> completeTest(results) }))
    }

    private fun completeTest(results: List<Results>) {
        if (running) {
            handler.postDelayed({ nextTest() }, DELAY)
        }
        displayResults(results)
    }
}

