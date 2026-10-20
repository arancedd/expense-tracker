from typing import List, Optional
from datetime import date
from expense_tracker.core.database import get_db
from expense_tracker.core.models import BudgetCreate, BudgetResponse


class BudgetService:
    @staticmethod
    def set_budget(budget: BudgetCreate) -> BudgetResponse:
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO budgets (category_id, month, amount_limit, alert_threshold)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(category_id, month) DO UPDATE SET
                    amount_limit = excluded.amount_limit,
                    alert_threshold = excluded.alert_threshold
                """,
                (budget.category_id, budget.month, budget.amount_limit, budget.alert_threshold)
            )
            b_id = cursor.lastrowid
            
        return BudgetService.get_budget(budget.category_id, budget.month)

    @staticmethod
    def get_budget(category_id: int, month: str) -> Optional[BudgetResponse]:
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                SELECT 
                    b.id, b.category_id, c.name AS category_name,
                    b.month, b.amount_limit, b.alert_threshold,
                    COALESCE((
                        SELECT SUM(t.amount) 
                        FROM transactions t 
                        WHERE t.category_id = b.category_id 
                          AND t.type = 'expense'
                          AND strftime('%Y-%m', t.date) = b.month
                    ), 0.0) AS amount_spent
                FROM budgets b
                JOIN categories c ON b.category_id = c.id
                WHERE b.category_id = ? AND b.month = ?
                """,
                (category_id, month)
            )
            row = cursor.fetchone()
            if not row:
                return None

            spent = row["amount_spent"]
            limit = row["amount_limit"]
            rem = limit - spent
            pct = round((spent / limit) * 100, 1) if limit > 0 else 0.0

            return BudgetResponse(
                id=row["id"],
                category_id=row["category_id"],
                category_name=row["category_name"],
                month=row["month"],
                amount_limit=limit,
                amount_spent=round(spent, 2),
                remaining_amount=round(rem, 2),
                percentage_used=pct,
                alert_threshold=row["alert_threshold"],
                is_over_budget=(spent > limit)
            )

    @staticmethod
    def get_all_budgets_for_month(month: Optional[str] = None) -> List[BudgetResponse]:
        if not month:
            month = date.today().strftime("%Y-%m")

        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                SELECT 
                    b.id, b.category_id, c.name AS category_name,
                    b.month, b.amount_limit, b.alert_threshold,
                    COALESCE((
                        SELECT SUM(t.amount) 
                        FROM transactions t 
                        WHERE t.category_id = b.category_id 
                          AND t.type = 'expense'
                          AND strftime('%Y-%m', t.date) = b.month
                    ), 0.0) AS amount_spent
                FROM budgets b
                JOIN categories c ON b.category_id = c.id
                WHERE b.month = ?
                ORDER BY b.amount_limit DESC
                """,
                (month,)
            )
            rows = cursor.fetchall()

            result = []
            for row in rows:
                spent = row["amount_spent"]
                limit = row["amount_limit"]
                rem = limit - spent
                pct = round((spent / limit) * 100, 1) if limit > 0 else 0.0
                result.append(
                    BudgetResponse(
                        id=row["id"],
                        category_id=row["category_id"],
                        category_name=row["category_name"],
                        month=row["month"],
                        amount_limit=limit,
                        amount_spent=round(spent, 2),
                        remaining_amount=round(rem, 2),
                        percentage_used=pct,
                        alert_threshold=row["alert_threshold"],
                        is_over_budget=(spent > limit)
                    )
                )
            return result
