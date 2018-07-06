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
package net.callmeike.android.fastlock.ui

import net.callmeike.android.fastlock.R
import net.callmeike.android.fastlock.model.Results
import net.callmeike.android.fastlock.model.TestTypeToName
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class ResultsAdapter(results: List<Results>?) : RecyclerView.Adapter<ResultsAdapter.ViewHolder>() {

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val nameView: TextView = root.findViewById(R.id.name)
        private val t0View: TextView = root.findViewById(R.id.t0)
        private val t1View: TextView = root.findViewById(R.id.t1)

        fun setResults(results: Results) {
            nameView.text = TestTypeToName[results.type]
            t0View.text = results.t0.toString()
            t1View.text = results.t1.toString()
        }
    }

    var results = results
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = results?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_result, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val r = results ?: return
        holder.setResults(r[pos])
    }
}
