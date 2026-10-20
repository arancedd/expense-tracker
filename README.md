# 📱 Expense Tracker — Native Android App (Money IQ Style)

Applicazione Android nativa all'avanguardia per la gestione delle finanze personali, budget e tracciamento spese, ispirata a [Money IQ](https://play.google.com/store/apps/details?id=org.pixelrush.moneyiq&hl=it&pli=1), con tracciamento automatico dei pagamenti con carta e modulo di consultazione statistica in sola lettura per **Leonardo Assistant**.

---

## 🛠️ Stack Tecnologico Android (Modern & Standard)

- **Linguaggio:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose + Material 3 (100% dichiarativo)
- **Architettura:** Clean Architecture + MVI/MVVM (Coroutines, Flow, StateFlow)
- **Persistenza & Database:** Room Database (SQLite locale, reattivo con Flow, offline-first)
- **Dependency Injection:** Hilt (Dagger)
- **Build System:** Gradle con Kotlin DSL e Version Catalogs (`gradle/libs.versions.toml`)
- **Target SDK:** Android 15 (API 35) / Min SDK: Android 8.0 (API 26)

---

## 🚀 Architettura delle Feature

### 1. 🏦 Gestione Multi-Conto (Accounts)
- Monitoraggio separato di conti multipli: *Conto Corrente*, *Carte di Debito / Credito* (es. Revolut, Hype), *Contanti*, *Fondo Risparmio*, *Investimenti*.
- Selezione immediata del conto di addebito o accredito.
- Calcolo automatico in tempo reale del saldo per singolo conto e del patrimonio complessivo.
- Supporto a **Giroconti** (trasferimenti tra conti con conservazione della coerenza).

### 2. 🏷️ Categorie & Sotto-categorie a Due Livelli
- Separazione netta tra *Uscite* ed *Entrate*.
- Gerarchia a 2 livelli (es. `Cibo & Spesa > Supermercato`, `Cibo & Spesa > Pranzo fuori`, `Sport > Palestra`).
- Icone vettoriali e palette colori distintiva in pieno stile Money IQ.

### 3. 🎯 Budgeting Mensile
- Configurazione budget per categoria o macro-budget mensile.
- Tracciamento della percentuale di utilizzo in tempo reale.
- Alert preventivi automatici al superamento dell'80% e sforamento del 100%.

### 4. 📜 Lista Transazioni & Ricerca
- Feed cronologico ordinato per data e ora.
- Filtri granulari per conto, tipologia e categoria.
- Ricerca full-text istantanea per esercente, note o causale.

### 5. 💾 Backup & Ripristino
- Snapshot SQLite e dump JSON/CSV archiviabili nello storage locale o su cloud drive.
- Funzione di restore deterministica senza perdita di consistenza relazionale.

### 6. 💳 Controllo Automatico Spese con Carta (Android Nativo)
> [!IMPORTANT]
> Non passa da server esterni o webhook: opera direttamente a livello di sistema operativo Android.
- **Servizio:** `BankNotificationListenerService` (`android.service.notification.NotificationListenerService`).
- **Funzionamento:** Intercetta in tempo reale le notifiche push delle app bancarie installate (Revolut, Hype, Intesa Sanpaolo, PostePay, BBVA, N26, ecc.).
- **Parser Intelligente (`BankNotificationParser`):** Estrae automaticamente importo (es. `14,50 €`), esercente (es. `CONAD`) e associa la spesa alla carta bancaria corrispondente senza intervento manuale.

### 7. 🤖 Integrazione Leonardo (Strictly Read-Only)
> [!NOTE]
> Leonardo non scrive, non modifica e non cancella alcuna transazione.
- **Modulo:** `LeonardoStatsExporter` (`service/leonardo/`).
- **Funzione:** Esporta ed espone aggregati statistici ad uso esclusivo di consultazione:
  - Totale entrate e uscite mensili.
  - Ripartizione percentuale per categoria di spesa.
  - Stato di avanzamento del budget.
  - Leonardo può così rispondere a domande dell'utente ("Quanto ho speso questo mese?", "Qual è la categoria più onerosa?") leggendo lo snapshot senza alcun impatto sul database dell'app.

---

## 📂 Struttura del Progetto

```
expense-tracker/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/edoardo/expensetracker/
│           ├── ExpenseTrackerApp.kt
│           ├── MainActivity.kt
│           ├── data/
│           │   ├── local/
│           │   │   ├── ExpenseDatabase.kt
│           │   │   ├── dao/
│           │   │   │   ├── AccountDao.kt
│           │   │   │   ├── CategoryDao.kt
│           │   │   │   ├── TransactionDao.kt
│           │   │   │   └── BudgetDao.kt
│           │   │   └── entity/
│           │   │       ├── AccountEntity.kt
│           │   │       ├── CategoryEntity.kt
│           │   │       ├── SubcategoryEntity.kt
│           │   │       ├── TransactionEntity.kt
│           │   │       └── BudgetEntity.kt
│           │   └── repository/
│           ├── service/
│           │   ├── BankNotificationListenerService.kt   # Cattura automatica pagamenti carta
│           │   ├── parser/
│           │   │   └── BankNotificationParser.kt
│           │   └── leonardo/
│           │       └── LeonardoStatsExporter.kt        # Modulo READ-ONLY statistiche
│           ├── ui/
│           │   ├── navigation/Screen.kt
│           │   ├── theme/
│           │   └── screens/
│           └── di/
│               └── AppModule.kt
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties
```
