package com.edoardo.expensetracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.edoardo.expensetracker.data.local.ExpenseDatabase
import com.edoardo.expensetracker.data.local.entity.AccountType
import com.edoardo.expensetracker.data.local.entity.TransactionEntity
import com.edoardo.expensetracker.data.local.entity.TransactionSource
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.service.parser.BankNotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import java.util.Locale

class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()

        if (!BankNotificationParser.isBankNotification(packageName, title, text)) {
            return
        }

        val parsed = BankNotificationParser.parse(packageName, title, text) ?: return

        serviceScope.launch {
            val database = ExpenseDatabase.getDatabase(applicationContext, serviceScope)
            val accountDao = database.accountDao()
            val transactionDao = database.transactionDao()

            // Cerca il conto corrispondente alla carta bancaria (es. "Revolut", "Hype")
            val account = accountDao.findAccountByKeyword(parsed.bankKeyword)
                ?: accountDao.getActiveAccounts().firstOrNull { it.accountType == AccountType.CREDIT_CARD }
                ?: accountDao.getActiveAccounts().firstOrNull()

            if (account != null) {
                val newTx = TransactionEntity(
                    accountId = account.id,
                    type = TransactionType.EXPENSE,
                    amount = parsed.amount,
                    payee = parsed.payee ?: "Pagamento Carta",
                    note = "Catturato automaticamente da notifica: ${parsed.rawText.take(50)}",
                    source = TransactionSource.CARD_NOTIFICATION,
                    dateMillis = System.currentTimeMillis()
                )
                transactionDao.insertTransaction(newTx)

                showExpenseCapturedNotification(parsed.amount, parsed.payee, account.name)
            }
        }
    }

    private fun showExpenseCapturedNotification(amount: Double, payee: String?, accountName: String) {
        val channelId = "card_expenses_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Spese con Carta Rilevate",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifiche di tracciamento automatico dei pagamenti con carta"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "💳 Spesa Registrata: -${String.format(Locale.ITALY, "%.2f", amount)} €"
        val content = "${payee ?: "Esercente sconosciuto"} su $accountName"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
