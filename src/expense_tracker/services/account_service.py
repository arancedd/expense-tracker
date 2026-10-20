from typing import List, Optional
from expense_tracker.core.database import get_db
from expense_tracker.core.models import AccountCreate, AccountResponse


class AccountService:
    @staticmethod
    def get_all_accounts(include_inactive: bool = False) -> List[AccountResponse]:
        with get_db() as conn:
            cursor = conn.cursor()
            query = """
                SELECT 
                    a.id, a.name, a.account_type, a.initial_balance, a.currency, 
                    a.color, a.icon, a.is_active, a.created_at,
                    (
                        a.initial_balance
                        + COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'income'), 0)
                        - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'expense'), 0)
                        - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'transfer'), 0)
                        + COALESCE((SELECT SUM(amount) FROM transactions WHERE destination_account_id = a.id AND type = 'transfer'), 0)
                    ) AS current_balance
                FROM accounts a
            """
            if not include_inactive:
                query += " WHERE a.is_active = 1"
            query += " ORDER BY a.id ASC"
            
            cursor.execute(query)
            rows = cursor.fetchall()
            
            return [
                AccountResponse(
                    id=row["id"],
                    name=row["name"],
                    account_type=row["account_type"],
                    initial_balance=row["initial_balance"],
                    currency=row["currency"],
                    color=row["color"],
                    icon=row["icon"],
                    is_active=bool(row["is_active"]),
                    current_balance=round(row["current_balance"], 2),
                    created_at=row["created_at"]
                )
                for row in rows
            ]

    @staticmethod
    def get_account_by_id(account_id: int) -> Optional[AccountResponse]:
        accounts = AccountService.get_all_accounts(include_inactive=True)
        for acc in accounts:
            if acc.id == account_id:
                return acc
        return None

    @staticmethod
    def create_account(account: AccountCreate) -> AccountResponse:
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO accounts (name, account_type, initial_balance, currency, color, icon, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (account.name, account.account_type, account.initial_balance, account.currency, account.color, account.icon, int(account.is_active))
            )
            acc_id = cursor.lastrowid
            
        return AccountService.get_account_by_id(acc_id)
