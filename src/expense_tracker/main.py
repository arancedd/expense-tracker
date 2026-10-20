from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from expense_tracker.core.database import init_db
from expense_tracker.api.routes import router

app = FastAPI(
    title="Expense Tracker API",
    description="Backend personale per tracciamento spese, multi-conto, budget e integrazione Leonardo / Carte",
    version="0.1.0"
)

# CORS per consentire connessione da frontend web, PWA o localhost
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Registrazione rotte API
app.include_router(router, prefix="/api")


@app.on_event("startup")
def startup_event():
    init_db()


@app.get("/")
def root():
    return {
        "app": "Expense Tracker",
        "version": "0.1.0",
        "status": "online",
        "docs_url": "/docs",
        "endpoints": {
            "accounts": "/api/accounts",
            "categories": "/api/categories",
            "transactions": "/api/transactions",
            "budgets": "/api/budgets",
            "backup_snapshot": "/api/backup/snapshot",
            "backup_json": "/api/backup/export/json",
            "card_webhook": "/api/webhooks/card-notification"
        }
    }


def main():
    import uvicorn
    init_db()
    uvicorn.run("expense_tracker.main:app", host="127.0.0.1", port=8000, reload=True)


if __name__ == "__main__":
    main()
