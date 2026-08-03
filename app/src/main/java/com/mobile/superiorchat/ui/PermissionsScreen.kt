package com.mobile.superiorchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.superiorchat.ui.components.bounceClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.*

// ══════════════════════════════════════════════════════════
//  Permission Data Model
// ══════════════════════════════════════════════════════════

data class PermissionState(
    val name: String,
    val isGranted: Boolean,
    val displayStatus: String? = null,
    val buttonText: String = "Grant",
    val onClick: () -> Unit
)

// ══════════════════════════════════════════════════════════
//  Permissions Screen
// ══════════════════════════════════════════════════════════

@Composable
fun PermissionsScreen(permissions: List<PermissionState>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "All permissions must be granted for full functionality.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
        items(permissions) { perm ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLevel2, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(if (perm.isGranted) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                perm.name,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                perm.displayStatus ?: if (perm.isGranted) "Granted" else "Required",
                                fontSize = 11.sp,
                                color = if (perm.isGranted) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!perm.isGranted) {
                        Box(
                            modifier = Modifier
                                .bounceClick(scaleDown = 0.95f) { perm.onClick() }
                                .background(PrimaryLight, RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(perm.buttonText, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(PrimaryLight.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("\u2713 Active", color = PrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
    }
}
