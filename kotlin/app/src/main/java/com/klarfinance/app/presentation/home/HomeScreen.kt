package com.klarfinance.app.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.presentation.components.AppBottomBar
import kotlinx.coroutines.launch

/**
 * Public homepage (PRD "Fitur Penelusuran Awal - Masuk Homepage Tanpa Login"): the app's
 * default landing screen, browsable without an account. There is no session/token store
 * yet (see kotlin-nasabah-app knowledge), so [accountState] only reflects reality for the
 * live register->dashboard handoff within a single app session - cold start always starts
 * back at [AccountState.GUEST] until a real session store exists to persist/re-derive it.
 *
 * Three states, three different behaviors for the exact same locked-looking UI:
 * - [AccountState.GUEST]: no account yet - every locked tap routes to Login.
 * - [AccountState.PENDING_APPLICATION]: account exists (KYC submitted), application still
 *   under review, no active limit - locked taps show a "still under review" dialog instead
 *   (routing to Login here would be nonsense, the user IS logged in).
 * - [AccountState.ACTIVE]: has an approved limit - most of this UI is still not real
 *   screens, so locked taps show a "coming soon" snackbar instead.
 */
@Composable
fun HomeScreen(
    onLoginRequested: () -> Unit,
    onAccountClick: () -> Unit,
    onRequestLoanClick: () -> Unit = {},
    onPayClick: () -> Unit = {},
    accountState: AccountState = AccountState.GUEST,
    limitSummary: LimitSummary? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPendingDialog by remember { mutableStateOf(false) }
    var showQrisIneligibleDialog by remember { mutableStateOf(false) }

    val onLockedFeatureClick: () -> Unit = {
        when (accountState) {
            AccountState.GUEST -> onLoginRequested()
            AccountState.PENDING_APPLICATION -> showPendingDialog = true
            AccountState.ACTIVE -> scope.launch { snackbarHostState.showSnackbar("Fitur ini akan segera hadir") }
        }
    }

    // "Pay" (QRIS) beda dari quick action lain - kalau accountState ACTIVE tapi plafond belum
    // eligible (QrisPolicy.isEligible di backend, < Rp2jt), tampilin dialog spesifik TANPA buka
    // kamera sama sekali (hemat izin kamera sia-sia) - backend (scan()) tetap validasi ulang
    // beneran, ini cuma UX di klien.
    val onPayTap: () -> Unit = {
        when {
            accountState != AccountState.ACTIVE -> onLockedFeatureClick()
            limitSummary?.isQrisEligible != true -> showQrisIneligibleDialog = true
            else -> onPayClick()
        }
    }

    // Account info + change password work regardless of loan-approval status - unlike
    // Loans/History (still no real screens), so this bypasses onLockedFeatureClick entirely
    // once an account exists at all.
    val onProfileClick: () -> Unit = {
        if (accountState == AccountState.GUEST) onLoginRequested() else onAccountClick()
    }

    // Once an account exists, there's nothing behind Home to go back to (RegisterSuccess's
    // popUpTo already dropped Login/OtpVerification/RegisterGraph from the backstack - see
    // KlarNavHost) - this just makes sure system back can't ever land the user back in
    // Login/Register even if that backstack assumption ever changes.
    BackHandler(enabled = accountState != AccountState.GUEST) {}

    if (showPendingDialog) {
        PendingApplicationDialog(onDismiss = { showPendingDialog = false })
    }
    if (showQrisIneligibleDialog) {
        QrisIneligibleDialog(onDismiss = { showQrisIneligibleDialog = false })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomBar(
                hasAccount = accountState != AccountState.GUEST,
                activeTab = "Home",
                onLoginRequested = onLoginRequested,
                onHomeClick = {},
                onAccountClick = onAccountClick,
                onLockedTabClick = onLockedFeatureClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            HomeTopBar(onProfileClick = onProfileClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Good Morning, User",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Here is your financial summary for today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (accountState != AccountState.GUEST) {
                Spacer(modifier = Modifier.height(20.dp))
                AvailableLoanCard(
                    accountState = accountState,
                    limitSummary = limitSummary,
                    onClick = onLockedFeatureClick,
                    onRequestLoanClick = onRequestLoanClick,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            QuickActionsRow(onClick = onLockedFeatureClick, onPayClick = onPayTap)

            Spacer(modifier = Modifier.height(28.dp))
            SectionHeader(title = "Spesial cuma buat kamu", onClick = onLockedFeatureClick)
            Spacer(modifier = Modifier.height(12.dp))
            PromoCarousel(onClick = onLockedFeatureClick)

            Spacer(modifier = Modifier.height(24.dp))
            ExploreFeaturesCard(accountState = accountState, onClick = onLockedFeatureClick)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PendingApplicationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = KlarTeal) },
        title = { Text("Pengajuan Masih Diproses") },
        text = {
            Text(
                "Pengajuan pinjaman Anda masih dijalankan. Silakan tunggu 2-3 hari kerja untuk hasil persetujuan.",
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Mengerti", color = KlarTeal) }
        },
    )
}

@Composable
private fun QrisIneligibleDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QRIS Belum Tersedia") },
        text = {
            Text(
                "Fitur bayar QRIS baru tersedia untuk plafond minimal Rp 2.000.000. Plafond Anda saat ini belum memenuhi syarat.",
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Mengerti", color = KlarTeal) }
        },
    )
}

@Composable
private fun HomeTopBar(onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "K",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "KlarFinance",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun AvailableLoanCard(
    accountState: AccountState,
    limitSummary: LimitSummary?,
    onClick: () -> Unit,
    onRequestLoanClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        // limitSummary is only ever non-null once HomeViewModel's GET /loan/limit call
        // resolves - ACTIVE-but-still-loading briefly falls through to the loading row below
        // rather than flashing stale/fabricated numbers.
        when {
            accountState == AccountState.ACTIVE && limitSummary != null ->
                ActiveLoanCardContent(limitSummary, onRequestLoanClick)
            accountState == AccountState.ACTIVE -> LoadingLoanCardContent()
            else -> PendingLoanCardContent(onClick)
        }
    }
}

@Composable
private fun LoadingLoanCardContent() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = KlarTeal)
        Text(
            text = "Memuat data pinjaman...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PendingLoanCardContent(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AVAILABLE LOAN",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Pengajuan Sedang Diproses",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    LinearProgressIndicator(
        progress = { 0.15f },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
        color = MaterialTheme.colorScheme.outline,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onClick,
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text("Menunggu Persetujuan", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ActiveLoanCardContent(limitSummary: LimitSummary, onRequestLoanClick: () -> Unit) {
    val usedFraction = if (limitSummary.totalLimit > 0) {
        limitSummary.usedLimit.toFloat() / limitSummary.totalLimit.toFloat()
    } else {
        0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AVAILABLE LOAN",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = formatRupiah(limitSummary.totalLimit),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = "Used from ${formatRupiah(limitSummary.totalLimit).removePrefix("Rp ")}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))
    LinearProgressIndicator(
        progress = { usedFraction },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
        color = KlarTeal,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(text = "USED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = formatRupiah(limitSummary.usedLimit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "REMAINING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = formatRupiah(limitSummary.availableLimit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
    // Kuota QRIS (min(30% plafond, Rp1jt), konfirmasi user) - terpisah dari breakdown
    // USED/REMAINING di atas karena itu plafond utama, ini "jatah di dalamnya" khusus QRIS.
    // Null kalau plafond belum eligible (< Rp2jt, lihat HomeScreen.onPayTap).
    if (limitSummary.isQrisEligible) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "QRIS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${formatRupiah(limitSummary.qrisUsedAmount ?: 0L)} / ${formatRupiah(limitSummary.qrisQuota ?: 0L)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onRequestLoanClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = KlarTeal, contentColor = Color.White),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Ajukan Pinjaman", style = MaterialTheme.typography.labelLarge)
    }
}

private fun formatRupiah(amount: Long): String {
    val grouped = amount.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp $grouped"
}

private data class QuickAction(val label: String, val icon: ImageVector)

private val quickActions = listOf(
    QuickAction("Pay", Icons.Default.Payment),
    QuickAction("Top Up", Icons.Default.AccountBalance),
    QuickAction("Bills", Icons.Default.ReceiptLong),
    QuickAction("More", Icons.Default.MoreHoriz),
)

@Composable
private fun QuickActionsRow(onClick: () -> Unit, onPayClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        quickActions.forEach { action ->
            val actionClick = if (action.label == "Pay") onPayClick else onClick
            LockedIconAction(label = action.label, icon = action.icon, onClick = actionClick)
        }
    }
}

/**
 * Icon bubble + label used everywhere in this screen (quick actions, explore-feature
 * grid). [badge] renders as a small pill anchored to the icon bubble's own top-right
 * corner (not the whole column, which would drift off-icon for labels wider than the
 * bubble) - this is the piece that used to be a plain padding offset and looked broken.
 */
@Composable
private fun LockedIconAction(label: String, icon: ImageVector, onClick: () -> Unit, badge: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class Promo(val title: String, val subtitle: String, val icon: ImageVector, val gradient: List<Color>)

private val promos = listOf(
    Promo(
        title = "QRIS TAP",
        subtitle = "GRATIS ke mana aja!",
        icon = Icons.Default.QrCode2,
        gradient = listOf(Color(0xFF3E8F7C), Color(0xFF1F5C50)),
    ),
    Promo(
        title = "Transjakarta",
        subtitle = "GRATIS naik TJ",
        icon = Icons.Default.DirectionsBus,
        gradient = listOf(Color(0xFF2E6E8E), Color(0xFF1B4A63)),
    ),
)

@Composable
private fun PromoCarousel(onClick: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 20.dp),
    ) {
        items(promos) { promo -> PromoCard(promo = promo, onClick = onClick) }
    }
}

@Composable
private fun PromoCard(promo: Promo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(promo.gradient))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = promo.icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = promo.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = promo.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
    }
}

private data class ExploreFeature(val label: String, val icon: ImageVector, val badge: String? = null)
private data class FeatureCategory(val title: String, val features: List<ExploreFeature>)

// Only the first category (TRANSFER & TERIMA) is shown while GUEST/PENDING_APPLICATION,
// as a teaser - the rest unlock once accountState is ACTIVE. None of these route anywhere
// real yet.
private val featureCategories = listOf(
    FeatureCategory(
        title = "TRANSFER & TERIMA",
        features = listOf(
            ExploreFeature("Transfer gratis", Icons.Default.Send, badge = "FREE"),
            ExploreFeature("Transfer luar negeri", Icons.Default.Public),
            ExploreFeature("Split bill", Icons.Default.CallSplit),
            ExploreFeature("Hadiah", Icons.Default.CardGiftcard),
        ),
    ),
    FeatureCategory(
        title = "PEMBAYARAN",
        features = listOf(
            ExploreFeature("Tagihan saya", Icons.Default.ReceiptLong),
            ExploreFeature("eSIM", Icons.Default.SimCard),
            ExploreFeature("Transjakarta", Icons.Default.DirectionsBus),
            ExploreFeature("PLN", Icons.Default.Bolt),
        ),
    ),
    FeatureCategory(
        title = "PROMO",
        features = listOf(
            ExploreFeature("Voucher saya", Icons.Default.ConfirmationNumber),
            ExploreFeature("Check-in", Icons.Default.EventAvailable),
            ExploreFeature("Rewards", Icons.Default.Loyalty),
            ExploreFeature("Belanja", Icons.Default.ShoppingBag),
        ),
    ),
    FeatureCategory(
        title = "GAMES & HIBURAN",
        features = listOf(
            ExploreFeature("Ruby Zone", Icons.Default.Diamond),
            ExploreFeature("Treasure Hunt", Icons.Default.Explore),
            ExploreFeature("Noice", Icons.Default.Headphones),
            ExploreFeature("Tiket Bioskop", Icons.Default.LocalMovies),
        ),
    ),
)

@Composable
private fun ExploreFeaturesCard(accountState: AccountState, onClick: () -> Unit) {
    // GUEST gets a teaser (1 category); PENDING_APPLICATION and ACTIVE both see all 4 - the
    // tiles themselves are already styled/locked the same way regardless of state, tapping
    // one just routes to a different response (Login / "still under review" dialog / "coming
    // soon" snackbar) via onLockedFeatureClick in the parent.
    val visibleCategories = if (accountState == AccountState.GUEST) featureCategories.take(1) else featureCategories

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text(
            text = "Eksplor fitur KlarFinance",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        visibleCategories.forEachIndexed { index, category ->
            Spacer(modifier = Modifier.height(if (index == 0) 16.dp else 24.dp))
            SectionHeader(title = category.title, onClick = onClick)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                category.features.forEach { feature ->
                    LockedIconAction(label = feature.label, icon = feature.icon, onClick = onClick, badge = feature.badge)
                }
            }
        }
    }
}

