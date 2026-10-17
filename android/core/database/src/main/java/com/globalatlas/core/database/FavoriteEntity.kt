package com.globalatlas.core.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["entityType", "entityId"],
    indices = [Index("createdAtEpochMillis")]
)
data class FavoriteEntity(
    /** COUNTRY or REGION */
    val entityType: String,
    /** ISO2 for countries; stable source ID for regions */
    val entityId: String,
    val countryIso2: String?,
    val displayName: String,
    val createdAtEpochMillis: Long
) {
    init { require(entityType == "COUNTRY" || entityType == "REGION") }
}
