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
package net.callmeike.android.fastlock.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import net.callmeike.android.fastlock.db.COL_T_READ
import net.callmeike.android.fastlock.db.COL_T_WRITE
import net.callmeike.android.fastlock.db.COL_THREADS
import net.callmeike.android.fastlock.db.COL_TS
import net.callmeike.android.fastlock.db.COL_TEST_TYPE


enum class TestType(val displayName: String, val type: Int) {
    Null("Null", 0),
    Synchronized("Synchronized", 1),
    DoubleChecked("Double Checked", 2),
    Spin("Spin", 3)
}


val TestTypeToName = TestType.values().map({ it.type to it.displayName }).toMap()


@Entity(primaryKeys = [COL_TEST_TYPE, COL_TS])
data class Results(
        @ColumnInfo(name = COL_TS)
        val ts: Long,
        @ColumnInfo(name = COL_TEST_TYPE)
        val type: Int,
        @ColumnInfo(name = COL_THREADS)
        val nThreads: Int,
        @ColumnInfo(name = COL_T_READ)
        val tRead: Long,
        @ColumnInfo(name = COL_T_WRITE)
        val tWrite: Long)
