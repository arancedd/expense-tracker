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
    version = 2,
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
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance

                // Garantisce che il seeding avvenga sempre se il DB è vuoto
                scope.launch(Dispatchers.IO) {
                    ensureDatabaseSeeded(instance)
                }

                instance
            }
        }

        suspend fun ensureDatabaseSeeded(database: ExpenseDatabase) {
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()

            val existingAccounts = accountDao.getActiveAccounts()
            if (existingAccounts.isEmpty()) {
                seedDatabase(database)
            }
        }

        suspend fun seedDatabase(database: ExpenseDatabase) {
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()

            // Seed Conti Default (Revolut, Conto Principale, Cash, PayPal, Risparmio)
            accountDao.insertAccount(
                AccountEntity(name = "Revolut", accountType = AccountType.CREDIT_CARD, initialBalance = 239.63, colorHex = "#6366F1", iconName = "credit_card")
            )
            accountDao.insertAccount(
                AccountEntity(name = "Conto Principale", accountType = AccountType.CHECKING, initialBalance = 1250.00, colorHex = "#2563EB", iconName = "account_balance")
            )
            accountDao.insertAccount(
                AccountEntity(name = "Cash", accountType = AccountType.CASH, initialBalance = 12.20, colorHex = "#10B981", iconName = "payments")
            )
            accountDao.insertAccount(
                AccountEntity(name = "Paypal", accountType = AccountType.CHECKING, initialBalance = 0.00, colorHex = "#0EA5E9", iconName = "account_balance_wallet")
            )
            accountDao.insertAccount(
                AccountEntity(name = "Fondo Risparmio", accountType = AccountType.SAVINGS, initialBalance = 220.77, colorHex = "#8B5CF6", iconName = "savings")
            )

            // Seed Categorie & Sottocategorie Uscite
            val expenseMap = mapOf(
                "Cibo & Spesa" to Pair("#EF4444", listOf("Supermercato", "Pranzo fuori", "Ristorante/Cena", "Bar & Caffè")),
                "Trasporti" to Pair("#F59E0B", listOf("Benzina", "Mezzi pubblici", "Treni / Aerei", "Taxi", "Parcheggio")),
                "Svago" to Pair("#EC4899", listOf("Cinema", "Uscite Amici", "Concerti", "Eventi")),
                "Ristoranti" to Pair("#8B5CF6", listOf("Cena fuori", "Pizzeria", "Sushi", "Takeaway")),
                "Salute" to Pair("#10B981", listOf("Farmacia", "Visite mediche", "Dentista", "Integratori")),
                "Regali" to Pair("#F43F5E", listOf("Compleanni", "Natale", "Ricorrenze")),
                "Abitazione" to Pair("#EAB308", listOf("Affitto", "Luce & Gas", "Internet/Wi-Fi", "Spese Condominiali")),
                "Shopping" to Pair("#D946EF", listOf("Abbigliamento", "Scarpe", "Accessori")),
                "Sport" to Pair("#14B8A6", listOf("Palestra", "Scarpe da corsa", "Attrezzatura")),
                "Studio & PoliTO" to Pair("#3B82F6", listOf("Tasse Universitarie", "Libri & Dispense", "Mensa")),
                "Tecnologia" to Pair("#6366F1", listOf("Abbonamenti Software", "Hosting / Cloud", "Gadget")),
                "Viaggi" to Pair("#06B6D4", listOf("Hotel / Alloggi", "Voli", "Esperienze"))
            )

            for ((catName, meta) in expenseMap) {
                val catId = categoryDao.insertCategory(
                    CategoryEntity(name = catName, type = TransactionType.EXPENSE, colorHex = meta.first)
                )
                for (subName in meta.second) {
                    categoryDao.insertSubcategory(
                        SubcategoryEntity(categoryId = catId, name = subName, iconName = "label")
                    )
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
                    categoryDao.insertSubcategory(
                        SubcategoryEntity(categoryId = catId, name = subName, iconName = "label")
                    )
                }
            }
        }
    }
}
