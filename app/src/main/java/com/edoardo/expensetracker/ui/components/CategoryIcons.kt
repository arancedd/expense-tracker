package com.edoardo.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/**
 * Mappatura unificata e catalogo completo (70+ icone) per Categorie, Sottocategorie e Finanza Personale.
 * Garantisce che l'icona selezionata nel picker corrisponda ESATTAMENTE a quella visualizzata
 * in tutta l'applicazione (Overview, Sheet, Transazioni, Budget).
 */
val CATEGORY_ICONS_LIST: List<Pair<String, ImageVector>> = listOf(
    // Cibo & Alimentari
    "shopping_cart" to Icons.Default.ShoppingCart,
    "shopping_bag" to Icons.Default.ShoppingBag,
    "shopping_basket" to Icons.Default.ShoppingBasket,
    "restaurant" to Icons.Default.Restaurant,
    "local_cafe" to Icons.Default.LocalCafe,
    "local_bar" to Icons.Default.LocalBar,
    "fastfood" to Icons.Default.Fastfood,
    "local_grocery_store" to Icons.Default.LocalGroceryStore,
    "local_pizza" to Icons.Default.LocalPizza,
    "bakery_dining" to Icons.Default.BakeryDining,
    "icecream" to Icons.Default.Icecream,

    // Casa, Utenze & Arredamento
    "home" to Icons.Default.Home,
    "apartment" to Icons.Default.Apartment,
    "lightbulb" to Icons.Default.Lightbulb,
    "water_drop" to Icons.Default.WaterDrop,
    "wifi" to Icons.Default.Wifi,
    "bolt" to Icons.Default.Bolt,
    "cleaning_services" to Icons.Default.CleaningServices,
    "handyman" to Icons.Default.Handyman,
    "chair" to Icons.Default.Chair,
    "bed" to Icons.Default.Bed,
    "tv" to Icons.Default.Tv,
    "router" to Icons.Default.Router,

    // Trasporti, Veicoli & Viaggi
    "directions_car" to Icons.Default.DirectionsCar,
    "directions_bus" to Icons.Default.DirectionsBus,
    "local_gas_station" to Icons.Default.LocalGasStation,
    "local_parking" to Icons.Default.LocalParking,
    "train" to Icons.Default.Train,
    "subway" to Icons.Default.Subway,
    "flight" to Icons.Default.Flight,
    "two_wheeler" to Icons.Default.TwoWheeler,
    "directions_bike" to Icons.AutoMirrored.Filled.DirectionsBike,
    "local_taxi" to Icons.Default.LocalTaxi,
    "commute" to Icons.Default.Commute,
    "hotel" to Icons.Default.Hotel,
    "beach_access" to Icons.Default.BeachAccess,
    "luggage" to Icons.Default.Luggage,

    // Salute, Cura Personale & Sport
    "local_hospital" to Icons.Default.LocalHospital,
    "fitness_center" to Icons.Default.FitnessCenter,
    "spa" to Icons.Default.Spa,
    "medication" to Icons.Default.Medication,
    "medical_services" to Icons.Default.MedicalServices,
    "favorite" to Icons.Default.Favorite,
    "pool" to Icons.Default.Pool,
    "sports_soccer" to Icons.Default.SportsSoccer,
    "sports_basketball" to Icons.Default.SportsBasketball,

    // Svago, Media & Intrattenimento
    "sports_esports" to Icons.Default.SportsEsports,
    "videogame_asset" to Icons.Default.VideogameAsset,
    "movie" to Icons.Default.Movie,
    "music_note" to Icons.Default.MusicNote,
    "headphones" to Icons.Default.Headphones,
    "confirmation_number" to Icons.Default.ConfirmationNumber,
    "celebration" to Icons.Default.Celebration,
    "camera_alt" to Icons.Default.CameraAlt,
    "theater_comedy" to Icons.Default.TheaterComedy,

    // Shopping, Abbigliamento & Tecnologia
    "checkroom" to Icons.Default.Checkroom,
    "watch" to Icons.Default.Watch,
    "diamond" to Icons.Default.Diamond,
    "brush" to Icons.Default.Brush,
    "smartphone" to Icons.Default.Smartphone,
    "computer" to Icons.Default.Computer,
    "devices" to Icons.Default.Devices,

    // Famiglia, Istruzione & Animali
    "pets" to Icons.Default.Pets,
    "school" to Icons.Default.School,
    "menu_book" to Icons.AutoMirrored.Filled.MenuBook,
    "child_care" to Icons.Default.ChildCare,
    "family_restroom" to Icons.Default.FamilyRestroom,
    "science" to Icons.Default.Science,

    // Finanza, Lavoro & Risparmio
    "work" to Icons.Default.Work,
    "payments" to Icons.Default.Payments,
    "account_balance" to Icons.Default.AccountBalance,
    "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
    "monetization_on" to Icons.Default.MonetizationOn,
    "savings" to Icons.Default.Savings,
    "receipt" to Icons.AutoMirrored.Filled.ReceiptLong,
    "credit_card" to Icons.Default.CreditCard,
    "wallet" to Icons.Default.AccountBalanceWallet,
    "euro" to Icons.Default.Euro,
    "currency_exchange" to Icons.Default.CurrencyExchange,
    "paid" to Icons.Default.Paid,
    "price_change" to Icons.Default.PriceChange,
    "subscriptions" to Icons.Default.Subscriptions,
    "card_giftcard" to Icons.Default.CardGiftcard,
    "local_offer" to Icons.Default.LocalOffer,
    "build" to Icons.Default.Build,
    "category" to Icons.Default.Category
)

val AVAILABLE_ICONS = CATEGORY_ICONS_LIST

private val ICONS_MAP: Map<String, ImageVector> = CATEGORY_ICONS_LIST.toMap()

/**
 * Restituisce l'ImageVector corrispondente a una chiave di icona o a un nome categoria.
 * Cerca prima per chiave esatta (es. "shopping_cart", "restaurant"), poi per fallback semantico.
 */
fun getCategoryIconByName(keyOrName: String): ImageVector {
    val lower = keyOrName.trim().lowercase(Locale.ITALY)
    
    // 1. Corrispondenza diretta per chiave icona salvata
    ICONS_MAP[lower]?.let { return it }

    // 2. Corrispondenza semantica per categorie generate o migrate
    return when {
        lower.contains("cibo") || lower.contains("spesa") || lower.contains("ristorant") || lower.contains("bar") || lower.contains("alimentari") || lower.contains("supermarket") || lower.contains("grocer") -> Icons.Default.ShoppingCart
        lower.contains("trasport") || lower.contains("auto") || lower.contains("benzina") || lower.contains("carburante") || lower.contains("treno") || lower.contains("metro") || lower.contains("taxi") -> Icons.Default.DirectionsCar
        lower.contains("casa") || lower.contains("affitto") || lower.contains("mutuo") || lower.contains("bollette") || lower.contains("luce") || lower.contains("gas") || lower.contains("apartment") -> Icons.Default.Home
        lower.contains("svago") || lower.contains("divertimento") || lower.contains("cinema") || lower.contains("giochi") || lower.contains("hobby") -> Icons.Default.ConfirmationNumber
        lower.contains("shopping") || lower.contains("abbigliamento") || lower.contains("vestiti") || lower.contains("acquisti") -> Icons.Default.ShoppingBag
        lower.contains("salute") || lower.contains("farmacia") || lower.contains("medico") || lower.contains("visite") -> Icons.Default.LocalHospital
        lower.contains("sport") || lower.contains("palestra") || lower.contains("fitness") -> Icons.Default.FitnessCenter
        lower.contains("stipendio") || lower.contains("lavoro") || lower.contains("salary") -> Icons.Default.Work
        lower.contains("saas") || lower.contains("progetti") || lower.contains("freelance") || lower.contains("business") -> Icons.Default.Devices
        lower.contains("investiment") || lower.contains("dividendi") || lower.contains("crypto") || lower.contains("trading") -> Icons.AutoMirrored.Filled.TrendingUp
        lower.contains("rimbors") || lower.contains("regal") || lower.contains("bonus") || lower.contains("gift") -> Icons.Default.CardGiftcard
        lower.contains("viagg") || lower.contains("vacanze") || lower.contains("hotel") || lower.contains("volo") -> Icons.Default.Flight
        lower.contains("istruzion") || lower.contains("corsi") || lower.contains("libri") || lower.contains("studio") -> Icons.AutoMirrored.Filled.MenuBook
        lower.contains("tecnologia") || lower.contains("elettronica") -> Icons.Default.Computer
        lower.contains("abbonament") || lower.contains("streaming") || lower.contains("subscript") -> Icons.Default.Subscriptions
        lower.contains("animal") || lower.contains("pets") || lower.contains("cane") || lower.contains("gatto") -> Icons.Default.Pets
        else -> Icons.Default.Category
    }
}
