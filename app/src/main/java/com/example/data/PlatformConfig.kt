package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "platform_configs")
data class PlatformConfig(
    @PrimaryKey val name: String,
    val isActive: Boolean = true,
    val collectionMethod: String, // "API", "알림메일", "수동등록", "크롤링 예정"
    val lastUpdated: Long = System.currentTimeMillis(),
    val colorHex: String
)
