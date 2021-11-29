package at.specure.data.repository

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.CoreDatabase
import at.specure.data.entity.HistoryContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

private const val LIMIT = 100

class HistoryLoader @Inject constructor(
    db: CoreDatabase,
    private val config: Config,
    private val historyRepository: HistoryRepository
) :
    PagedList.BoundaryCallback<HistoryContainer>() {

    var latestLoadedPage = if (isONTBasedApp()) 1 else 0
    var totalPagesCount = 1 // bigger than latestLoadedPage to have first load
    var isLoadingChannel: Channel<Boolean>? = null
    var errorChannel: Channel<HandledException>? = null

    private val historyDao = db.historyDao()
    private var isLoading = false
        set(value) {
            field = value
            runBlocking {
                isLoadingChannel?.send(value)
            }
        }

    val historyLiveData: LiveData<PagedList<HistoryContainer>> by lazy {
        val source = historyDao.getHistorySource()
        val config = PagedList.Config.Builder()
            .setPageSize(LIMIT)
            .build()
        LivePagedListBuilder(source, config)
            .setBoundaryCallback(this)
            .build()
    }

    val source: DataSource.Factory<Int, HistoryContainer>
        get() = historyDao.getHistorySource()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        if (isONTBasedApp()) {
            loadPreviousItems()
        } else {
            loadItems()
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: HistoryContainer) {
        super.onItemAtEndLoaded(itemAtEnd)
        if (isONTBasedApp()) {
            loadNextItems()
        } else {
            loadItems()
        }
    }

    @Synchronized
    private fun loadItems(pageToLoad: Int, totalPagesCount: Int) = io {
        if (!isLoading) {
            isLoading = true

            val count = historyDao.getItemsCount()
            Timber.d("HistoryItemsCount: $count")
            if (pageToLoad <= totalPagesCount) {
                val result = historyRepository.loadHistoryItems(pageToLoad * LIMIT, LIMIT)
                Timber.d("HISTORY: loading with params latestLoadedPage: $latestLoadedPage and totalPages: $totalPagesCount")
                result.onFailure {
                    errorChannel?.send(it)
                }
                result.onSuccess {
                    this@HistoryLoader.latestLoadedPage = (it.currentPage ?: 0) + 1 // number of first page is 0, but we must ask 1 based position of page
                    this@HistoryLoader.totalPagesCount = it.totalPages ?: 1
                    Timber.d("HISTORY: loaded with params latestLoadedPage: ${this@HistoryLoader.latestLoadedPage} ${it.currentPage} and totalPages: ${this@HistoryLoader.totalPagesCount} ${it.totalPages}")
                }
            }
            isLoading = false
        }
    }

    @Synchronized
    private fun loadItems() = io {
        if (!isLoading) {
            isLoading = true

            val count = historyDao.getItemsCount()
            Timber.d("HistoryItemsCount: $count")
            if ((count % LIMIT == 0) && (count / LIMIT != latestLoadedPage)) {
                val result = historyRepository.loadHistoryItems(count, LIMIT)
                result.onFailure {
                    errorChannel?.send(it)
                }
                result.onSuccess {
                    latestLoadedPage = count / LIMIT
                }
            }
            isLoading = false
        }
    }

    fun clear() = io {
        historyDao.clear()
    }

    fun loadNextItems() {
        if (!isLoading) {
            if (latestLoadedPage < totalPagesCount) {
                latestLoadedPage += 1
                Timber.d("$this HISTORY latestLoadedPage: current item to be loaded before loading: latestLoadedPage: $latestLoadedPage and totalPages: $totalPagesCount is loading?: $isLoading")
                loadItems(latestLoadedPage, totalPagesCount)
            }
        }
    }

    fun loadPreviousItems() {
        if (!isLoading) {
            if (latestLoadedPage > 1) {
                latestLoadedPage -= 1
                Timber.d("$this HISTORY loadPreviousItems: current item to be loaded before loading: latestLoadedPage: $latestLoadedPage and totalPages: $totalPagesCount is loading?: $isLoading")
                loadItems(latestLoadedPage, totalPagesCount)
            }
        }
    }

    fun refresh() = io {
        isLoading = true
        // for first page we need to make offset to be == LIMIT to ask for page number 1
        val result = if (isONTBasedApp()) {
            historyRepository.loadHistoryItems(LIMIT, LIMIT)
        } else {
            historyRepository.loadHistoryItems(0, LIMIT)
        }
        result.onFailure {
            errorChannel?.send(it)
        }
        result.onSuccess {
            if (isONTBasedApp()) {
                this@HistoryLoader.latestLoadedPage = (it.currentPage ?: 0) + 1 // number of first page is 0, but we must ask 1 based position of page
                this@HistoryLoader.totalPagesCount = it.totalPages ?: 1
                Timber.d("HISTORY refresh: loaded with params latestLoadedPage: ${this@HistoryLoader.latestLoadedPage} ${it.currentPage} and totalPages: ${this@HistoryLoader.totalPagesCount} ${it.totalPages}")
            } else {
                latestLoadedPage = 0
            }
        }
        isLoading = false
    }

    private fun isONTBasedApp(): Boolean {
        return config.headerValue.isNotEmpty()
    }
}