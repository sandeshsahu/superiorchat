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

// ═══════════════════════════════════════════════════════════
//  COLOR TOKENS (From Stitch Design)
// ═══════════════════════════════════════════════════════════

// ── Primary & Secondary ─────────────────────────────────
val Primary = Color(0xFF6366F1) // Violet-Indigo
val Secondary = Color(0xFFA855F7) // Rich Purple
val PrimaryLight = Color(0xFFC0C1FF)
val InversePrimary = Color(0xFF494BD6)

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

private val SuperiorDarkScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Color(0xFF1000A9),
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
