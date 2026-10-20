from datetime import date, time
from typing import Optional, Literal, List
from pydantic import BaseModel, Field


# --- Accounts ---
class AccountBase(BaseModel):
    name: str
    account_type: Literal["checking", "cash", "credit_card", "savings", "crypto"] = "checking"
    initial_balance: float = 0.0
    currency: str = "EUR"
    color: str = "#3B82F6"
    icon: str = "wallet"
    is_active: bool = True


class AccountCreate(AccountBase):
    pass


class AccountResponse(AccountBase):
    id: int
    current_balance: float
    created_at: str


# --- Categories & Subcategories ---
class SubcategoryBase(BaseModel):
    name: str


class SubcategoryCreate(SubcategoryBase):
    category_id: int


class SubcategoryResponse(SubcategoryBase):
    id: int
    category_id: int


class CategoryBase(BaseModel):
    name: str
    type: Literal["expense", "income"]
    color: str = "#64748B"
    icon: str = "tag"


class CategoryCreate(CategoryBase):
    pass


class CategoryResponse(CategoryBase):
    id: int
    is_default: bool
    subcategories: List[SubcategoryResponse] = []


# --- Transactions ---
class TransactionCreate(BaseModel):
    account_id: int
    type: Literal["expense", "income", "transfer"]
    amount: float = Field(..., gt=0, description="Importo positivo")
    category_id: Optional[int] = None
    subcategory_id: Optional[int] = None
    destination_account_id: Optional[int] = None  # Obbligatorio se type == 'transfer'
    date: str = Field(default_factory=lambda: date.today().isoformat())
    time: Optional[str] = None
    payee: Optional[str] = None
    note: Optional[str] = None
    source: str = "manual"  # 'manual', 'leonardo', 'card_webhook', 'csv'


class TransactionResponse(BaseModel):
    id: int
    account_id: int
    account_name: str
    type: Literal["expense", "income", "transfer"]
    amount: float
    category_id: Optional[int] = None
    category_name: Optional[str] = None
    subcategory_id: Optional[int] = None
    subcategory_name: Optional[str] = None
    destination_account_id: Optional[int] = None
    destination_account_name: Optional[str] = None
    date: str
    time: Optional[str] = None
    payee: Optional[str] = None
    note: Optional[str] = None
    source: str
    created_at: str


# --- Budgets ---
class BudgetCreate(BaseModel):
    category_id: int
    month: str = Field(..., pattern=r"^\d{4}-\d{2}$", description="Formato YYYY-MM")
    amount_limit: float = Field(..., gt=0)
    alert_threshold: float = 0.8


class BudgetResponse(BaseModel):
    id: int
    category_id: int
    category_name: str
    month: str
    amount_limit: float
    amount_spent: float
    remaining_amount: float
    percentage_used: float
    alert_threshold: float
    is_over_budget: bool


# --- Webhook & Card Notification Ingestion ---
class CardNotificationPayload(BaseModel):
    account_name_or_keyword: Optional[str] = None  # es. 'Revolut' o 'Hype'
    raw_notification_text: str  # es. "Pagamento di 14.50 EUR presso CONAD effettuato con successo"
    payee: Optional[str] = None
    amount: Optional[float] = None
    date: Optional[str] = None
