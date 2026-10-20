package com.edoardo.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.edoardo.expensetracker.data.local.dao.*
import com.edoardo.expensetracker.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDatabase(database)
                    }
                }
            }
        }

        suspend fun seedDatabase(database: ExpenseDatabase) {
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()

            // Seed Conti Default (Checking, Carta Revolut/Hype, Contanti, Risparmio)
            accountDao.insertAccount(
                AccountEntity(name = "Conto Principale", accountType = AccountType.CHECKING, colorHex = "#2563EB", iconName = "account_balance")
            )
            accountDao.insertAccount(
                AccountEntity(name = "Carta Revolut", accountType = AccountType.CREDIT_CARD, colorHex = "#0EA5E9", iconName = "credit_card")
            )
            accountDao.insertAccount(
                AccountEntity(name = "Contanti", accountType = AccountType.CASH, colorHex = "#10B981", iconName = "payments")
            )
            accountDao.insertAccount(
                AccountEntity(name = "Fondo Risparmio", accountType = AccountType.SAVINGS, colorHex = "#8B5CF6", iconName = "savings")
            )

            // Seed Categorie & Sottocategorie Uscite
            val expenseMap = mapOf(
                "Cibo & Spesa" to Pair("#EF4444", listOf("Supermercato", "Pranzo fuori", "Ristorante/Cena", "Bar & Caffè")),
                "Abitazione" to Pair("#F59E0B", listOf("Affitto", "Luce & Gas", "Internet/Wi-Fi", "Spese Condominiali")),
                "Sport & Salute" to Pair("#10B981", listOf("Palestra", "Corsa & Scarpe", "Farmacia & Visite")),
                "Trasporti" to Pair("#06B6D4", listOf("Mezzi GTT", "Treni / Aerei", "Taxi")),
                "Studio & PoliTO" to Pair("#3B82F6", listOf("Tasse Universitarie", "Libri & Dispense", "Mensa")),
                "Lifestyle & Svago" to Pair("#EC4899", listOf("Uscite Amici", "Cinema", "Abbigliamento", "Shopping")),
                "Tecnologia & SaaS" to Pair("#6366F1", listOf("Abbonamenti Software", "Hosting / Cloud", "Gadget"))
            )

            for ((catName, meta) in expenseMap) {
                val catId = categoryDao.insertCategory(
                    CategoryEntity(name = catName, type = TransactionType.EXPENSE, colorHex = meta.first)
                )
                for (subName in meta.second) {
                    categoryDao.insertSubcategory(SubcategoryEntity(categoryId = catId, name = subName))
                }
            }

            // Seed Categorie Entrate
            val incomeMap = mapOf(
                "Stipendio / Lavoro" to Pair("#22C55E", listOf("Stipendio", "Bonus")),
                "SaaS & Progetti" to Pair("#14B8A6", listOf("Abbonamenti B2B", "Consulenze")),
                "Investimenti" to Pair("#A855F7", listOf("Dividendi ETF", "Interessi")),
                "Rimborsi & Regali" to Pair("#84CC16", listOf("Rimborsi", "Regali"))
            )

            for ((catName, meta) in incomeMap) {
                val catId = categoryDao.insertCategory(
                    CategoryEntity(name = catName, type = TransactionType.INCOME, colorHex = meta.first)
                )
                for (subName in meta.second) {
                    categoryDao.insertSubcategory(SubcategoryEntity(categoryId = catId, name = subName))
                }
            }
        }
    }
}
