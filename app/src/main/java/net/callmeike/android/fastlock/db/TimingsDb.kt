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
package net.callmeike.android.fastlock.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Database
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.RoomDatabase
import net.callmeike.android.fastlock.model.Results
import android.os.AsyncTask


const val DB_VERSION = 1
const val DB_FILE = "timings.db"

const val COL_TEST_TYPE = "type"
const val COL_THREADS = "threads"
const val COL_TS = "ts"
const val COL_T_READ = "tRead"
const val COL_T_WRITE = "tWrite"


class StoreResults(private val db: TimingsDb, private val results: List<Results>) : AsyncTask<Unit, Unit, Unit>() {
    override fun doInBackground(vararg p0: Unit?) {
        db.dao().insertTimings(results)
    }
}


@Dao
abstract class TimingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTimings(reportedMoments: List<Results>)
}


@Database(version = DB_VERSION, entities = [Results::class])
abstract class TimingsDb : RoomDatabase() {
    abstract fun dao(): TimingsDao
}
