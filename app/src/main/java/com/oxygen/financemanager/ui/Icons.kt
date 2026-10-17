package com.oxygen.financemanager.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Іконки, які вміє показати застосунок. Колонка `icon` зберігає ім'я з префіксом
 * джерела: `ms:savings`. Мапа явна, а не через рефлексію: рефлексія повільна, і R8
 * не зміг би вирізати невикористане, якби імена збирались у рантаймі.
 *
 * Порядок — порядок показу у виборі: спершу побут, далі транспорт, дозвілля, гроші.
 */
private val symbols: Map<String, ImageVector> by lazy {
    linkedMapOf(
        "shopping_basket" to Icons.Default.ShoppingBasket,
        "restaurant" to Icons.Default.Restaurant,
        "local_cafe" to Icons.Default.LocalCafe,
        "fastfood" to Icons.Default.Fastfood,
        "local_bar" to Icons.Default.LocalBar,
        "home" to Icons.Default.Home,
        "bolt" to Icons.Default.Bolt,
        "water_drop" to Icons.Default.WaterDrop,
        "wifi" to Icons.Default.Wifi,
        "phone_iphone" to Icons.Default.PhoneIphone,
        "construction" to Icons.Default.Construction,
        "handyman" to Icons.Default.Handyman,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "checkroom" to Icons.Default.Checkroom,
        "laptop" to Icons.Default.Laptop,
        "sell" to Icons.Default.Sell,
        "local_offer" to Icons.Default.LocalOffer,
        "directions_bus" to Icons.Default.DirectionsBus,
        "train" to Icons.Default.Train,
        "directions_car" to Icons.Default.DirectionsCar,
        "local_taxi" to Icons.Default.LocalTaxi,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "local_parking" to Icons.Default.LocalParking,
        "two_wheeler" to Icons.Default.TwoWheeler,
        "flight" to Icons.Default.Flight,
        "hotel" to Icons.Default.Hotel,
        "luggage" to Icons.Default.Luggage,
        "beach_access" to Icons.Default.BeachAccess,
        "medical_services" to Icons.Default.MedicalServices,
        "local_hospital" to Icons.Default.LocalHospital,
        "local_pharmacy" to Icons.Default.LocalPharmacy,
        "spa" to Icons.Default.Spa,
        "content_cut" to Icons.Default.ContentCut,
        "fitness_center" to Icons.Default.FitnessCenter,
        "pool" to Icons.Default.Pool,
        "theaters" to Icons.Default.Theaters,
        "movie" to Icons.Default.Movie,
        "music_note" to Icons.Default.MusicNote,
        "sports_esports" to Icons.Default.SportsEsports,
        "subscriptions" to Icons.Default.Subscriptions,
        "menu_book" to Icons.AutoMirrored.Filled.MenuBook,
        "school" to Icons.Default.School,
        "brush" to Icons.Default.Brush,
        "park" to Icons.Default.Park,
        "child_care" to Icons.Default.ChildCare,
        "pets" to Icons.Default.Pets,
        "cake" to Icons.Default.Cake,
        "celebration" to Icons.Default.Celebration,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "redeem" to Icons.Default.Redeem,
        "favorite" to Icons.Default.Favorite,
        "volunteer_activism" to Icons.Default.VolunteerActivism,
        "star" to Icons.Default.Star,
        "shield" to Icons.Default.Shield,
        "receipt" to Icons.Default.Receipt,
        "account_balance" to Icons.Default.AccountBalance,
        "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
        "credit_card" to Icons.Default.CreditCard,
        "payments" to Icons.Default.Payments,
        "savings" to Icons.Default.Savings,
        "work" to Icons.Default.Work,
        "business_center" to Icons.Default.BusinessCenter,
        "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
        "undo" to Icons.AutoMirrored.Filled.Undo,
        "more_horiz" to Icons.Default.MoreHoriz,
        // службова — для переказів у списку, не пропонується у виборі
        "swap_horiz" to Icons.Default.SwapHoriz,
    )
}

fun iconFor(spec: String): ImageVector? = symbols[spec.removePrefix("ms:")]

/** Що пропонувати у виборі іконки категорії. */
val pickableIcons: List<String> by lazy { symbols.keys.filterNot { it == "swap_horiz" }.map { "ms:$it" } }
