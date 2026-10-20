# 💳 Expense Tracker (Personal Finance & Money IQ Clone)

Applicazione personale per il tracciamento delle spese, gestione del budget e cashflow, ispirata a **Money IQ** e integrata nell'ecosistema di **Leonardo Assistant**.

---

## 🎯 Obiettivi & Visione

- **Esperienza Money IQ:** Gestione rapida, fluida e senza frizione da mobile/desktop.
- **Single Source of Truth Finanziaria:** Tutti i conti, carte e contanti tracciati con precisione.
- **Ecosistema Leonardo & Pacioli:** Integrazione con l'assistente personale per inserimento spese in linguaggio naturale via Discord o webhook.
- **Automazione Carte & Ricevute:** Ingestione automatica delle transazioni bancarie tramite notifiche push o email triage.

---

## 🚀 Funzionalità Principali

### 1. 🏦 Gestione Multi-Conto (Accounts)
- Supporto per molteplici tipologie di conto: Conto Corrente, Carta di Credito/Debito, Contanti, Risparmio/Investimenti, Crypto.
- Selezione del conto sorgente per ogni spesa o destinazione per le entrate.
- Supporto per **Giroconti** (trasferimenti tra conti con eventuale commissione).
- Riconciliazione periodica del saldo reale.

### 2. 🏷️ Categorie & Sotto-Categorie
- Struttura gerarchica a due livelli per Uscite ed Entrate.
- Personalizzazione con icone, colori ed etichette (es. `Cibo > Spesa`, `Cibo > Ristorante`, `Sport > Palestra/Corsa`, `Università > Tasse/Libri`).
- Tag opzionali e note per ricerca avanzata.

### 3. 📊 Budgeting Intelligente
- Definizione budget mensili/periodici per categoria o budget globale.
- Monitoraggio della spesa in tempo reale con soglie di alert (es. 80%, 100%).
- Allineamento con il framework 50/30/20 di Edoardo (`02_Aree/Finanza_Personale/Asset_Allocation_e_Budget.md`).

### 4. 📜 Lista & Analisi Transazioni
- Vista cronologica con filtri combinati (data, conto, categoria, tipo, importo).
- Ricerca full-text rapida per esercente, note o tag.
- Report statistici e grafici di ripartizione spese/entrate.

### 5. 💾 Backup & Ripristino
- Snapshot automatici del database locale (SQLite).
- Esportazione/Importazione in formato JSON e CSV.
- Backup automatico archiviabile su cloud o sincronizzato con il Second Brain.

### 6. 🤖 Integrazione Leonardo & Pacioli
- **API REST & MCP Tool:** Integrazione con `leonardo-hub` / `E:\my-local-server`.
- **Comandi Naturali su Discord:** Possibilità di scrivere `Leonardo ho speso 18€ da Conad con Hype` e vedere la spesa registrata istantaneamente con categoria e conto corretti.
- **Query Vocali o da Chat:** Richieste del tipo "Quanto ho speso questo mese per il cibo?" o "A quanto ammonta il budget residuo?".

### 7. 💳 Automazione Pagamenti Carta
- **Webhook Endpoint:** Ricezione notifiche push da app bancarie (es. tramite Tasker / MacroDroid su Android) per intercettare pagamenti con carta in tempo reale.
- **Email Parser (Leonardo Mail Watcher):** Intercettazione automatica ricevute e conferme pagamento inviate via email (già etichettate da `services/mail_watcher.py`).

---

## 🛠️ Stack Tecnologico

- **Core & Backend:** Python 3.14 + FastAPI / FastMCP (`uv` package manager).
- **Database:** SQLite (WAL mode) con migrazioni leggere e backup deterministico.
- **UI / Frontend:** Web UI reattiva / PWA mobile-first (ispirata al layout pulito di Money IQ).
- **Integrazioni:** Discord Bot Webhook (`services/webhook_service.py`), MacroDroid Webhook, FastMCP Server.

---

## 📂 Struttura della Repository

```
expense-tracker/
├── src/
│   ├── api/             # Endpoint FastAPI (conti, transazioni, budget, webhooks)
│   ├── core/            # Configurazione, database SQLite, modelli dati
│   ├── services/        # Logica di business: conti, budget, categorizzazione, backup
│   ├── integrations/    # Connector per Leonardo, mail watcher, webhook pagamenti
│   └── web/             # Frontend UI (PWA responsive)
├── data/                # Database locale SQLite e directory backup
├── pyproject.toml       # Dipendenze di progetto gestite con uv
└── README.md
```

---

## 🔗 Collegamento con il Vault Obsidian

- Progetto PARA: [[budget-tracker]] / [[expense-tracker]] in `01_Progetti/Programmazione/`
- Area di Riferimento: [[Finanza_Personale]] e [[Asset_Allocation_e_Budget]]
- Ecosistema Agenti: [[Leonardo Assistant]]
