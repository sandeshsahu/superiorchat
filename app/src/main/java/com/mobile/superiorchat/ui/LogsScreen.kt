package com.mobile.superiorchat.ui

import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.bounceClick
import com.mobile.superiorchat.ui.components.glow
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogEntry
import com.mobile.superiorchat.utils.LogLevel
import com.mobile.superiorchat.utils.AppLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════
//  Logs Screen — Phase 4 Polish (Filtering, Typography, Animations)
// ══════════════════════════════════════════════════════════

private data class TabConfig(
    val label: String,
    val icon: ImageVector,
    val category: LogCategory?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val tabs = listOf(
        TabConfig("All", Icons.Outlined.Menu, null),
        TabConfig("System", Icons.Outlined.Settings, LogCategory.SYSTEM),
        TabConfig("Bot", Icons.Outlined.SmartToy, LogCategory.BOT_ACTIVITY),
        TabConfig("Network", Icons.Outlined.Cloud, LogCategory.NETWORK),
        TabConfig("Errors", Icons.Outlined.ErrorOutline, LogCategory.ERROR)
    )
    var selectedIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(pagerState.currentPage) {
        selectedIndex = pagerState.currentPage
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Sub-Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SurfaceLevel2, RoundedCornerShape(12.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Live Logs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryLight)
                Spacer(modifier = Modifier.width(8.dp))
                var showLogsInfo by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.padding(4.dp).size(20.dp).clickable { showLogsInfo = true }
                )
                
                if (showLogsInfo) {
                    com.mobile.superiorchat.ui.components.popups.InfoDialog(
                        title = "Live Logs",
                        message = "These logs record system background activity, network requests, and bot interactions for troubleshooting.\n\nOnly *Last 150 Logs* will be displayed.",
                        onDismiss = { showLogsInfo = false }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .bounceClick(scaleDown = 0.95f) {
                        val currentCat = tabs[selectedIndex].category
                        if (currentCat != null) {
                            AppLog.clearLogs(currentCat)
                        } else {
                            AppLog.clearAllLogs()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = ErrorRed)
            }
        }
        
        // Search Bar (Key-based Filtering)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter logs...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceLevel2,
                unfocusedContainerColor = SurfaceLevel1,
                focusedBorderColor = Primary,
                unfocusedBorderColor = DividerColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        // Filter Tags
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val bgColor = if (isSelected) PrimaryLight else SurfaceLevel2
                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                val animatedBgColor by animateColorAsState(targetValue = bgColor, label = "bg_color")
                val animatedTextColor by animateColorAsState(targetValue = textColor, label = "text_color")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(animatedBgColor, RoundedCornerShape(20.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                        .bounceClick(scaleDown = 0.95f) { 
                            selectedIndex = index
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = animatedTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tab.label, color = animatedTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Log Content Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageCategory = tabs[page].category
            val allCategoryLogs = LogCategory.entries.associateWith { AppLog.getLogs(it).collectAsState() }
            
            // Source logs for this tab
            val sourceLogs = if (pageCategory != null) {
                allCategoryLogs[pageCategory]?.value ?: emptyList()
            } else {
                AppLog.allLogs.collectAsState().value
            }
            
            // Filter by search query
            val pageLogs = if (searchQuery.isNotBlank()) {
                sourceLogs.filter { it.message.contains(searchQuery, ignoreCase = true) }
            } else {
                sourceLogs
            }

            if (pageLogs.isEmpty()) {
                // Empty State
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No matching logs" else "No logs yet", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Try a different search term." else "System activity will appear here.", 
                            color = TextSecondary, 
                            fontSize = 12.sp, 
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Log List
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pageLogs, key = { it.timestamp.toString() + it.message.hashCode() }) { entry ->
                        Box(modifier = Modifier.animateItem()) {
                            Column {
                                LogEntryRow(entry)
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 24.dp, top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val sdf = remember { SimpleDateFormat("hh:mm:ss.SSS a", Locale.getDefault()) }
    val timeStr = sdf.format(Date(entry.timestamp))

    val levelColor = when (entry.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.WARN -> WarningAmber
        LogLevel.DEBUG -> InfoBlue
        LogLevel.INFO -> Success
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Level dot
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .glow(color = levelColor, radius = 12f, dx = 0f, dy = 0f)
                .background(levelColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                timeStr,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}
