package com.edoardo.expensetracker.service.investment

import android.util.Log
import com.edoardo.expensetracker.data.local.entity.StockEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servizio globale per la ricerca e il recupero in tempo reale di quotazioni
 * di ETF, Indici di Borsa, Azioni mondiali e Criptovalute da provider di mercato aperti (Yahoo Finance).
 */
@Singleton
class MarketDataService @Inject constructor() {
    private val tag = "MarketDataService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Cerca online qualsiasi ETF, Indice, Azione o Cripto al mondo in tempo reale tramite query testuale.
     * Es: "MSCI World", "S&P 500", "VWCE", "Ferrari", "BTP", "Bitcoin".
     */
    suspend fun searchAssetsOnline(query: String): List<StockEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v1/finance/search?q=$encodedQuery&quotesCount=15&newsCount=0"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(tag, "Ricerca online fallita: HTTP ${response.code}")
                    return@withContext emptyList()
                }

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val quotesArray = json.optJSONArray("quotes") ?: return@withContext emptyList()

                val resultList = mutableListOf<StockEntity>()

                for (i in 0 until quotesArray.length()) {
                    val item = quotesArray.getJSONObject(i)
                    val symbol = item.optString("symbol").trim()
                    if (symbol.isBlank()) continue

                    val name = item.optString("longname").ifEmpty {
                        item.optString("shortname").ifEmpty { symbol }
                    }

                    val rawType = item.optString("quoteType").uppercase()
                    val assetType = when {
                        rawType == "ETF" -> StockEntity.TYPE_ETF
                        rawType == "INDEX" || symbol.startsWith("^") -> StockEntity.TYPE_INDEX
                        rawType in listOf("CRYPTOCURRENCY", "COIN") -> StockEntity.TYPE_CRYPTO
                        rawType in listOf("COMMODITY", "FUTURE") -> StockEntity.TYPE_COMMODITY
                        else -> StockEntity.TYPE_STOCK
                    }

                    resultList.add(
                        StockEntity(
                            symbol = symbol,
                            name = name,
                            currentPrice = 0.0, // Verrà popolato con fetchQuote se seguito
                            previousClosePrice = 0.0,
                            assetType = assetType,
                            isFollowed = false
                        )
                    )
                }

                return@withContext resultList
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore durante la ricerca online di '$query'", e)
            emptyList()
        }
    }

    /**
     * Scarica la quotazione di mercato reale aggiornata per un simbolo (es. "VWCE.DE", "^GSPC", "AAPL", "BTC-USD").
     * Se il simbolo non specifica una borsa (es. "VWCE", "SWDA"), tenta automaticamente i suffissi delle
     * principali borse europee (.DE, .MI, .PA, .L) per garantire la corrispondenza con i prezzi live.
     * Restituisce Pair(regularMarketPrice, chartPreviousClose) o null in caso di errore.
     */
    suspend fun fetchLatestPrice(symbol: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val trimmed = symbol.trim().uppercase()
        val candidates = if (trimmed.contains(".") || trimmed.startsWith("^") || trimmed.contains("-")) {
            listOf(trimmed)
        } else {
            listOf(trimmed, "$trimmed.DE", "$trimmed.MI", "$trimmed.PA", "$trimmed.L")
        }

        for (cand in candidates) {
            val quote = fetchSingleSymbolPrice(cand)
            if (quote != null) {
                return@withContext quote
            }
        }
        null
    }

    private fun fetchSingleSymbolPrice(symbol: String): Pair<Double, Double>? {
        return try {
            val encodedSymbol = URLEncoder.encode(symbol, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol?interval=1d&range=1d"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                val chartObj = json.optJSONObject("chart") ?: return null
                val resultArray = chartObj.optJSONArray("result") ?: return null
                if (resultArray.length() == 0) return null

                val metaObj = resultArray.getJSONObject(0).optJSONObject("meta") ?: return null

                val currentPrice = metaObj.optDouble("regularMarketPrice", Double.NaN)
                val previousClose = metaObj.optDouble("chartPreviousClose", currentPrice)

                if (currentPrice.isNaN() || currentPrice <= 0.0) return null

                Pair(currentPrice, if (previousClose.isNaN()) currentPrice else previousClose)
            }
        } catch (e: Exception) {
            null
        }
    }
}
