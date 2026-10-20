package com.edoardo.expensetracker.data.local.dao

import androidx.room.*
import com.edoardo.expensetracker.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

data class AccountWithBalance(
    @Embedded val account: AccountEntity,
    val currentBalance: Double
)

@Dao
interface AccountDao {

    @Query("""
        SELECT a.*,
        (
            a.initialBalance
            + COALESCE((SELECT SUM(amount) FROM transactions WHERE accountId = a.id AND type = 'INCOME'), 0.0)
            - COALESCE((SELECT SUM(amount) FROM transactions WHERE accountId = a.id AND type = 'EXPENSE'), 0.0)
            - COALESCE((SELECT SUM(amount) FROM transactions WHERE accountId = a.id AND type = 'TRANSFER'), 0.0)
            + COALESCE((SELECT SUM(amount) FROM transactions WHERE destinationAccountId = a.id AND type = 'TRANSFER'), 0.0)
        ) AS currentBalance
        FROM accounts a
        WHERE a.isActive = 1
        ORDER BY a.id ASC
    """)
    fun observeAccountsWithBalance(): Flow<List<AccountWithBalance>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getActiveAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE name LIKE '%' || :nameKeyword || '%' LIMIT 1")
    suspend fun findAccountByKeyword(nameKeyword: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}
