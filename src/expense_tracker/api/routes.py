from fastapi import APIRouter, HTTPException, Query
from typing import List, Optional
from expense_tracker.core.models import (
    AccountCreate, AccountResponse,
    TransactionCreate, TransactionResponse,
    CategoryResponse, BudgetCreate, BudgetResponse,
    CardNotificationPayload
)
from expense_tracker.services.account_service import AccountService
from expense_tracker.services.transaction_service import TransactionService
from expense_tracker.services.budget_service import BudgetService
from expense_tracker.services.backup_service import BackupService

router = APIRouter()


# --- Conti (Accounts) ---
@router.get("/accounts", response_model=List[AccountResponse], tags=["Conti"])
def list_accounts(include_inactive: bool = False):
    return AccountService.get_all_accounts(include_inactive)


@router.post("/accounts", response_model=AccountResponse, tags=["Conti"])
def create_account(account: AccountCreate):
    return AccountService.create_account(account)


# --- Categorie & Sottocategorie ---
@router.get("/categories", response_model=List[CategoryResponse], tags=["Categorie"])
def list_categories(type: Optional[str] = Query(None, description="'expense' o 'income'")):
    return TransactionService.get_categories(type)


# --- Transazioni (Uscite, Entrate, Giroconti) ---
@router.get("/transactions", response_model=List[TransactionResponse], tags=["Transazioni"])
def list_transactions(
    account_id: Optional[int] = None,
    category_id: Optional[int] = None,
    type: Optional[str] = None,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    search: Optional[str] = None,
    limit: int = 100
):
    return TransactionService.list_transactions(
        account_id=account_id,
        category_id=category_id,
        tx_type=type,
        start_date=start_date,
        end_date=end_date,
        search=search,
        limit=limit
    )


@router.post("/transactions", response_model=TransactionResponse, tags=["Transazioni"])
def create_transaction(tx: TransactionCreate):
    try:
        return TransactionService.create_transaction(tx)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


# --- Budgeting ---
@router.get("/budgets", response_model=List[BudgetResponse], tags=["Budget"])
def list_budgets(month: Optional[str] = Query(None, description="Formato YYYY-MM")):
    return BudgetService.get_all_budgets_for_month(month)


@router.post("/budgets", response_model=BudgetResponse, tags=["Budget"])
def set_budget(budget: BudgetCreate):
    return BudgetService.set_budget(budget)


# --- Backup & Ripristino ---
@router.post("/backup/snapshot", tags=["Backup"])
def take_sqlite_snapshot():
    path = BackupService.create_sqlite_snapshot()
    return {"status": "ok", "backup_file": str(path)}


@router.get("/backup/export/json", tags=["Backup"])
def export_json():
    return BackupService.export_to_json()


# --- Integrazione Carte & Leonardo Webhook ---
@router.post("/webhooks/card-notification", response_model=TransactionResponse, tags=["Integrazioni"])
def ingest_card_notification(payload: CardNotificationPayload):
    """
    Riceve testo grezzo da notifiche push (MacroDroid, Tasker, Leonardo)
    ed effettua l'estrazione automatica di importo ed esercente.
    """
    tx = TransactionService.parse_card_notification(
        raw_text=payload.raw_notification_text,
        default_account_name=payload.account_name_or_keyword
    )
    if payload.payee:
        tx.payee = payload.payee
    if payload.amount and payload.amount > 0:
        tx.amount = payload.amount
    if payload.date:
        tx.date = payload.date

    return TransactionService.create_transaction(tx)
