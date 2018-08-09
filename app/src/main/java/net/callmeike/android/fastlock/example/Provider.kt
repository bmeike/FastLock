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
package net.callmeike.android.fastlock.example

import java.util.concurrent.atomic.AtomicReference

interface Provider<A> {
    fun get(): A
}

/**
 * Example Double-check provider
 */
class DoubleCheckedProvider<A>(private val provider: Provider<A>) : Provider<A> {
    private val lock = Object()
    @Volatile
    private var value: A? = null

    override fun get(): A {
        if (value == null) {
            synchronized(lock) {
                if (value == null) {
                    value = provider.get()
                }
            }
        }
        return value!!
    }
}

class SpinProvider<A>(private val provider: Provider<A>) : Provider<A> {
    private val value: AtomicReference<A> = AtomicReference()

    override fun get(): A {
        while (true) {
            val v1 = value.get()
            if (v1 != null) {
                return v1
            }
            val v2 = provider.get()
            if (value.compareAndSet(null, v2)) {
                return v2
            }
        }
    }
}
