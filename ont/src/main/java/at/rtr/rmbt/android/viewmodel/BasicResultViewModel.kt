package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.rtr.rmbt.android.ui.viewstate.BasicResultViewState
import at.specure.config.Config
import at.specure.data.HistoryLoopMedian
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.repository.HistoryRepository
import at.specure.data.repository.TestResultsRepository
import at.specure.test.TestUuidType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class BasicResultViewModel @Inject constructor(
    private val config: Config,
    private val testResultsRepository: TestResultsRepository,
    private val historyRepository: HistoryRepository
) : BaseViewModel() {

    val state = BasicResultViewState()

    val testServerResultLiveData: LiveData<TestResultRecord?>
        get() = testResultsRepository.getServerTestResult(state.testUUID)

    val qoeResultLiveData: LiveData<List<QoeInfoRecord>>
        get() = testResultsRepository.getOverallQosItem(state.testUUID)

    val qoeLoopResultLiveData: LiveData<List<QoeInfoRecord>>
        get() = testResultsRepository.getQoEItems(state.testUUID)

    val qoeSingleResultLiveData: LiveData<List<QoeInfoRecord>>
        get() = testResultsRepository.getQoEItems(state.testUUID)

    val loopResultLiveData: LiveData<HistoryLoopMedian?>
        get() = historyRepository.getLoopMedianValues(state.testUUID)

    val downloadGraphLiveData: LiveData<List<TestResultGraphItemRecord>>
        get() = testResultsRepository.getGraphDataLiveData(state.testUUID, TestResultGraphItemRecord.Type.DOWNLOAD)

    val uploadGraphLiveData: LiveData<List<TestResultGraphItemRecord>>
        get() = testResultsRepository.getGraphDataLiveData(state.testUUID, TestResultGraphItemRecord.Type.UPLOAD)

    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()
    val _qoeResultLiveData = MutableLiveData<List<QoeInfoRecord>>()

    init {
        addStateSaveHandler(state)
    }

    fun loadTestResults() {
        when (state.uuidType) {
            TestUuidType.TEST_UUID -> launch {
                if (state.useLatestResults) {
                    delay(750)
                } // in case of currently finished test we need to wait for BE to prepare qos results to be sent back
                val localServerResults = testResultsRepository.getServerTestResult(state.testUUID).value
                val localServerResultsLoaded = localServerResults != null
                Timber.d("Local test results loaded: $localServerResultsLoaded")
                if (!localServerResultsLoaded) {
                    if (config.headerValue.isEmpty()) {
                        testResultsRepository.loadTestResults(state.testUUID).zip(
                            testResultsRepository.loadTestDetailsResult(state.testUUID)
                        ) { a, b -> a && b }
                            .flowOn(Dispatchers.IO)
                            .catch {
                                if (it is HandledException) {
                                    emit(false)
                                    postError(it)
                                } else {
                                    throw it
                                }
                            }
                            .collect {
                                _loadingLiveData.postValue(it)
                            }
                    } else {
                        testResultsRepository.loadTestResults(state.testUUID)
                            .flowOn(Dispatchers.IO)
                            .catch {
                                if (it is HandledException) {
                                    emit(false)
                                    postError(it)
                                } else {
                                    throw it
                                }
                            }
                            .collect {
                                _loadingLiveData.postValue(it)
                            }
                    }

                }
            }
            TestUuidType.LOOP_UUID ->
                io {

//                    historyRepository.getLoopMedianValues(state.testUUID).onCompletion {
//                        _loadingLiveData.postValue(true)
//                    }.collect {
//                        _loopMedianValuesLiveData.postValue(it)
//                    }
                }
        }
    }
}