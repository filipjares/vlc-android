package org.videolan.vlc.viewmodels.paged

import android.content.Context
import androidx.collection.SimpleArrayMap
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import kotlinx.coroutines.*
import org.videolan.medialibrary.Medialibrary
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.vlc.util.MEDIALIBRARY_PAGE_SIZE
import org.videolan.vlc.util.ModelsHelper
import org.videolan.vlc.util.Settings
import org.videolan.vlc.util.retry
import org.videolan.vlc.viewmodels.SortableModel

abstract class MLPagedModel<T : MediaLibraryItem>(context: Context) : SortableModel(context), Medialibrary.OnMedialibraryReadyListener, Medialibrary.OnDeviceChangeListener {
    protected val medialibrary = Medialibrary.getInstance()
    val loading = MutableLiveData<Boolean>().apply { value = false }

    protected val headers = SimpleArrayMap<Int, String>()

    private val pagingConfig = PagedList.Config.Builder()
            .setPageSize(MEDIALIBRARY_PAGE_SIZE)
            .setPrefetchDistance(MEDIALIBRARY_PAGE_SIZE/2)
            .setEnablePlaceholders(true)
            .build()

    val pagedList = LivePagedListBuilder(MLDatasourceFactory(), pagingConfig)
            .build()

    init {
        medialibrary.apply {
            medialibrary.addOnMedialibraryReadyListener(this@MLPagedModel)
            medialibrary.addOnDeviceChangeListener(this@MLPagedModel)
        }
    }

    override fun onMedialibraryReady() {
        launch { refresh() }
    }

    override fun onMedialibraryIdle() {
        launch { refresh() }
    }

    override fun onDeviceChange() {
        launch {
            refresh()
        }
    }

    override fun onCleared() {
        medialibrary.removeOnMedialibraryReadyListener(this)
        medialibrary.removeOnDeviceChangeListener(this)
        super.onCleared()
    }

    abstract fun getTotalCount() : Int
    abstract fun getPage(loadSize: Int, startposition: Int) : Array<T>
    abstract fun getAll() : Array<T>

    override fun sort(sort: Int) {
        headers.clear()
        if (this.sort != sort) {
            this.sort = sort
            desc = false
        } else desc = !desc
        refresh()
        Settings.getInstance(context).edit()
                .putInt(sortKey, sort)
                .putBoolean("${sortKey}_desc", desc)
                .apply()
    }

    fun isFiltering() = filterQuery != null

    override fun filter(query: String?) {
        filterQuery = query
        refresh()
    }

    private lateinit var restoreJob : Job
    override fun restore() {
        restoreJob = launch {
            delay(500L)
            if (filterQuery != null) {
                filterQuery = null
                refresh()
            }
        }
    }

    override fun refresh(): Boolean {
        launch { headers.clear() }
        if (this::restoreJob.isInitialized && restoreJob.isActive) restoreJob.cancel()
        if (pagedList.value?.dataSource?.isInvalid == false) {
            loading.postValue(true)
            pagedList.value?.dataSource?.invalidate()
        }
        return true
    }

    protected fun completeHeaders(list: Array<out MediaLibraryItem>, startposition: Int) {
        for ((position, item) in list.withIndex()) {
            val previous = when {
                position > 0 -> list[position-1]
                startposition > 0 -> pagedList.value?.get(startposition+position-1)
                else -> null
            }
            ModelsHelper.getHeader(context, sort, item, previous)?.let {
                launch { headers.put(startposition+position, it) }
            }
        }
    }

    fun getSectionforPosition(position: Int): String {
        for (pos in 0 until headers.size()) if (position < headers.keyAt(pos)) return headers.valueAt(pos)
        return ""
    }

    inner class MLDataSource : PositionalDataSource<T>() {

        @ExperimentalCoroutinesApi
        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
            launch(Dispatchers.Unconfined) {
                retry( 1) {
                    val page = getPage(params.requestedLoadSize, params.requestedStartPosition)
                    val count = if (page.size < params.requestedLoadSize) page.size else getTotalCount()
                    try {
                        callback.onResult(page.toList(), params.requestedStartPosition, count)
                        true
                    } catch (e: IllegalArgumentException) {
                        false
                    }
                }
                loading.postValue(false)
            }
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
            callback.onResult(getPage(params.loadSize, params.startPosition).toList())
        }
    }

    inner class MLDatasourceFactory : DataSource.Factory<Int, T>() {
        override fun create() = MLDataSource()
    }
}
