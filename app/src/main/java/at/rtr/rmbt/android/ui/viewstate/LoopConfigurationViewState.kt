package at.rtr.rmbt.android.ui.viewstate

import androidx.databinding.ObservableInt
import androidx.databinding.ObservableBoolean
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.addOnPropertyChanged

class LoopConfigurationViewState(config: AppConfig) : ViewState {

    val numberOfTests = ObservableInt(config.loopModeNumberOfTests)
    val waitingTime = ObservableInt(config.loopModeWaitingTimeMin)
    val distance = ObservableInt(config.loopModeDistanceMeters)
    val developerModeEnabled = ObservableBoolean(config.developerModeIsEnabled)

    init {
        numberOfTests.addOnPropertyChanged {
            config.loopModeNumberOfTests = it.get()
        }
        waitingTime.addOnPropertyChanged {
            config.loopModeWaitingTimeMin = it.get()
        }
        distance.addOnPropertyChanged {
            config.loopModeDistanceMeters = it.get()
        }
    }
}