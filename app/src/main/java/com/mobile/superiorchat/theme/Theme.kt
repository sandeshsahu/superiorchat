package com.mobile.superiorchat.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// ═══════════════════════════════════════════════════════════
//  COLOR TOKENS (From Stitch Design)
// ═══════════════════════════════════════════════════════════

// ── Primary & Secondary ─────────────────────────────────
var Secondary by mutableStateOf(Color(0xFFA855F7)) // Rich Purple
var PrimaryLight by mutableStateOf(Color(0xFFC0C1FF))
var InversePrimary by mutableStateOf(Color(0xFF494BD6))
var OnPrimaryContainerDark by mutableStateOf(Color(0xFF1000A9))

// ── Surfaces & Backgrounds ──────────────────────────────
val Background = Color(0xFF000000)
val SurfaceLevel1 = Color(0xFF121212)
val SurfaceLevel2 = Color(0xFF1E1E1E)
val SurfaceContainerHighest = Color(0xFF353535)

// ── Status Colors ───────────────────────────────────────
val Success = Color(0xFF22C55E)
val ErrorRed = Color(0xFFFFB4AB)
val WarningAmber = Color(0xFFFFB783)
val InfoBlue = Color(0xFF3B82F6)

// ── Text ────────────────────────────────────────────────
val TextPrimary = Color(0xFFFFFFFF).copy(alpha = 0.95f)
val TextSecondary = Color(0xFFFFFFFF).copy(alpha = 0.60f)

// ── Misc ────────────────────────────────────────────────
val DividerColor = Color(0xFFFFFFFF).copy(alpha = 0.08f)

// ── Call Specific Tokens ────────────────────────────────
val CallBackground = Color(0xFF0F172A)
val CallSurface = Color(0xFF1E293B)
val CallTextPrimary = Color.White
val CallTextSecondary = Color(0xFF94A3B8)
val CallDanger = Color(0xFFEF4444)
val CallSuccess = Color(0xFF10B981)
val CallAccent = Color(0xFF6366F1)
val CallGlass = Color(0x0DFFFFFF) // 5% white

// ── File Type Colors ────────────────────────────────────
val FilePdf = Color(0xFFFF8B8B)
val FileApk = Color(0xFF8BFFB5)
val FileArchive = Color(0xFFFFC08B)
val FileDoc = Color(0xFF8BBAFF)
val FileSheet = Color(0xFF8BFF9B)
val FilePresentation = Color(0xFFFF9B8B)
val FileAudio = Color(0xFFD68BFF)
val FileVideo = Color(0xFFFF8B8B)
val FileImage = Color(0xFFFFDB8B)
val FileCode = Color(0xFF8BFFF0)

// ── Scanner Colors ──────────────────────────────────────
val ScannerError = Color(0xFFCF6679)
val ScannerErrorBg = Color(0xFFB00020)

// ── Pill Colors ─────────────────────────────────────────
val PillTextBlue = Color(0xFF1000A9)
val PillBgDark = Color(0xFF1E1E24)
val PillBgError = Color(0xFF690005)
val PillCallActive = Color(0xFF34D399)
val PillCallWarning = Color(0xFFFBBF24)
val PillAppleRed = Color(0xFFFF3B30)
val PillMedia = Color(0xFF4CAF50)
val PillDoc = Color(0xFF2196F3)
val PillAudio = Color(0xFFFF9800)

// ── Popup Colors ────────────────────────────────────────
val PopupBorder = Color(0xFF333333)
val PopupIcon = Color(0xFF8E8E93)
val PopupDark = Color(0xFF1A1A1A)
val PopupNavBg = Color(0xFF1E1E2E)
val PopupNavBorder = Color(0xFF3A3A4E)
val AttachMenuIcon = Color(0xFF9E9E9E)

// ── Input & Bubble Colors ───────────────────────────────
val InputRecordingRed = Color(0xCCEF4444)
val InputCancelRed = Color(0xFFFF6B6B)
val InputCancelRedTranslucent = Color(0xCCFF6B6B)
val InputPrimaryTranslucent = Color(0x99C0C1FF)
val AudioBubbleButtonBg = Color(0xFF353535)
val BubbleUnselectedBorder = Color(0xFF8E8E93)

// ── Call Screen Colors ──────────────────────────────────
val CallEndingBg = Color(0xFF080C14)
val CallGradientEnd = Color(0xFF7C3AED)

// ═══════════════════════════════════════════════════════════
//  TYPOGRAPHY (Inter-based)
// ═══════════════════════════════════════════════════════════

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.64).sp, // -0.02em
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.24).sp, // -0.01em
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp, // 0.05em
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    )
)

// ═══════════════════════════════════════════════════════════
//  THEME
// ═══════════════════════════════════════════════════════════

enum class AppTheme(val primaryLightColor: Color) { 
    LAVENDER(Color(0xFFC0C1FF)), 
    SAGE(Color(0xFFA5C7B2)), 
    AMBER(Color(0xFFD89F6B)), 
    ROSE(Color(0xFFE6A5C0)) 
}

fun applyTheme(theme: AppTheme) {
    when (theme) {
        AppTheme.LAVENDER -> {
            PrimaryLight = theme.primaryLightColor
            InversePrimary = Color(0xFF494BD6)
            Secondary = Color(0xFFA855F7)
            OnPrimaryContainerDark = Color(0xFF1000A9)
        }
        AppTheme.SAGE -> {
            PrimaryLight = theme.primaryLightColor
            InversePrimary = Color(0xFF4E7A5D)
            Secondary = Color(0xFF7CB895)
            OnPrimaryContainerDark = Color(0xFF0C2413)
        }
        AppTheme.AMBER -> {
            PrimaryLight = theme.primaryLightColor
            InversePrimary = Color(0xFF8A5D3B)
            Secondary = Color(0xFFD69A73)
            OnPrimaryContainerDark = Color(0xFF33190A)
        }
        AppTheme.ROSE -> {
            PrimaryLight = theme.primaryLightColor
            InversePrimary = Color(0xFF9E4B6E)
            Secondary = Color(0xFFD9739D)
            OnPrimaryContainerDark = Color(0xFF3B0B1D)
        }
    }
}

private val SuperiorDarkScheme 
    @Composable get() = darkColorScheme(
        primary = PrimaryLight,
        onPrimary = Color.White,
        primaryContainer = PrimaryLight,
        onPrimaryContainer = OnPrimaryContainerDark,
        inversePrimary = InversePrimary,
        secondary = Secondary,
        onSecondary = Color.White,
        tertiary = WarningAmber,
        onTertiary = Background,
        background = Background,
        onBackground = TextPrimary,
        surface = SurfaceLevel1,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceLevel2,
        onSurfaceVariant = TextSecondary,
        outline = DividerColor,
        outlineVariant = DividerColor,
        error = ErrorRed,
        onError = Color(0xFF690005),
    )

@Composable
fun SuperiorChatTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SuperiorDarkScheme,
        typography = Typography,
        content = content
    )
}
