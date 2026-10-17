package com.globalatlas.core.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "recent_views",
    primaryKeys = ["entityType", "entityId"],
    indices = [Index("viewedAtEpochMillis")]
)
data class RecentViewEntity(
    val entityType: String,
    val entityId: String,
    val countryIso2: String?,
    val displayName: String,
    val viewedAtEpochMillis: Long
) {
    init { require(entityType == "COUNTRY" || entityType == "REGION") }
}
