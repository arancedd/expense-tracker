import os
import sqlite3
from pathlib import Path
from contextlib import contextmanager

BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent
DATA_DIR = BASE_DIR / "data"
DB_PATH = DATA_DIR / "expenses.db"


def get_db_path() -> Path:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    return DB_PATH


@contextmanager
def get_db():
    conn = sqlite3.connect(get_db_path())
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    conn.execute("PRAGMA journal_mode = WAL;")
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def init_db():
    """Inizializza le tabelle del database e carica i dati seed iniziali se vuoto."""
    with get_db() as conn:
        cursor = conn.cursor()

        # 1. Accounts (Conti)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                account_type TEXT NOT NULL,
                initial_balance REAL NOT NULL DEFAULT 0.0,
                currency TEXT NOT NULL DEFAULT 'EUR',
                color TEXT DEFAULT '#3B82F6',
                icon TEXT DEFAULT 'wallet',
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
            );
        """)

        # 2. Categorie
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL, -- 'expense' o 'income'
                color TEXT DEFAULT '#64748B',
                icon TEXT DEFAULT 'tag',
                is_default INTEGER NOT NULL DEFAULT 0,
                UNIQUE(name, type)
            );
        """)

        # 3. Sotto-categorie
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS subcategories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                UNIQUE(category_id, name)
            );
        """)

        # 4. Transazioni (Uscite, Entrate, Giroconti)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id INTEGER NOT NULL REFERENCES accounts(id),
                type TEXT NOT NULL, -- 'expense', 'income', 'transfer'
                amount REAL NOT NULL,
                category_id INTEGER REFERENCES categories(id),
                subcategory_id INTEGER REFERENCES subcategories(id),
                destination_account_id INTEGER REFERENCES accounts(id),
                date TEXT NOT NULL, -- YYYY-MM-DD
                time TEXT DEFAULT (time('now', 'localtime')),
                payee TEXT,
                note TEXT,
                source TEXT NOT NULL DEFAULT 'manual', -- 'manual', 'leonardo', 'card_webhook', 'csv'
                created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
            );
        """)

        # 5. Budgeting
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER REFERENCES categories(id) ON DELETE CASCADE,
                month TEXT NOT NULL, -- YYYY-MM
                amount_limit REAL NOT NULL,
                alert_threshold REAL NOT NULL DEFAULT 0.8,
                UNIQUE(category_id, month)
            );
        """)

        # Seed iniziale categorie predefinite se la tabella è vuota
        cursor.execute("SELECT COUNT(*) FROM categories;")
        if cursor.fetchone()[0] == 0:
            seed_initial_data(cursor)


def seed_initial_data(cursor):
    """Carica conti base e categorie/sottocategorie predefinite ispirate a Money IQ."""
    # Conti predefiniti
    accounts = [
        ("Conto Principale", "checking", 0.0, "EUR", "#2563EB", "landmark"),
        ("Revolut / Carta", "credit_card", 0.0, "EUR", "#0EA5E9", "credit-card"),
        ("Contanti", "cash", 0.0, "EUR", "#10B981", "banknotes"),
        ("Fondo Risparmio", "savings", 0.0, "EUR", "#8B5CF6", "piggy-bank")
    ]
    cursor.executemany(
        "INSERT INTO accounts (name, account_type, initial_balance, currency, color, icon) VALUES (?, ?, ?, ?, ?, ?);",
        accounts
    )

    # Categorie e sottocategorie di spesa
    expense_catalog = {
        "Cibo & Alimentari": {
            "color": "#EF4444", "icon": "utensils",
            "sub": ["Supermercato", "Pranzo fuori", "Ristorante/Cena", "Bar & Snack"]
        },
        "Abitazione & Utenze": {
            "color": "#F59E0B", "icon": "home",
            "sub": ["Affitto", "Luce & Gas", "Internet/Wi-Fi", "Spese Condominiali", "Manutenzione"]
        },
        "Sport & Salute": {
            "color": "#10B981", "icon": "activity",
            "sub": ["Palestra", "Corsa & Scarpe", "Integratori", "Visite Mediche / Farmacia"]
        },
        "Trasporti & Viaggi": {
            "color": "#06B6D4", "icon": "train",
            "sub": ["Mezzi Pubblici (GTT)", "Treni / Aerei", "Car Sharing / Taxi"]
        },
        "Studio & PoliTO": {
            "color": "#3B82F6", "icon": "graduation-cap",
            "sub": ["Tasse Universitarie", "Libri & Dispense", "Cancelleria", "Mensa"]
        },
        "Lifestyle & Svago": {
            "color": "#EC4899", "icon": "sparkles",
            "sub": ["Uscite Sociali", "Cinema & Eventi", "Abbigliamento", "Shopping"]
        },
        "Tecnologia & Abbonamenti": {
            "color": "#6366F1", "icon": "cpu",
            "sub": ["Software / SaaS", "Hosting & Server", "Dispositivi", "Streaming"]
        }
    }

    for cat_name, cat_data in expense_catalog.items():
        cursor.execute(
            "INSERT INTO categories (name, type, color, icon, is_default) VALUES (?, 'expense', ?, ?, 1);",
            (cat_name, cat_data["color"], cat_data["icon"])
        )
        cat_id = cursor.lastrowid
        for sub in cat_data["sub"]:
            cursor.execute(
                "INSERT INTO subcategories (category_id, name) VALUES (?, ?);",
                (cat_id, sub)
            )

    # Categorie di entrata
    income_catalog = {
        "Stipendio / Lavoro": {
            "color": "#22C55E", "icon": "briefcase",
            "sub": ["Stipendio Fisso", "Bonus", "Consulenze"]
        },
        "B2B SaaS / Business": {
            "color": "#14B8A6", "icon": "trending-up",
            "sub": ["Abbonamenti SaaS", "Servizi AI"]
        },
        "Rimborsi & Regali": {
            "color": "#84CC16", "icon": "gift",
            "sub": ["Rimborsi Spese", "Regali Ricevuti"]
        },
        "Investimenti & Dividendi": {
            "color": "#A855F7", "icon": "pie-chart",
            "sub": ["Dividendi ETF", "Interessi"]
        }
    }

    for cat_name, cat_data in income_catalog.items():
        cursor.execute(
            "INSERT INTO categories (name, type, color, icon, is_default) VALUES (?, 'income', ?, ?, 1);",
            (cat_name, cat_data["color"], cat_data["icon"])
        )
        cat_id = cursor.lastrowid
        for sub in cat_data["sub"]:
            cursor.execute(
                "INSERT INTO subcategories (category_id, name) VALUES (?, ?);",
                (cat_id, sub)
            )
