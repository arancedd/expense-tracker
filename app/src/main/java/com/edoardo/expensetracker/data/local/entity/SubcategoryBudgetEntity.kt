package com.edoardo.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategory_budgets",
    foreignKeys = [
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subcategoryId"], unique = true)
    ]
)
data class SubcategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subcategoryId: Long,
    val categoryId: Long,
    val amountLimit: Double,
    val isPercentage: Boolean = false,
    val percentageValue: Double = 0.0
)
