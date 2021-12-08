package at.specure.location

import timber.log.Timber

private const val LOCATION_MAX_AGE_MS = 30_000L

/**
 * [LocationDispatcher] that uses to choose the best location to publish
 *
 * Dispatched is following rules to publish location:
 * - last location was missing
 * - last location accuracy is worse then new location accuracy
 * - last location age is more then [LOCATION_MAX_AGE_MS]
 * - last location came from the same source that new location
 */
class DefaultLocationDispatcher : LocationDispatcher {

    private var lastLocation: LocationInfo? = null
    private var lastSource: LocationSource? = null
    private var lastTimestamp = 0L

    override fun latestLocation(sources: List<LocationSource>): LocationInfo? {
        val timestamp = System.currentTimeMillis()
        var location: LocationInfo? = null
        var missingLocationsCount = 0
        if (timestamp >= lastTimestamp + LOCATION_MAX_AGE_MS) {
            sources.forEach {
                val newLocation = it.location
                if (newLocation == null) {
                    missingLocationsCount++
                }

                newLocation?.let {
                    if (location == null) {
                        location = newLocation
                        location?.let {
                            Timber.d("LOCU: LOCATION UPDATE 1 ageNanos:  ${it.ageNanos} in seconds: ${it.ageNanos / 1000000000}")
                        }
                    } else if (location?.accuracy ?: Float.MAX_VALUE > newLocation.accuracy) {
                        location = newLocation
                        location?.let {
                            Timber.d("LOCU: LOCATION UPDATE 2 ageNanos:  ${it.ageNanos} in seconds: ${it.ageNanos / 1000000000}")
                        }
                    }
                }
            }

            return if (location == null) {
                if (missingLocationsCount == sources.size) {
                    lastLocation = null
                    lastSource = null
                    lastTimestamp = 0
                    null
                } else {
                    lastLocation
                }
            } else {
                location
            }
        }

        return lastLocation
    }

    override fun onPermissionsDisabled() {
        lastLocation = null
        lastSource = null
        lastTimestamp = 0
    }

    override fun onLocationInfoChanged(source: LocationSource, location: LocationInfo?): LocationDispatcher.Decision {
        val timestamp = System.currentTimeMillis()
        location?.let {
            Timber.i(
                "LOCU: location update: new location came: ${source::class.java.simpleName} ${location.provider} ${location.accuracy} ${
                    location.ageNanos.div(
                        1000000000
                    )
                } ${((location.time.plus(LOCATION_MAX_AGE_MS * 2)) >= System.currentTimeMillis())}  ${location.time} $timestamp ${timestamp - location.time} $location"
            )
        }
        if (location == null) {
            return if (lastSource == source) {
                LocationDispatcher.Decision(null, true)
            } else {
                LocationDispatcher.Decision(null, false)
            }
        } else {
            return if (lastLocation == null) {
                Timber.v("location update: no last location")
                updateLastLocation(location, timestamp, source)
            } else if (lastLocation?.accuracy ?: Float.MAX_VALUE > location.accuracy) {
                Timber.v("location update: accuracy is better")
                updateLastLocation(location, timestamp, source)
            } else if (timestamp >= lastTimestamp + LOCATION_MAX_AGE_MS) {
                Timber.v("location update: last outdated")
                updateLastLocation(location, timestamp, source)
            } else if (lastSource != null && lastSource == source) {
                Timber.v("location update: source")
                updateLastLocation(location, timestamp, source)
            } else {
                Timber.v("location update: else")
                LocationDispatcher.Decision(null, false)
            }
        }
    }

    private fun updateLastLocation(
        location: LocationInfo?,
        timestamp: Long,
        source: LocationSource
    ): LocationDispatcher.Decision {
        lastLocation = location
        lastTimestamp = timestamp
        lastSource = source
        location?.let {
            Timber.d("LOCU: LOCATION UPDATE ageNanos:  ${it.ageNanos} in seconds: ${it.ageNanos / 1000000000}")
        }
        return LocationDispatcher.Decision(location, true)
    }
}