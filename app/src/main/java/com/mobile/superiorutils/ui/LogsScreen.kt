package com.mobile.superiorutils.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorutils.theme.*
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.LogEntry
import com.mobile.superiorutils.utils.LogLevel
import com.mobile.superiorutils.utils.AppLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════
//  Logs Screen — Hybrid Original UI with New Categories
// ══════════════════════════════════════════════════════════

private data class TabConfig(
    val label: String,
    val icon: ImageVector,
    val category: LogCategory?
)

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
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Sync pager state with selectedIndex
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
                Text("Live Logs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E2E2))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFC7C4D7), modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable {
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
                val textColor = if (isSelected) Color(0xFF1000A9) else Color(0xFFC7C4D7)

                val animatedBgColor by animateColorAsState(targetValue = bgColor, label = "bg_color")
                val animatedTextColor by animateColorAsState(targetValue = textColor, label = "text_color")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(animatedBgColor, RoundedCornerShape(20.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                        .clickable { 
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
            val pageLogs = if (pageCategory != null) {
                allCategoryLogs[pageCategory]?.value ?: emptyList()
            } else {
                AppLog.allLogs.collectAsState().value
            }

            if (pageLogs.isEmpty()) {
                // Empty State
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(48.dp), tint = Color(0xFFC7C4D7).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No logs yet", color = Color(0xFFE2E2E2), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("System activity will appear here.", color = Color(0xFF908FA0), fontSize = 12.sp, textAlign = TextAlign.Center)
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
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = sdf.format(Date(entry.timestamp))

    val levelColor = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFEF4444)
        LogLevel.WARN -> Color(0xFFFF9F0A)
        LogLevel.DEBUG -> Color(0xFF60A5FA)
        LogLevel.INFO -> Color(0xFF22C55E)
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
                .background(levelColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.message,
                fontSize = 12.sp,
                color = Color(0xFFE2E2E2),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                timeStr,
                fontSize = 10.sp,
                color = Color(0xFF908FA0)
            )
        }
    }
}
