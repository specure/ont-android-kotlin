package at.rtr.rmbt.android.ui.fragment.results

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.LoopMeasurementResultsActivity
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.rtr.rmbt.android.ui.adapter.HistoryLoopAdapter
import at.rtr.rmbt.android.ui.fragment.BaseFragment
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

class HistoryFragment : BaseFragment() {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentHistoryBinding by bindingLazy()
    private val adapter: HistoryLoopAdapter by lazy { HistoryLoopAdapter() }

    override val layoutResId = R.layout.fragment_history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = historyViewModel.state
        binding.recyclerViewHistoryItems.adapter = adapter

        adapter.simpleClickChannel.receiveAsFlow().onEach {
            ResultsActivity.start(requireContext(), it, ResultsActivity.ReturnPoint.HISTORY)
        }.launchIn(lifecycleScope)

        adapter.loopClickChannel.receiveAsFlow().onEach {
            LoopMeasurementResultsActivity.start(requireContext(), it)
        }.launchIn(lifecycleScope)

        binding.recyclerViewHistoryItems.apply {
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.recyclerViewHistoryItems.addItemDecoration(itemDecoration)
        }

//        binding.recyclerViewHistoryItems.addOnScrollListener(object : OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                val visibleItemCount = binding.recyclerViewHistoryItems.childCount
//                val totalItemCount = binding.recyclerViewHistoryItems.layoutManager?.itemCount
//                val layoutManager = (binding.recyclerViewHistoryItems.layoutManager as LinearLayoutManager)
//                val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()
//                val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()
//                Timber.d("History FVI: $firstVisibleItem LVI: $lastVisibleItem TIC: $totalItemCount VIC: $visibleItemCount")
//                if (lastVisibleItem + 1 == totalItemCount) {
//                    historyViewModel.loadMoreItems()
//                }
//                if (firstVisibleItem == 0) {
//                    historyViewModel.loadPreviousItems()
//                }
//            }
//        })

        historyViewModel.historyLiveData.listen(this) {
            binding.swipeRefreshLayoutHistoryItems.isRefreshing = false
            historyViewModel.state.isHistoryEmpty.set(it.isEmpty())
            (parentFragment as? ResultsFragment)?.onDataLoaded(it.isEmpty())
            adapter.submitList(it) {
                val layoutManager = (binding.recyclerViewHistoryItems.layoutManager as LinearLayoutManager)
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION) {
                    binding.recyclerViewHistoryItems.scrollToPosition(position)
                }
            }
        }

        binding.swipeRefreshLayoutHistoryItems.setOnRefreshListener {
            refreshHistory()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun refreshHistory() {
        historyViewModel.refreshHistory()
        binding.swipeRefreshLayoutHistoryItems.isRefreshing = true
    }
}