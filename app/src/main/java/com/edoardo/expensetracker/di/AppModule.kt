package com.edoardo.expensetracker.di

import android.content.Context
import com.edoardo.expensetracker.data.local.ExpenseDatabase
import com.edoardo.expensetracker.data.local.dao.AccountDao
import com.edoardo.expensetracker.data.local.dao.BudgetDao
import com.edoardo.expensetracker.data.local.dao.CategoryDao
import com.edoardo.expensetracker.data.local.dao.TransactionDao
import com.edoardo.expensetracker.service.leonardo.LeonardoStatsExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): ExpenseDatabase {
        return ExpenseDatabase.getDatabase(context, scope)
    }

    @Provides
    fun provideAccountDao(db: ExpenseDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: ExpenseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: ExpenseDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBudgetDao(db: ExpenseDatabase): BudgetDao = db.budgetDao()

    @Provides
    @Singleton
    fun provideLeonardoStatsExporter(transactionDao: TransactionDao): LeonardoStatsExporter {
        return LeonardoStatsExporter(transactionDao)
    }
}
