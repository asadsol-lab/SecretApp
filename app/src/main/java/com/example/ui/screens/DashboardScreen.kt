package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VerifiedPasswordType
import com.example.model.AuditItem
import com.example.model.CallType
import com.example.ui.AuditUiState
import com.example.ui.ItemTypeFilter
import com.example.ui.TimeFilter
import com.example.ui.components.ActivityDetailDialog
import com.example.ui.components.CallActivityCard
import com.example.ui.components.LendingSessionCard
import com.example.ui.components.SmsActivityCard
import com.example.ui.theme.CardDark
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: AuditUiState,
    onStartSession: (borrower: String) -> Unit,
    onEndSession: () -> Unit,
    onTimeFilterSelected: (TimeFilter) -> Unit,
    onTypeFilterSelected: (ItemTypeFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onItemClick: (AuditItem) -> Unit,
    onDismissDetail: () -> Unit,
    onRefresh: () -> Unit,
    onLockApp: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Cyan400.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Device Audit Log",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate200
                                )
                            )
                            val userLabel = if (uiState.authenticatedUserType == VerifiedPasswordType.MASTER_ADMIN) {
                                "Master Owner Access"
                            } else {
                                "Backup Password Access"
                            }
                            Text(
                                text = userLabel,
                                style = MaterialTheme.typography.labelSmall.copy(color = Cyan400),
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_button")) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Slate200)
                    }
                    IconButton(onClick = onOpenArchive, modifier = Modifier.testTag("archive_button")) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Past Sessions", tint = Slate200)
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("settings_button")) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Slate200)
                    }
                    IconButton(onClick = onLockApp, modifier = Modifier.testTag("lock_app_button")) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock App", tint = Rose500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy800)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Navy900
    ) { innerPadding ->

        // Prepare unified filtered items
        val searchLower = uiState.searchQuery.lowercase().trim()

        val filteredCalls = uiState.calls.filter { call ->
            val matchesType = uiState.typeFilter != ItemTypeFilter.SMS_ONLY
            val matchesSearch = searchLower.isEmpty() ||
                    call.number.lowercase().contains(searchLower) ||
                    (call.cachedName?.lowercase()?.contains(searchLower) == true)
            matchesType && matchesSearch
        }.map { AuditItem.Call(it) }

        val filteredSms = uiState.sms.filter { sms ->
            val matchesType = uiState.typeFilter != ItemTypeFilter.CALLS_ONLY
            val matchesSearch = searchLower.isEmpty() ||
                    sms.address.lowercase().contains(searchLower) ||
                    (sms.cachedName?.lowercase()?.contains(searchLower) == true) ||
                    sms.body.lowercase().contains(searchLower)
            matchesType && matchesSearch
        }.map { AuditItem.Sms(it) }

        val combinedItems = (filteredCalls + filteredSms).sortedByDescending { it.timestamp }

        // Metrics counts
        val outgoingCount = uiState.calls.count { it.type == CallType.OUTGOING }
        val incomingCount = uiState.calls.count { it.type == CallType.INCOMING }
        val missedCount = uiState.calls.count { it.type == CallType.MISSED }
        val totalSmsCount = uiState.sms.size

        val currentSessionStart = uiState.activeSession?.startTime ?: 0L
        val callsDuringSession = if (currentSessionStart > 0) uiState.calls.count { it.timestamp >= currentSessionStart } else 0
        val smsDuringSession = if (currentSessionStart > 0) uiState.sms.count { it.timestamp >= currentSessionStart } else 0

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Permission Banner (if real permissions missing and demo mode is off)
            if (!uiState.hasCallLogPermission || !uiState.hasSmsPermission) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Navy800),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Device Access Permissions",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Cyan400
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Apne mobile ke live calls aur SMS read karne ke liye Android permissions ki zaroorat hai. (Demo mode active hai taake aap test kar sakein).",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRequestPermissions,
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("request_permissions_button")
                            ) {
                                Text("Grant Call & SMS Permissions", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Lending Session Tracker Card
            item {
                LendingSessionCard(
                    activeSession = uiState.activeSession,
                    onStartSession = onStartSession,
                    onEndSession = onEndSession,
                    callsDuringSession = callsDuringSession,
                    smsDuringSession = smsDuringSession
                )
            }

            // Quick Metrics Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChip(
                        title = "Outgoing",
                        count = outgoingCount,
                        color = Emerald400,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        title = "Incoming",
                        count = incomingCount,
                        color = Cyan400,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        title = "Missed",
                        count = missedCount,
                        color = Rose500,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        title = "SMS",
                        count = totalSmsCount,
                        color = Slate200,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Search number, name, or message...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Slate400)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Navy700,
                        focusedTextColor = Slate200,
                        unfocusedTextColor = Slate200
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )
            }

            // Filter Chips (Time & Type)
            item {
                Column {
                    // Time filters
                    val timeScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(timeScroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeFilter.values().forEach { filter ->
                            val isSelected = uiState.timeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTimeFilterSelected(filter) },
                                label = { Text(filter.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan400,
                                    selectedLabelColor = Navy900,
                                    containerColor = CardDark,
                                    labelColor = Slate200
                                ),
                                modifier = Modifier.testTag("filter_time_${filter.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Type filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemTypeFilter.values().forEach { filter ->
                            val isSelected = uiState.typeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTypeFilterSelected(filter) },
                                label = { Text(filter.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald400,
                                    selectedLabelColor = Navy900,
                                    containerColor = CardDark,
                                    labelColor = Slate200
                                ),
                                modifier = Modifier.weight(1f).testTag("filter_type_${filter.name}")
                            )
                        }
                    }
                }
            }

            // Header for Timeline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Log (${combinedItems.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    )

                    Surface(
                        color = Navy800,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = uiState.timeFilter.subtitle,
                            color = Slate400,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Empty state
            if (combinedItems.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Calls or SMS Found",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate200
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Is waqt ke doran koi call ya message record nahi hua, ya filters change kar ke 'All Time' check karein.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(combinedItems, key = {
                    when (it) {
                        is AuditItem.Call -> "call_${it.entry.id}"
                        is AuditItem.Sms -> "sms_${it.entry.id}"
                    }
                }) { item ->
                    when (item) {
                        is AuditItem.Call -> {
                            CallActivityCard(
                                call = item.entry,
                                onClick = { onItemClick(item) }
                            )
                        }
                        is AuditItem.Sms -> {
                            SmsActivityCard(
                                sms = item.entry,
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    uiState.selectedDetailItem?.let { item ->
        ActivityDetailDialog(
            item = item,
            onDismiss = onDismissDetail
        )
    }
}

@Composable
private fun MetricChip(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                color = color,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = title,
                color = Slate400,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
