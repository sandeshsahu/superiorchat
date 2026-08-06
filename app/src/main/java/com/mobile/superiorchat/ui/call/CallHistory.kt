package com.mobile.superiorchat.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.data.entity.CallHistoryNode
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.bounceClick
import com.mobile.superiorchat.ui.components.glow
import com.mobile.superiorchat.ui.components.popups.InfoDialog
import com.mobile.superiorchat.ui.components.popups.ActionDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.format.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryPage(viewModel: CallViewModel = viewModel()) {
    val callHistory by viewModel.callHistory.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedCall by remember { mutableStateOf<CallHistoryNode?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showClearWarning by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.undoDeleteEvent.collect { node ->
            val result = snackbarHostState.showSnackbar(
                message = "Call log deleted",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteCall(node)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.undoBulkDeleteEvent.collect { nodes ->
            val result = snackbarHostState.showSnackbar(
                message = "History cleared",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoClearCallHistory(nodes)
            }
        }
    }

    if (showInfoDialog) {
        InfoDialog(
            title = "Recent Calls",
            message = "A history of all secure peer-to-peer WebRTC calls initiated from this device.",
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showClearWarning) {
        ActionDialog(
            title = "Clear History",
            message = "Are you sure you want to clear your entire call history? This will delete all logs.",
            icon = Icons.Filled.Warning,
            iconTint = ErrorRed,
            confirmText = "Clear",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.clearCallHistory()
                showClearWarning = false
            },
            onDismiss = { showClearWarning = false }
        )
    }

    if (selectedCall != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCall = null },
            sheetState = sheetState,
            containerColor = SurfaceLevel1,
            dragHandle = { BottomSheetDefaults.DragHandle(color = DividerColor) }
        ) {
            CallDetailsSheetContent(
                call = selectedCall!!,
                onDelete = {
                    viewModel.deleteCall(selectedCall!!)
                    selectedCall = null
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        // We use a LazyColumn for the entire page so the empty state can take up the remaining space
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing to 8dp
        ) {
            item {
                // Simple Header without background card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 8.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = null, 
                        tint = PrimaryLight, 
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Recent Calls", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { showInfoDialog = true }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (callHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearWarning = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete, 
                                contentDescription = "Clear History", 
                                tint = ErrorRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            if (callHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(0.7f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .glow(color = Primary, radius = 40f, dx = 0f, dy = 0f)
                                    .background(PrimaryLight, CircleShape)
                                    .border(1.dp, PrimaryLight.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = "Secure Chat",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("No calls yet", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your secure call logs will appear here.", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(callHistory) { call ->
                    CallHistoryRow(
                        call = call, 
                        onClick = { selectedCall = call }
                    )
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = SurfaceLevel2,
                contentColor = Color.White,
                actionColor = PrimaryLight
            )
        }
    }
}

@Composable
private fun CallHistoryRow(call: CallHistoryNode, onClick: () -> Unit) {
    val timeFormatted = formatCallTime(call.timestamp)
    val statusInfo = getStatusInfo(call)

    // Using two levels of background as requested
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.95f) { onClick() }
            .background(SurfaceLevel1, RoundedCornerShape(20.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceLevel2)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusInfo.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusInfo.icon,
                        contentDescription = null,
                        tint = statusInfo.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = call.partnerName, 
                        fontSize = 16.sp, 
                        color = TextPrimary, 
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatted, 
                        color = TextSecondary, 
                        fontSize = 12.sp
                    )
                }
            }
            
            Text(
                text = if (call.callStatus == "COMPLETED") CallManager.formatDuration(call.durationSeconds) else statusInfo.text,
                color = if (call.callStatus == "COMPLETED") TextSecondary else statusInfo.color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CallDetailsSheetContent(call: CallHistoryNode, onDelete: () -> Unit) {
    val fullDateFormat = SimpleDateFormat("MMMM dd, yyyy - hh:mm:ss a", Locale.getDefault()).format(Date(call.timestamp))
    val statusInfo = getStatusInfo(call)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Call Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLevel2, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow("Partner", call.partnerName)
            HorizontalDivider(color = DividerColor)
            DetailRow("Date & Time", fullDateFormat)
            HorizontalDivider(color = DividerColor)
            DetailRow("Status", statusInfo.text, valueColor = statusInfo.color)
            if (call.callStatus == "COMPLETED") {
                HorizontalDivider(color = DividerColor)
                DetailRow("Duration", CallManager.formatDuration(call.durationSeconds))
            }
            if (call.domain.isNotEmpty()) {
                HorizontalDivider(color = DividerColor)
                DetailRow("Domain", call.domain)
            }
            if (call.peerJsId.isNotEmpty()) {
                HorizontalDivider(color = DividerColor)
                DetailRow("Peer ID", call.peerJsId)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Delete action button
        Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorRed.copy(alpha = 0.15f),
                contentColor = ErrorRed
            ),
            elevation = null
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Call Log", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatCallTime(timestamp: Long): String {
    val date = Date(timestamp)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    return when {
        DateUtils.isToday(timestamp) -> "Today, $timeFormat"
        DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> "Yesterday, $timeFormat"
        else -> SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
    }
}

private data class StatusInfo(val text: String, val color: Color, val icon: ImageVector)

private fun getStatusInfo(call: CallHistoryNode): StatusInfo {
    return when (call.callStatus) {
        "COMPLETED" -> StatusInfo("Connected", PrimaryLight, Icons.Filled.CallMade)
        "CANCELLED" -> StatusInfo("Cancelled", WarningAmber, Icons.Filled.CallMissed)
        "FAILED_NO_ANSWER" -> StatusInfo("No Answer", WarningAmber, Icons.Filled.CallMissed)
        "FAILED_NETWORK" -> StatusInfo("Network Error", ErrorRed, Icons.Filled.ErrorOutline)
        "FAILED_HARDWARE" -> StatusInfo("Hardware Error", ErrorRed, Icons.Filled.ErrorOutline)
        "FAILED_CONFIG" -> StatusInfo("Server Error", ErrorRed, Icons.Filled.ErrorOutline)
        else -> if (call.isMissed) {
            StatusInfo("Unanswered", WarningAmber, Icons.Filled.CallMissed)
        } else {
            StatusInfo("Connected", PrimaryLight, Icons.Filled.CallMade)
        }
    }
}
