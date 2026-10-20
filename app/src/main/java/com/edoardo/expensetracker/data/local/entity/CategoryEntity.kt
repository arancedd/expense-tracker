package com.edoardo.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val colorHex: String = "#64748B",
    val iconName: String = "category"
)
