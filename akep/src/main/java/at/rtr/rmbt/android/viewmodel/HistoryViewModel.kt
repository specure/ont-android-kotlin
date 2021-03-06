package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.HistoryViewState
import at.specure.data.entity.HistoryContainer
import at.specure.data.repository.HistoryLoader
import at.specure.data.repository.HistoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

class HistoryViewModel @Inject constructor(private val repository: HistoryRepository, private val loader: HistoryLoader, val config: AppConfig) : BaseViewModel() {

    private val _isLoadingLiveData = MutableLiveData<Boolean>()

    val state = HistoryViewState(config)

    val isLoadingLiveData: LiveData<Boolean>
        get() = _isLoadingLiveData

    val historyLiveData: LiveData<PagedList<HistoryContainer>>
        get() = loader.historyLiveData

    init {
        addStateSaveHandler(state)

        val isLoadingChannel = Channel<Boolean>()

        launch {
            isLoadingChannel.consumeAsFlow()
                .debounce(200)
                .collect {
                    _isLoadingLiveData.postValue(it)
                }
        }

        val errorChannel = Channel<HandledException>()
        launch {
            errorChannel.consumeAsFlow()
                .collect {
                    postError(it)
                }
        }

        loader.isLoadingChannel = isLoadingChannel
        loader.errorChannel = errorChannel
    }

    val activeFiltersLiveData = repository.appliedFiltersLiveData

    fun refreshHistory() {
        loader.refresh()
    }

    fun removeFromFilters(value: String) {
        repository.removeFromFilters(value)
        refreshHistory()
    }
}