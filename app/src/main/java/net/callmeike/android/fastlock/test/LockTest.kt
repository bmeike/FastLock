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

import net.callmeike.android.fastlock.model.TestType
import java.util.concurrent.atomic.AtomicReference


class NullTest<T>(initVal: T) : Test<T>() {
    override val type = TestType.Null
    private val value: T = initVal
    override fun get(newVal: T): T = value
}


class SynchronizedTest<T>(initVal: T) : Test<T>() {
    override val type = TestType.Synchronized
    private val lock = Object()
    private var value: T = initVal

    override fun get(newVal: T): T = synchronized(lock) {
        if (value !== newVal) {
            value = newVal
        }
        return newVal
    }
}


class DoubleCheckTest<T>(initVal: T) : Test<T>() {
    override val type = TestType.DoubleChecked
    private val lock = Object()
    @Volatile
    private var value: T = initVal

    override fun get(newVal: T): T {
        val v1 = value
        if (v1 === newVal) {
            return v1
        }
        synchronized(lock) {
            val v2 = value
            if (v1 === newVal) {
                return v2
            }
            value = newVal
            return value
        }
    }
}


class SpinTest<T>(initVal: T) : Test<T>() {
    override val type = TestType.Spin
    private val value: AtomicReference<T> = AtomicReference(initVal)

    override fun get(newVal: T): T {
        while (true) {
            val v1 = value.get()
            if (v1 === newVal) {
                return v1
            }
            if (value.compareAndSet(v1, newVal)) {
                return newVal
            }
        }
    }
}
