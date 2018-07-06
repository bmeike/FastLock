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

import android.app.Application
import android.arch.persistence.room.Room
import net.callmeike.android.fastlock.db.DB_FILE
import net.callmeike.android.fastlock.db.TimingsDb


class FastLock : Application() {
    lateinit var db: TimingsDb

    override fun onCreate() {
        super.onCreate()

        db = Room.databaseBuilder(this, TimingsDb::class.java, DB_FILE)
                .fallbackToDestructiveMigration()
                .build()
    }
}
