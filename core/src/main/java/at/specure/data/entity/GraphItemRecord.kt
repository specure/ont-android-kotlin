package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(tableName = Tables.TEST_GRAPH_ITEM)
data class GraphItemRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(
        entity = TestRecord::class,
        parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
        childColumns = ["testUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testUUID: String?,
    val progress: Int,
    val value: Long,
    val type: Int
) {
    companion object {
        const val GRAPH_ITEM_TYPE_DOWNLOAD = 1
        const val GRAPH_ITEM_TYPE_UPLOAD = 2
    }
}
