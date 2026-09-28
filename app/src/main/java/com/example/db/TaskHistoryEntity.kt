package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_history")
data class TaskHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val actionType: String,
    val resultText: String,
    val isSuccess: Boolean = true,
    val errorDetails: String? = null
)
