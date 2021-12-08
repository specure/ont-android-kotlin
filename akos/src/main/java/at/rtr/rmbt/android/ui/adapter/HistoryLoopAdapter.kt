/*
 *
 *  Licensed under the Apache License, Version 2.0 (the “License”);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an “AS IS” BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package at.rtr.rmbt.android.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.databinding.ItemHistoryLoopBinding
import at.rtr.rmbt.android.util.bindWith
import at.rtr.rmbt.android.util.safeOffer
import at.specure.data.entity.HistoryContainer
import kotlinx.coroutines.channels.Channel
import java.text.NumberFormat
import kotlin.math.ceil

private const val ITEM_LOOP = 0
private const val ITEM_HISTORY = 1

class HistoryLoopAdapter : PagedListAdapter<HistoryContainer, HistoryLoopAdapter.Holder>(DIFF_CALLBACK) {

    val simpleClickChannel = Channel<String>(Channel.CONFLATED)
    val loopClickChannel = Channel<String>(Channel.CONFLATED)

    override fun getItemViewType(position: Int): Int {
        val size = getItem(position)?.items?.size ?: 1
        return if (size == 1) {
            ITEM_HISTORY
        } else {
            ITEM_LOOP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == ITEM_LOOP) {
            LoopHolder(parent.bindWith(R.layout.item_history_loop))
        } else {
            HistoryHolder(parent.bindWith(R.layout.item_history))
        }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val channel = if (getItemViewType(position) == ITEM_HISTORY) simpleClickChannel else loopClickChannel
        getItem(position)?.let { item -> holder.bind(position, item, channel) }
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(position: Int, item: HistoryContainer, clickChannel: Channel<String>)
    }

    class LoopHolder(val binding: ItemHistoryLoopBinding) : Holder(binding.root) {
        override fun bind(position: Int, item: HistoryContainer, clickChannel: Channel<String>) {
            if (item.items.isEmpty()) {
                return
            }
            binding.item = item.items.last()
            val numberFormat = NumberFormat.getInstance()
            binding.download.text = numberFormat.format(median(item.items.mapNotNull { it.speedDownload.toFloatOrNull() }))
            binding.upload.text = numberFormat.format(median(item.items.mapNotNull { it.speedUpload.toFloatOrNull() }))
            binding.ping.text = numberFormat.format(median(item.items.mapNotNull { it.ping.toFloatOrNull() }))
            binding.qos.text = numberFormat.format(median(item.items.mapNotNull { it.qos?.toFloatOrNull() }))
            binding.jitter.text = numberFormat.format(median(item.items.mapNotNull { it.jitterMillis?.toFloatOrNull() }))
            binding.packetLoss.text = numberFormat.format(median(item.items.mapNotNull { it.packetLossPercents?.toFloatOrNull() }))

            binding.root.setOnClickListener {
                item.items.first().loopUUID?.let(clickChannel::safeOffer)
            }
        }

        private fun median(floatList: List<Float>): Float {
            if (floatList.isEmpty()) {
                return 0f
            }

            val sortedFloatList = floatList.sorted()
            val halfIndex = (ceil((sortedFloatList.size / 2f).toDouble()) - 1).toInt()
            return if (sortedFloatList.size % 2 == 0) {
                (sortedFloatList[halfIndex] + sortedFloatList[halfIndex + 1]) / 2f
            } else {
                sortedFloatList[halfIndex]
            }
        }
    }

    class HistoryHolder(val binding: ItemHistoryBinding) : Holder(binding.root) {
        override fun bind(position: Int, item: HistoryContainer, clickChannel: Channel<String>) {
            binding.item = item.items.first()
            binding.root.setOnClickListener {
                clickChannel.safeOffer(item.items.first().testUUID)
            }
        }
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryContainer>() {

            override fun areItemsTheSame(oldItem: HistoryContainer, newItem: HistoryContainer) =
                oldItem.reference.uuid == newItem.reference.uuid

            override fun areContentsTheSame(oldItem: HistoryContainer, newItem: HistoryContainer) =
                oldItem.reference.uuid == newItem.reference.uuid &&
                        oldItem.reference.time == newItem.reference.time &&
                        oldItem.items.size == newItem.items.size
        }
    }
}