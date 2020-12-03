package com.raywenderlich.podplay.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

// Chapter 24 - Podcast Subscriptions, Part 1



// Page 523 - update
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("podcastId")]
)


// Chapter 22

// page 492
data class Episode (
    @PrimaryKey var guid: String = "",
    var podcastId: Long? = null,
    var title: String = "",
    var description: String = "",
    var mediaUrl: String = "",
    var mimeType: String = "",
    var releaseDate: Date = Date(),
    var duration: String = ""
)

