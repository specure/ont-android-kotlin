package at.specure.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.GraphItemRecord

@Dao
interface GraphItemDao {

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == ${GraphItemRecord.GRAPH_ITEM_TYPE_UPLOAD} ORDER BY progress asc")
    fun getUploadGraphLiveData(testUUID: String): LiveData<List<GraphItemRecord>>

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == ${GraphItemRecord.GRAPH_ITEM_TYPE_DOWNLOAD} ORDER BY progress asc")
    fun getDownloadGraphLiveData(testUUID: String): LiveData<List<GraphItemRecord>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(graphItem: GraphItemRecord): Long

    @Query("DELETE FROM ${Tables.TEST_GRAPH_ITEM}")
    fun deleteAll(): Int
}