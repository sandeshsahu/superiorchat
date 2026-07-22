package com.mobile.superiorchat.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.DividerColor
import com.mobile.superiorchat.theme.ErrorRed
import com.mobile.superiorchat.theme.Primary
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.theme.TextPrimary
import com.mobile.superiorchat.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInfoSheet(
    currentName: String,
    currentDescription: String,
    currentShortDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var draftName by remember { mutableStateOf(currentName) }
    var draftDesc by remember { mutableStateOf(currentDescription) }
    var draftShortDesc by remember { mutableStateOf(currentShortDescription) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceLevel1,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp).height(4.dp)
                    .background(DividerColor, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Edit Info", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Changes saved locally — backend coming soon", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Name field
            EditSheetField(
                label = "Display Name",
                value = draftName,
                onValueChange = { if (it.length <= 64) draftName = it },
                maxLength = 64,
                singleLine = true,
                placeholder = "Bot display name"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Short Description field
            EditSheetField(
                label = "About",
                value = draftShortDesc,
                onValueChange = { if (it.length <= 120) draftShortDesc = it },
                maxLength = 120,
                singleLine = true,
                placeholder = "Shown when sharing (≤120 chars)"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Description field
            EditSheetField(
                label = "Description",
                value = draftDesc,
                onValueChange = { if (it.length <= 512) draftDesc = it },
                maxLength = 512,
                singleLine = false,
                placeholder = "Shown on bot profile page"
            )
            Spacer(modifier = Modifier.height(28.dp))

            val canSave = draftName.trim().isNotEmpty() &&
                    (draftName.trim() != currentName ||
                     draftDesc.trim() != currentDescription ||
                     draftShortDesc.trim() != currentShortDescription)

            Button(
                onClick = {
                    if (canSave) onSave(draftName.trim(), draftDesc.trim(), draftShortDesc.trim())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLight,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = SurfaceLevel2
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (canSave) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary)
            }
        }
    }
}

@Composable
private fun EditSheetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    singleLine: Boolean,
    placeholder: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                "${value.length}/$maxLength",
                color = if (value.length >= maxLength) ErrorRed else TextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            maxLines = if (singleLine) 1 else 5,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SurfaceLevel2,
                focusedContainerColor = SurfaceLevel2,
                unfocusedBorderColor = DividerColor,
                focusedBorderColor = Primary,
                unfocusedTextColor = TextPrimary,
                focusedTextColor = TextPrimary,
                cursorColor = PrimaryLight
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}
