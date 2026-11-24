package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutUser
import com.example.data.BusinessRepository
import com.example.data.Client
import com.example.data.ClientAccountSummary
import com.example.data.LunchEntry
import com.example.data.PaymentRecord
import com.example.ui.BusinessLedgerViewModel
import com.example.ui.UiState
import com.example.ui.components.ClientFormSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LunchEntrySheet
import com.example.ui.components.PaymentSheet
import com.example.ui.screens.ClientDetailScreen
import com.example.ui.screens.ClientesScreen
import com.example.ui.screens.HoyScreen
import com.example.ui.screens.PendientesScreen
import com.example.ui.screens.ResumenScreen
import com.example.ui.theme.LasCuentasDePatoTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LasCuentasDePatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

private enum class MainTab(
    val routeId: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOY("today", R.string.nav_today, Icons.Rounded.Restaurant, Icons.Outlined.Restaurant),
    PENDIENTES("pending", R.string.nav_pending, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    CLIENTES("clients", R.string.nav_clients, Icons.Rounded.People, Icons.Outlined.People),
    RESUMEN("summary", R.string.nav_summary, Icons.Rounded.BarChart, Icons.Outlined.BarChart)
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var checkedSilentSignIn by remember { mutableStateOf(Firebase.auth.currentUser != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null && !checkedSilentSignIn) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    checkedSilentSignIn = true
                },
                onUnauthenticated = {
                    checkedSilentSignIn = true
                },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            MainBusinessScaffold(
                user = user,
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = { currentUser = null }
                    )
                }
            )
        }
    }
}

@Composable
private fun MainBusinessScaffold(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val viewModel: BusinessLedgerViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BusinessLedgerViewModel(BusinessRepository(db), user.uid)
            }
        }
    )

    val ledgerState by viewModel.ledgerState.collectAsStateWithLifecycle()
    val selectedDateEpochDay by viewModel.selectedDateEpochDay.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var currentTab by remember { mutableStateOf(MainTab.HOY) }
    var selectedDetailClientId by remember { mutableStateOf<String?>(null) }

    // Modal Sheet States
    var showClientForm by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }

    var showLunchSheet by remember { mutableStateOf(false) }
    var lunchSheetInitialClient by remember { mutableStateOf<Client?>(null) }
    var editingLunchEntry by remember { mutableStateOf<LunchEntry?>(null) }

    var paymentSheetSummary by remember { mutableStateOf<ClientAccountSummary?>(null) }
    var editingPaymentRecord by remember { mutableStateOf<PaymentRecord?>(null) }

    val loadedLedger = (ledgerState as? UiState.Success)?.data
    val pendingDebtClientsCount = loadedLedger?.totalClientsWithDebt ?: 0

    // If viewing a specific client's detail ledger, show ClientDetailScreen with BackHandler
    val detailSummary = selectedDetailClientId?.let { id ->
        loadedLedger?.summariesByClientId?.get(id)
    }

    if (detailSummary != null) {
        ClientDetailScreen(
            summary = detailSummary,
            onBack = { selectedDetailClientId = null },
            onEditClient = { client ->
                editingClient = client
                showClientForm = true
            },
            onAddLunch = { client ->
                editingLunchEntry = null
                lunchSheetInitialClient = client
                showLunchSheet = true
            },
            onRegisterPayment = { summary ->
                editingPaymentRecord = null
                paymentSheetSummary = summary
            },
            onEditLunch = { entry ->
                editingLunchEntry = entry
                lunchSheetInitialClient = loadedLedger?.clients?.firstOrNull { it.id == entry.clientId }
                showLunchSheet = true
            },
            onDeleteLunch = { entry ->
                viewModel.deleteLunchEntry(entry.id, entry.clientNameSnapshot)
            },
            onEditPayment = { payment ->
                editingPaymentRecord = payment
                paymentSheetSummary = detailSummary
            },
            onDeletePayment = { payment ->
                viewModel.deletePayment(payment.id, payment.clientNameSnapshot)
            }
        )
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 700.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .navigationBarsPadding()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        pendingCount = pendingDebtClientsCount
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_${tab.routeId}")
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            TabIconWithBadge(
                                                tab = tab,
                                                selected = selected,
                                                pendingCount = pendingDebtClientsCount
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_${tab.routeId}")
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (loadedLedger != null) {
                            when (currentTab) {
                                MainTab.HOY -> {
                                    if (loadedLedger.clients.isNotEmpty()) {
                                        ExtendedFloatingActionButton(
                                            onClick = {
                                                editingLunchEntry = null
                                                lunchSheetInitialClient = null
                                                showLunchSheet = true
                                            },
                                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                            text = { Text(stringResource(R.string.today_add_lunch)) },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier.testTag("fab_add_lunch")
                                        )
                                    }
                                }
                                MainTab.CLIENTES -> {
                                    ExtendedFloatingActionButton(
                                        onClick = {
                                            editingClient = null
                                            showClientForm = true
                                        },
                                        icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                                        text = { Text(stringResource(R.string.clients_add)) },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.testTag("fab_add_client")
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp)
                        ) {
                            when (val state = ledgerState) {
                                is UiState.Loading -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = stringResource(R.string.loading_data),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                is UiState.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EmptyStateCard(
                                            icon = Icons.Rounded.Warning,
                                            title = stringResource(R.string.error_loading_data),
                                            subtitle = state.message
                                        )
                                    }
                                }

                                is UiState.Success -> {
                                    val ledger = state.data
                                    when (currentTab) {
                                        MainTab.HOY -> HoyScreen(
                                            ledger = ledger,
                                            selectedDateEpochDay = selectedDateEpochDay,
                                            onSelectDate = viewModel::selectDate,
                                            onShiftDate = viewModel::shiftSelectedDate,
                                            onJumpToday = viewModel::jumpToToday,
                                            onQuickAddLunch = { client ->
                                                editingLunchEntry = null
                                                lunchSheetInitialClient = client
                                                showLunchSheet = true
                                            },
                                            onEditLunch = { entry ->
                                                editingLunchEntry = entry
                                                lunchSheetInitialClient =
                                                    ledger.clients.firstOrNull { it.id == entry.clientId }
                                                showLunchSheet = true
                                            },
                                            onDeleteLunch = { entry ->
                                                viewModel.deleteLunchEntry(entry.id, entry.clientNameSnapshot)
                                            },
                                            onOpenPendingTab = { currentTab = MainTab.PENDIENTES },
                                            onAddFirstClient = {
                                                editingClient = null
                                                showClientForm = true
                                            },
                                            onOpenClientDetail = { id -> selectedDetailClientId = id }
                                        )

                                        MainTab.PENDIENTES -> PendientesScreen(
                                            ledger = ledger,
                                            onRegisterPayment = { summary ->
                                                editingPaymentRecord = null
                                                paymentSheetSummary = summary
                                            },
                                            onOpenClientDetail = { id -> selectedDetailClientId = id }
                                        )

                                        MainTab.CLIENTES -> ClientesScreen(
                                            ledger = ledger,
                                            onAddClient = {
                                                editingClient = null
                                                showClientForm = true
                                            },
                                            onEditClient = { client ->
                                                editingClient = client
                                                showClientForm = true
                                            },
                                            onToggleClientStatus = viewModel::toggleClientStatus,
                                            onQuickAddLunch = { client ->
                                                editingLunchEntry = null
                                                lunchSheetInitialClient = client
                                                showLunchSheet = true
                                            },
                                            onOpenClientDetail = { id -> selectedDetailClientId = id }
                                        )

                                        MainTab.RESUMEN -> ResumenScreen(
                                            ledger = ledger,
                                            userDisplayName = user.displayName,
                                            userEmail = user.email,
                                            onEditLunch = { entry ->
                                                editingLunchEntry = entry
                                                lunchSheetInitialClient =
                                                    ledger.clients.firstOrNull { it.id == entry.clientId }
                                                showLunchSheet = true
                                            },
                                            onDeleteLunch = { entry ->
                                                viewModel.deleteLunchEntry(entry.id, entry.clientNameSnapshot)
                                            },
                                            onEditPayment = { payment ->
                                                val summary = ledger.summariesByClientId[payment.clientId]
                                                if (summary != null) {
                                                    editingPaymentRecord = payment
                                                    paymentSheetSummary = summary
                                                }
                                            },
                                            onDeletePayment = { payment ->
                                                viewModel.deletePayment(payment.id, payment.clientNameSnapshot)
                                            },
                                            onSignOut = onSignOut
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Modal Bottom Sheets ---

    if (showClientForm) {
        ClientFormSheet(
            editingClient = editingClient,
            isSaving = isSaving,
            onSave = { fullName, phone, address, modality, agreedPrice, notes, status ->
                viewModel.saveClient(
                    editingClientId = editingClient?.id,
                    fullName = fullName,
                    phone = phone,
                    address = address,
                    modality = modality,
                    agreedPrice = agreedPrice,
                    notes = notes,
                    status = status,
                    onComplete = {
                        showClientForm = false
                        editingClient = null
                    }
                )
            },
            onDelete = { client ->
                viewModel.deleteClient(client) {
                    showClientForm = false
                    editingClient = null
                    if (selectedDetailClientId == client.id) {
                        selectedDetailClientId = null
                    }
                }
            },
            onDismiss = {
                showClientForm = false
                editingClient = null
            }
        )
    }

    if (showLunchSheet && loadedLedger != null) {
        LunchEntrySheet(
            clients = loadedLedger.clients,
            initialClient = lunchSheetInitialClient,
            editingEntry = editingLunchEntry,
            initialDateEpochDay = selectedDateEpochDay,
            isSaving = isSaving,
            onSave = { client, dateEpochDay, quantity, unitPrice, note ->
                viewModel.saveLunchEntry(
                    editingEntryId = editingLunchEntry?.id,
                    client = client,
                    dateEpochDay = dateEpochDay,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    note = note,
                    onComplete = {
                        showLunchSheet = false
                        editingLunchEntry = null
                        lunchSheetInitialClient = null
                    }
                )
            },
            onDismiss = {
                showLunchSheet = false
                editingLunchEntry = null
                lunchSheetInitialClient = null
            }
        )
    }

    paymentSheetSummary?.let { summary ->
        PaymentSheet(
            summary = summary,
            editingPayment = editingPaymentRecord,
            isSaving = isSaving,
            onSave = { client, dateEpochDay, amount, method, paymentType, note ->
                viewModel.savePayment(
                    editingPaymentId = editingPaymentRecord?.id,
                    client = client,
                    dateEpochDay = dateEpochDay,
                    amount = amount,
                    method = method,
                    paymentType = paymentType,
                    note = note,
                    onComplete = {
                        paymentSheetSummary = null
                        editingPaymentRecord = null
                    }
                )
            },
            onDismiss = {
                paymentSheetSummary = null
                editingPaymentRecord = null
            }
        )
    }
}

@Composable
private fun TabIconWithBadge(
    tab: MainTab,
    selected: Boolean,
    pendingCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == MainTab.PENDIENTES && pendingCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(if (pendingCount > 99) "99+" else pendingCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = null)
        }
    } else {
        Icon(imageVector = icon, contentDescription = null)
    }
}
