package at.rtr.rmbt.android.viewmodel

import android.widget.ArrayAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.io
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.adapter.MapSearchResultAdapter
import at.rtr.rmbt.android.ui.viewstate.MapViewState
import at.rtr.rmbt.android.util.model.MapSearchResult
import at.specure.data.repository.MapRepository
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.util.formatForFilter
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mapbox.mapboxsdk.geometry.LatLng
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

class MapViewModel @Inject constructor(
    private val repository: MapRepository,
    private val locationWatcher: LocationWatcher
) : BaseViewModel() {

    val mapSearchResultAdapter: MapSearchResultAdapter = MapSearchResultAdapter()

    private var filterTechnology: TechnologyFilter = TechnologyFilter.FILTER_ALL
    private var currentProvider: String

    val state = MapViewState()

    val locationLiveData: LiveData<LocationInfo?>
        get() = locationWatcher.liveData

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    lateinit var providersSpinnerAdapter: ArrayAdapter<String>
    private val basicProviderList = arrayListOf("All")
    val providersLiveData: MutableLiveData<List<String>> = MutableLiveData(basicProviderList)

    var currentLayers: List<String> = emptyList()
        get() {
            val filterList = obtainFilters()
            val date = filterList[FilterTypeCode.CODE_TIME.ordinal]
            val technology = filterList[FilterTypeCode.CODE_TECHNOLOGY.ordinal]?.toUpperCase(Locale.US) ?: TechnologyFilter.FILTER_ALL.filterValue.toUpperCase(Locale.US)

            currentLayers = listOf(
                "C-$date-$technology-${currentProvider.toUpperCase(Locale.US)}",
                "M-$date-$technology-${currentProvider.toUpperCase(Locale.US)}",
                "H10-$date-$technology-${currentProvider.toUpperCase(Locale.US)}",
                "H1-$date-$technology-${currentProvider.toUpperCase(Locale.US)}",
                "H01-$date-$technology-${currentProvider.toUpperCase(Locale.US)}",
                "H001-$date-$technology-${currentProvider.toUpperCase(Locale.US)}"
            )
            return field
        }

    init {
        currentProvider = basicProviderList[0]
        addStateSaveHandler(state)
        setTimeFilter(repository.getTimeSelected().second, repository.getTimeSelected().first)
        obtainProviders()
    }

    private fun obtainProviders() {
        repository.obtainProviders {
            providersLiveData.postValue(it.apply { it.addAll(0, basicProviderList) })
        }
    }

    private fun obtainFilters(): List<String?> {
        val filterList = mutableListOf(null, null, null, TechnologyFilter.FILTER_ALL.filterValue, null, null, null, repository.getTimeSelected().formatForFilter())
        filterTechnology = TechnologyFilter.FILTER_ALL
        try {
            val technologyFilter = repository.active.technology
            if (technologyFilter != null) {
                filterList[FilterTypeCode.CODE_TECHNOLOGY.ordinal] = technologyFilter.technology
            }
            filterList[FilterTypeCode.CODE_TIME.ordinal] = repository.active.time.formatForFilter()
        } catch (e: Exception) {
            Timber.e("Unable to load set map filters: ${e.localizedMessage}")
        }
        return filterList
    }

    fun prepareDetailsLink(openUUID: String) = repository.prepareDetailsLink(openUUID)

    fun provideStyle(): String {
        return "mapbox://styles/specure/ckgqqcmvg51fj19qlisdg0vde"
    }

    fun setTechnologyFilter(filterValue: TechnologyFilter) {
        repository.markTechnologyAsSelected(filterValue.filterValue)
    }

    fun setProvider(index: Int) {
        currentProvider = providersLiveData.value!![index]
    }

    fun obtainFiltersFromServer() {
        repository.obtainFilters {
            obtainFilters()
        }
    }

    fun setTimeFilter(year: Int, month: Int) {
        state.setTimeFilterValue(year, month)
        repository.setTimeSelected(year, month)
    }

    fun loadSearchResults(query: String, latLng: LatLng, found: (List<MapSearchResult>) -> Unit) = io {
        val token = BuildConfig.HERE_API_KEY
        val limit = 10
        val queryParameters = "?q=$query&at=${latLng.latitude},${latLng.longitude}&in=countryCode:NOR&limit=$limit&apiKey=$token"
        val request = Request.Builder()
            .url("https://geocode.search.hereapi.com/v1/geocode$queryParameters")
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val jsonElement = JsonParser.parseString(res.body!!.string())
                    val results = jsonElement.asJsonObject.getAsJsonArray("items").map {
                        Gson().fromJson(it, MapSearchResult::class.java)
                    }
                    found.invoke(results)
                }
            }
        })
    }
}

enum class TechnologyFilter(val colorId: Int, val filterValue: String) {
    FILTER_ALL(R.color.map_filter_technology_ALL, "all"),
    FILTER_2G(R.color.map_filter_technology_2G, "2G"),
    FILTER_3G(R.color.map_filter_technology_3G, "3G"),
    FILTER_4G(R.color.map_filter_technology_4G, "4G"),
    FILTER_5G(R.color.map_filter_technology_5G, "5G")
}

enum class FilterTypeCode {
    CODE_TYPE, // Mobile, WLAN, Browser
    CODE_SUBTYPE, // Upload, Download, Ping, Signal
    CODE_STATISTICS, // statistics method
    CODE_TECHNOLOGY, // 2G, 3G, ...
    CODE_OPERATOR, // operator (mobile networks)
    CODE_PERIOD, // period
    CODE_PROVIDER, // wlan, browser
    CODE_TIME, // year and month to display data
}