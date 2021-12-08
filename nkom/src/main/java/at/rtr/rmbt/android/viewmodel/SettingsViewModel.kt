package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.SettingsViewState
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.MeasurementServers
import at.specure.data.repository.SettingsRepository
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val locationWatcher: LocationWatcher,
    clientUUID: ClientUUID,
    val measurementServers: MeasurementServers,
    settingsRepository: SettingsRepository,
    controlServerSettings: ControlServerSettings
) :
    BaseViewModel() {

    val state = SettingsViewState(appConfig, clientUUID, measurementServers, controlServerSettings, settingsRepository)

    private val _openCodeWindow = MutableLiveData<Boolean>()
    private var count: Int = 0

    val openCodeWindow: LiveData<Boolean>
        get() = _openCodeWindow

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    init {
        addStateSaveHandler(state)
    }

    fun isLoopModeWaitingTimeValid(value: Int, minValue: Int, maxValue: Int): Boolean {

        if (value in minValue..maxValue || state.developerModeIsEnabled.get() == true) {
            state.loopModeWaitingTimeMin.set(value)
            return true
        }
        return false
    }

    fun isLoopModeDistanceMetersValid(value: Int, minValue: Int, maxValue: Int): Boolean {
        if (value in minValue..maxValue || state.developerModeIsEnabled.get() == true) {
            state.loopModeDistanceMeters.set(value)
            return true
        }
        return false
    }

    fun isLoopModeNumberOfTestValid(value: Int, minValue: Int, maxValue: Int): Boolean {
        if (value in minValue..maxValue || state.developerModeIsEnabled.get() == true) {
            state.loopModeNumberOfTests.set(value)
            return true
        }
        return false
    }

    /**
     * Function is used for check input code
     */
    fun isCodeValid(code: String): Int {

        if (code.isNotBlank()) {

            return when (code) {
                appConfig.secretCodeDeveloperModeOn -> {
                    state.developerModeIsEnabled.set(true)
                    R.string.preferences_developer_options_available
                }
                appConfig.secretCodeDeveloperModeOff -> {
                    state.developerModeIsEnabled.set(false)
                    R.string.preferences_developer_options_disabled
                }
                appConfig.secretCodeAllModesOff -> {
                    state.developerModeIsEnabled.set(false)
                    R.string.preferences_all_disabled
                }
                else -> {
                    R.string.preferences_developer_try_again
                }
            }
        }
        return R.string.preferences_developer_try_again
    }

    /**
     * Function is used for show input dialog when user 10 time click
     */
    fun onVersionClicked() {

        if (state.developerModeIsAvailable) {

            _openCodeWindow.postValue(false)
            if (count < 9) {
                count += 1
            } else {
                count = 0
                _openCodeWindow.postValue(true)
            }
        }
    }
}
