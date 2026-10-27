package com.edoardo.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        BudgetEntity::class,
        ExpectedIncomeEntity::class,
        SubcategoryBudgetEntity::class,
        SavingsBudgetEntity::class,
        StockEntity::class,
        InvestmentTransactionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun investmentDao(): InvestmentDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS budgets")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        amountLimit REAL NOT NULL,
                        isPercentage INTEGER NOT NULL DEFAULT 0,
                        percentageValue REAL NOT NULL DEFAULT 0.0,
                        alertThreshold REAL NOT NULL DEFAULT 0.8,
                        FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS expected_incomes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        expectedAmount REAL NOT NULL,
                        FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expected_incomes_categoryId ON expected_incomes(categoryId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN hasSubcategoriesBudget INTEGER NOT NULL DEFAULT 0")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subcategory_budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subcategoryId INTEGER NOT NULL,
                        categoryId INTEGER NOT NULL,
                        amountLimit REAL NOT NULL,
                        isPercentage INTEGER NOT NULL DEFAULT 0,
                        percentageValue REAL NOT NULL DEFAULT 0.0,
                        FOREIGN KEY (subcategoryId) REFERENCES subcategories(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_subcategory_budgets_subcategoryId ON subcategory_budgets(subcategoryId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS savings_budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        amountLimit REAL NOT NULL,
                        isPercentage INTEGER NOT NULL DEFAULT 0,
                        percentageValue REAL NOT NULL DEFAULT 0.0,
                        FOREIGN KEY (accountId) REFERENCES accounts(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_savings_budgets_accountId ON savings_budgets(accountId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS stocks (
                        symbol TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        currentPrice REAL NOT NULL,
                        previousClosePrice REAL NOT NULL,
                        currency TEXT NOT NULL,
                        isFollowed INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS investment_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        symbol TEXT NOT NULL,
                        stockName TEXT NOT NULL,
                        type TEXT NOT NULL,
                        shares REAL NOT NULL,
                        pricePerShare REAL NOT NULL,
                        totalAmount REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        notes TEXT,
                        FOREIGN KEY (accountId) REFERENCES accounts(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_transactions_accountId ON investment_transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_transactions_symbol ON investment_transactions(symbol)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stocks ADD COLUMN assetType TEXT NOT NULL DEFAULT 'STOCK'")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker_db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
            val investmentDao = database.investmentDao()

            val existingAccounts = accountDao.getActiveAccounts()
            if (existingAccounts.isEmpty()) {
                seedDatabase(database)
            }

            // Assicurati che tutti gli ETF, Indici e Stock di base siano presenti
            seedStocks(investmentDao)
        }

        suspend fun seedStocks(investmentDao: InvestmentDao) {
            val defaultStocks = listOf(
                // --- ETF Mondiali & Settoriali (MSCI All World, S&P 500, Nasdaq, ecc.) ---
                StockEntity(
                    symbol = "VWCE",
                    name = "Vanguard FTSE All-World UCITS ETF",
                    currentPrice = 170.80,
                    previousClosePrice = 170.00,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = true
                ),
                StockEntity(
                    symbol = "SWDA",
                    name = "iShares Core MSCI World UCITS ETF",
                    currentPrice = 130.60,
                    previousClosePrice = 130.50,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = true
                ),
                StockEntity(
                    symbol = "ACWI",
                    name = "iShares MSCI ACWI ETF",
                    currentPrice = 115.60,
                    previousClosePrice = 114.80,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "SPY",
                    name = "SPDR S&P 500 ETF Trust",
                    currentPrice = 581.30,
                    previousClosePrice = 579.50,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = true
                ),
                StockEntity(
                    symbol = "VUAA",
                    name = "Vanguard S&P 500 UCITS ETF (Acc)",
                    currentPrice = 103.50,
                    previousClosePrice = 102.80,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "QQQ",
                    name = "Invesco QQQ Trust (Nasdaq 100)",
                    currentPrice = 492.20,
                    previousClosePrice = 488.60,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "EIMI",
                    name = "iShares Core MSCI Emerging Markets",
                    currentPrice = 32.70,
                    previousClosePrice = 32.40,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "AGGH",
                    name = "iShares Global Aggregate Bond ETF",
                    currentPrice = 4.85,
                    previousClosePrice = 4.84,
                    assetType = StockEntity.TYPE_ETF,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "SGLD",
                    name = "Invesco Physical Gold ETC",
                    currentPrice = 215.40,
                    previousClosePrice = 213.90,
                    assetType = StockEntity.TYPE_COMMODITY,
                    isFollowed = false
                ),

                // --- Indici di Borsa Mondiali ---
                StockEntity(
                    symbol = "^GSPC",
                    name = "S&P 500 Index",
                    currentPrice = 5864.00,
                    previousClosePrice = 5845.00,
                    assetType = StockEntity.TYPE_INDEX,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "^IXIC",
                    name = "Nasdaq Composite",
                    currentPrice = 18518.00,
                    previousClosePrice = 18420.00,
                    assetType = StockEntity.TYPE_INDEX,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "^DJI",
                    name = "Dow Jones Industrial Average",
                    currentPrice = 42930.00,
                    previousClosePrice = 42810.00,
                    assetType = StockEntity.TYPE_INDEX,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "^FTSEMIB",
                    name = "FTSE MIB (Borsa Italiana)",
                    currentPrice = 34820.00,
                    previousClosePrice = 34650.00,
                    assetType = StockEntity.TYPE_INDEX,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "^STOXX50E",
                    name = "EURO STOXX 50 Index",
                    currentPrice = 4940.00,
                    previousClosePrice = 4920.00,
                    assetType = StockEntity.TYPE_INDEX,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "^DAX",
                    name = "DAX 40 (Germania)",
                    currentPrice = 19460.00,
                    previousClosePrice = 19380.00,
                    assetType = StockEntity.TYPE_INDEX,
                    isFollowed = false
                ),

                // --- Azioni Singole ---
                StockEntity(
                    symbol = "AAPL",
                    name = "Apple Inc.",
                    currentPrice = 232.50,
                    previousClosePrice = 230.10,
                    assetType = StockEntity.TYPE_STOCK,
                    isFollowed = true
                ),
                StockEntity(
                    symbol = "NVDA",
                    name = "NVIDIA Corporation",
                    currentPrice = 141.20,
                    previousClosePrice = 138.80,
                    assetType = StockEntity.TYPE_STOCK,
                    isFollowed = true
                ),
                StockEntity(
                    symbol = "TSLA",
                    name = "Tesla, Inc.",
                    currentPrice = 218.40,
                    previousClosePrice = 213.90,
                    assetType = StockEntity.TYPE_STOCK,
                    isFollowed = true
                ),
                StockEntity(
                    symbol = "MSFT",
                    name = "Microsoft Corporation",
                    currentPrice = 428.10,
                    previousClosePrice = 425.30,
                    assetType = StockEntity.TYPE_STOCK,
                    isFollowed = true
                ),
                StockEntity(
                    symbol = "AMZN",
                    name = "Amazon.com, Inc.",
                    currentPrice = 186.70,
                    previousClosePrice = 185.20,
                    assetType = StockEntity.TYPE_STOCK,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "GOOGL",
                    name = "Alphabet Inc.",
                    currentPrice = 165.90,
                    previousClosePrice = 164.50,
                    assetType = StockEntity.TYPE_STOCK,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "META",
                    name = "Meta Platforms, Inc.",
                    currentPrice = 582.40,
                    previousClosePrice = 576.80,
                    assetType = StockEntity.TYPE_STOCK,
                    isFollowed = false
                ),

                // --- Crypto Assets ---
                StockEntity(
                    symbol = "BTCUSDT",
                    name = "Bitcoin",
                    currentPrice = 67450.00,
                    previousClosePrice = 66800.00,
                    assetType = StockEntity.TYPE_CRYPTO,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "ETHUSDT",
                    name = "Ethereum",
                    currentPrice = 2620.00,
                    previousClosePrice = 2595.00,
                    assetType = StockEntity.TYPE_CRYPTO,
                    isFollowed = false
                ),
                StockEntity(
                    symbol = "SOLUSDT",
                    name = "Solana",
                    currentPrice = 165.00,
                    previousClosePrice = 162.00,
                    assetType = StockEntity.TYPE_CRYPTO,
                    isFollowed = false
                )
            )
            investmentDao.insertStocksIgnore(defaultStocks)
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
