package com.edoardo.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionSource {
    MANUAL,
    CARD_NOTIFICATION,
    CSV_IMPORT
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("subcategoryId"),
        Index("destinationAccountId"),
        Index("dateMillis")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val destinationAccountId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val payee: String? = null,
    val note: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL
)
