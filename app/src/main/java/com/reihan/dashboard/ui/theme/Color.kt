package com.reihan.dashboard.ui.theme

import androidx.compose.ui.graphics.Color

// --- LIGHT NEOBRUTALISM PALETTE ---
val NeoBg = Color(0xFFF4F4EE)              // Bright Off-White / Light Gray
val NeoCardSurface = Color(0xFFFFFFFF)     // Pure White Card Surface
val NeoCardSurfaceElevated = Color(0xFFF9F9FB) // Elevated Card Base
val NeoBorder = Color(0xFF000000)          // Thick Black Border
val NeoBorderLight = Color(0xFFE2E2EC)     // Inner stroke / divider

// Vibrant High-Contrast Accent Colors
val NeoYellow = Color(0xFFFFDE59)          // Canary Electric Yellow
val NeoCyan = Color(0xFF00F0FF)            // Electric Neon Cyan
val NeoPink = Color(0xFFFF5390)            // Hot Pink / Magenta
val NeoPurple = Color(0xFFA855F7)          // Electric Purple
val NeoGreen = Color(0xFF22C55E)           // Neon Emerald Green
val NeoOrange = Color(0xFFFF7A00)          // Punchy Vibrant Orange
val NeoRed = Color(0xFFFF334B)             // Chili Crimson Red
val NeoBlue = Color(0xFF2563EB)            // Bright Cobalt Blue

// Text Colors
val NeoTextPrimary = Color(0xFF121212)    // Stark Black Text
val NeoTextSecondary = Color(0xFF555566)  // Muted Slate Gray Text

// --- LEGACY COLOR ALIASES FOR BACKWARD COMPATIBILITY ---
val DarkBg = NeoBg
val DarkSurface = NeoCardSurface
val DarkSurfaceGlass = NeoCardSurfaceElevated
val DarkSurface2 = NeoCardSurfaceElevated
val AccentBlue = NeoYellow                 // Main Accent
val AccentPurple = NeoPink
val AccentTask = NeoCyan
val AccentJournal = NeoPurple
val AccentSchedule = NeoGreen

val IncomeGreen = NeoGreen
val IncomeGreenGrad = NeoGreen
val ExpenseRed = NeoRed
val ExpenseRedGrad = NeoRed

val QuadrantQ1 = NeoRed
val QuadrantQ2 = NeoBlue
val QuadrantQ3 = NeoOrange
val QuadrantQ4 = NeoTextSecondary

val TextPrimary = NeoTextPrimary
val TextSecondary = NeoTextSecondary
val BorderGlass = NeoBorderLight
