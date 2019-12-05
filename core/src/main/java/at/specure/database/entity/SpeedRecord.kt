package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns
import at.specure.database.Tables

@Entity(tableName = Tables.SPEED)
data class SpeedRecord(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ForeignKey(
        entity = TestRecord::class,
        parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
        childColumns = ["testUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testUUID: String,

    val isUpload: Boolean,
    val threadId: Int,
    val timestampNanos: Long,
    val bytes: Long
)