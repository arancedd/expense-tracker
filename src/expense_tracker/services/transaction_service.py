import re
from typing import List, Optional
from datetime import datetime, date
from expense_tracker.core.database import get_db
from expense_tracker.core.models import (
    TransactionCreate, TransactionResponse, 
    CategoryResponse, SubcategoryResponse
)


class TransactionService:
    @staticmethod
    def get_categories(category_type: Optional[str] = None) -> List[CategoryResponse]:
        with get_db() as conn:
            cursor = conn.cursor()
            query = "SELECT id, name, type, color, icon, is_default FROM categories"
            params = []
            if category_type:
                query += " WHERE type = ?"
                params.append(category_type)
            query += " ORDER BY id ASC"
            cursor.execute(query, params)
            cats = cursor.fetchall()

            result = []
            for cat in cats:
                cursor.execute(
                    "SELECT id, category_id, name FROM subcategories WHERE category_id = ? ORDER BY id ASC",
                    (cat["id"],)
                )
                subs = cursor.fetchall()
                result.append(
                    CategoryResponse(
                        id=cat["id"],
                        name=cat["name"],
                        type=cat["type"],
                        color=cat["color"],
                        icon=cat["icon"],
                        is_default=bool(cat["is_default"]),
                        subcategories=[
                            SubcategoryResponse(id=s["id"], category_id=s["category_id"], name=s["name"])
                            for s in subs
                        ]
                    )
                )
            return result

    @staticmethod
    def create_transaction(tx: TransactionCreate) -> TransactionResponse:
        if tx.type == "transfer" and not tx.destination_account_id:
            raise ValueError("Per un giroconto è obbligatorio specificare destination_account_id")
        if tx.type == "transfer" and tx.account_id == tx.destination_account_id:
            raise ValueError("Il conto di origine e destinazione non possono coincidere")

        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO transactions (
                    account_id, type, amount, category_id, subcategory_id,
                    destination_account_id, date, time, payee, note, source
                ) VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, time('now', 'localtime')), ?, ?, ?)
                """,
                (
                    tx.account_id, tx.type, tx.amount, tx.category_id, tx.subcategory_id,
                    tx.destination_account_id, tx.date, tx.time, tx.payee, tx.note, tx.source
                )
            )
            tx_id = cursor.lastrowid

        return TransactionService.get_transaction_by_id(tx_id)

    @staticmethod
    def get_transaction_by_id(tx_id: int) -> Optional[TransactionResponse]:
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                SELECT 
                    t.id, t.account_id, a.name AS account_name,
                    t.type, t.amount,
                    t.category_id, c.name AS category_name,
                    t.subcategory_id, s.name AS subcategory_name,
                    t.destination_account_id, da.name AS destination_account_name,
                    t.date, t.time, t.payee, t.note, t.source, t.created_at
                FROM transactions t
                JOIN accounts a ON t.account_id = a.id
                LEFT JOIN categories c ON t.category_id = c.id
                LEFT JOIN subcategories s ON t.subcategory_id = s.id
                LEFT JOIN accounts da ON t.destination_account_id = da.id
                WHERE t.id = ?
                """,
                (tx_id,)
            )
            row = cursor.fetchone()
            if not row:
                return None

            return TransactionResponse(
                id=row["id"],
                account_id=row["account_id"],
                account_name=row["account_name"],
                type=row["type"],
                amount=row["amount"],
                category_id=row["category_id"],
                category_name=row["category_name"],
                subcategory_id=row["subcategory_id"],
                subcategory_name=row["subcategory_name"],
                destination_account_id=row["destination_account_id"],
                destination_account_name=row["destination_account_name"],
                date=row["date"],
                time=row["time"],
                payee=row["payee"],
                note=row["note"],
                source=row["source"],
                created_at=row["created_at"]
            )

    @staticmethod
    def list_transactions(
        account_id: Optional[int] = None,
        category_id: Optional[int] = None,
        tx_type: Optional[str] = None,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        search: Optional[str] = None,
        limit: int = 100
    ) -> List[TransactionResponse]:
        with get_db() as conn:
            cursor = conn.cursor()
            query = """
                SELECT 
                    t.id, t.account_id, a.name AS account_name,
                    t.type, t.amount,
                    t.category_id, c.name AS category_name,
                    t.subcategory_id, s.name AS subcategory_name,
                    t.destination_account_id, da.name AS destination_account_name,
                    t.date, t.time, t.payee, t.note, t.source, t.created_at
                FROM transactions t
                JOIN accounts a ON t.account_id = a.id
                LEFT JOIN categories c ON t.category_id = c.id
                LEFT JOIN subcategories s ON t.subcategory_id = s.id
                LEFT JOIN accounts da ON t.destination_account_id = da.id
                WHERE 1=1
            """
            params = []

            if account_id:
                query += " AND (t.account_id = ? OR t.destination_account_id = ?)"
                params.extend([account_id, account_id])
            if category_id:
                query += " AND t.category_id = ?"
                params.append(category_id)
            if tx_type:
                query += " AND t.type = ?"
                params.append(tx_type)
            if start_date:
                query += " AND t.date >= ?"
                params.append(start_date)
            if end_date:
                query += " AND t.date <= ?"
                params.append(end_date)
            if search:
                query += " AND (t.payee LIKE ? OR t.note LIKE ?)"
                params.extend([f"%{search}%", f"%{search}%"])

            query += " ORDER BY t.date DESC, t.time DESC, t.id DESC LIMIT ?"
            params.append(limit)

            cursor.execute(query, params)
            rows = cursor.fetchall()

            return [
                TransactionResponse(
                    id=row["id"],
                    account_id=row["account_id"],
                    account_name=row["account_name"],
                    type=row["type"],
                    amount=row["amount"],
                    category_id=row["category_id"],
                    category_name=row["category_name"],
                    subcategory_id=row["subcategory_id"],
                    subcategory_name=row["subcategory_name"],
                    destination_account_id=row["destination_account_id"],
                    destination_account_name=row["destination_account_name"],
                    date=row["date"],
                    time=row["time"],
                    payee=row["payee"],
                    note=row["note"],
                    source=row["source"],
                    created_at=row["created_at"]
                )
                for row in rows
            ]

    @staticmethod
    def parse_card_notification(raw_text: str, default_account_name: Optional[str] = None) -> TransactionCreate:
        """
        Interpreta notifiche tipiche di pagamento con carta bancaria (es. Revolut, Hype, Intesa).
        Es: 'Pagamento di 14,50 EUR presso CONAD' oppure 'Hai speso 8.20 € da STARBUCKS'
        """
        # Estrazione importo
        amount_match = re.search(r"(\d+[\.,]\d{1,2})\s*(?:€|EUR)", raw_text, re.IGNORECASE)
        if not amount_match:
            amount_match = re.search(r"(?:€|EUR)\s*(\d+[\.,]\d{1,2})", raw_text, re.IGNORECASE)
            
        amount = 0.0
        if amount_match:
            amount_str = amount_match.group(1).replace(",", ".")
            amount = float(amount_str)

        # Estrazione esercente (payee)
        payee = None
        payee_match = re.search(r"(?:presso|da|a)\s+([A-Za-z0-9\s\.\-_&]+?)(?:\s+(?:con|effettuato|il|\.)|$)", raw_text, re.IGNORECASE)
        if payee_match:
            payee = payee_match.group(1).strip()

        # Determina conto
        with get_db() as conn:
            cursor = conn.cursor()
            account_id = None
            if default_account_name:
                cursor.execute("SELECT id FROM accounts WHERE name LIKE ? LIMIT 1", (f"%{default_account_name}%",))
                row = cursor.fetchone()
                if row:
                    account_id = row["id"]
            if not account_id:
                # Fallback sul primo conto carta o conto checking
                cursor.execute("SELECT id FROM accounts WHERE account_type = 'credit_card' LIMIT 1")
                row = cursor.fetchone()
                if not row:
                    cursor.execute("SELECT id FROM accounts ORDER BY id ASC LIMIT 1")
                    row = cursor.fetchone()
                account_id = row["id"] if row else 1

        return TransactionCreate(
            account_id=account_id,
            type="expense",
            amount=amount if amount > 0 else 1.0,
            payee=payee,
            note=f"Auto-captured: {raw_text[:80]}",
            source="card_webhook"
        )
