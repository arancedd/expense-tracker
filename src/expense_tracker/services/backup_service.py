import os
import shutil
import json
import csv
from datetime import datetime
from pathlib import Path
from typing import Dict, Any
from expense_tracker.core.database import get_db, get_db_path, DATA_DIR


BACKUP_DIR = DATA_DIR / "backups"


class BackupService:
    @staticmethod
    def create_sqlite_snapshot() -> Path:
        """Crea una copia binaria timestamped del database SQLite in data/backups/."""
        BACKUP_DIR.mkdir(parents=True, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_file = BACKUP_DIR / f"expenses_backup_{timestamp}.db"
        
        # Effettua un backup deterministico usando l'API SQLite
        source_db = get_db_path()
        if source_db.exists():
            shutil.copy2(source_db, backup_file)
        return backup_file

    @staticmethod
    def export_to_json() -> Dict[str, Any]:
        """Esporta l'intero contenuto del database in formato JSON portatile."""
        with get_db() as conn:
            cursor = conn.cursor()
            
            cursor.execute("SELECT * FROM accounts;")
            accounts = [dict(row) for row in cursor.fetchall()]

            cursor.execute("SELECT * FROM categories;")
            categories = [dict(row) for row in cursor.fetchall()]

            cursor.execute("SELECT * FROM subcategories;")
            subcategories = [dict(row) for row in cursor.fetchall()]

            cursor.execute("SELECT * FROM transactions;")
            transactions = [dict(row) for row in cursor.fetchall()]

            cursor.execute("SELECT * FROM budgets;")
            budgets = [dict(row) for row in cursor.fetchall()]

            return {
                "exported_at": datetime.now().isoformat(),
                "version": "1.0",
                "accounts": accounts,
                "categories": categories,
                "subcategories": subcategories,
                "transactions": transactions,
                "budgets": budgets
            }

    @staticmethod
    def export_transactions_csv(file_path: Path):
        """Esporta la lista delle transazioni in formato CSV compatibile Excel/Google Sheets."""
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT 
                    t.id, t.date, t.time, t.type, t.amount,
                    a.name AS account,
                    c.name AS category,
                    s.name AS subcategory,
                    t.payee, t.note, t.source
                FROM transactions t
                JOIN accounts a ON t.account_id = a.id
                LEFT JOIN categories c ON t.category_id = c.id
                LEFT JOIN subcategories s ON t.subcategory_id = s.id
                ORDER BY t.date DESC, t.time DESC
            """)
            rows = cursor.fetchall()

            with open(file_path, mode="w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow([
                    "ID", "Data", "Ora", "Tipo", "Importo (EUR)", 
                    "Conto", "Categoria", "Sotto-categoria", "Esercente", "Note", "Origine"
                ])
                for r in rows:
                    writer.writerow([
                        r["id"], r["date"], r["time"], r["type"], r["amount"],
                        r["account"], r["category"] or "", r["subcategory"] or "",
                        r["payee"] or "", r["note"] or "", r["source"]
                    ])
