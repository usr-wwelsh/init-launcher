package com.initlauncher.theme

import android.graphics.Color

object DefaultThemes {

    /** Today's "flat mono" palette — the unchanged default. */
    val CURRENT = Theme(
        id = "current",
        displayName = "Current",
        background = Color.parseColor("#3B3E47"),
        surface = Color.parseColor("#3B3E47"),
        surfaceDark = Color.parseColor("#3B3E47"),
        bevelHighlight = Color.parseColor("#3B3E47"),
        bevelShadow = Color.parseColor("#3B3E47"),
        textPrimary = Color.parseColor("#B8B8B8"),
        textLabel = Color.parseColor("#B8B8B8"),
        textHint = Color.parseColor("#B8B8B8"),
        accentPrimary = Color.parseColor("#B8B8B8"),
        accentSecondary = Color.parseColor("#3B3E47"),
        borderColor = Color.parseColor("#B8B8B8"),
        ramBar = Color.parseColor("#B8B8B8"),
        diskBar = Color.parseColor("#B8B8B8"),
        netUpload = Color.parseColor("#B8B8B8"),
        netDownload = Color.parseColor("#B8B8B8"),
        netGrid = Color.parseColor("#3B3E47"),
        success = Color.parseColor("#8FBF8F"),
        error = Color.parseColor("#C05870")
    )

    val LIGHT = Theme(
        id = "light",
        displayName = "Light",
        background = Color.parseColor("#F2F0EA"),
        surface = Color.parseColor("#FFFFFF"),
        surfaceDark = Color.parseColor("#E4E1D8"),
        bevelHighlight = Color.parseColor("#FFFFFF"),
        bevelShadow = Color.parseColor("#C9C5B8"),
        textPrimary = Color.parseColor("#1C1C1C"),
        textLabel = Color.parseColor("#3A3A3A"),
        textHint = Color.parseColor("#8A8A8A"),
        accentPrimary = Color.parseColor("#2A6F97"),
        accentSecondary = Color.parseColor("#E4E1D8"),
        borderColor = Color.parseColor("#B5B0A2"),
        ramBar = Color.parseColor("#2A6F97"),
        diskBar = Color.parseColor("#9A4C95"),
        netUpload = Color.parseColor("#3F8F3F"),
        netDownload = Color.parseColor("#2A6F97"),
        netGrid = Color.parseColor("#D8D4C7"),
        success = Color.parseColor("#2E7D32"),
        error = Color.parseColor("#C62828")
    )

    val DARK = Theme(
        id = "dark",
        displayName = "Dark",
        background = Color.parseColor("#14161B"),
        surface = Color.parseColor("#1C1F26"),
        surfaceDark = Color.parseColor("#101216"),
        bevelHighlight = Color.parseColor("#2A2E36"),
        bevelShadow = Color.parseColor("#0A0B0D"),
        textPrimary = Color.parseColor("#E8E8E8"),
        textLabel = Color.parseColor("#C7C7C7"),
        textHint = Color.parseColor("#7C818A"),
        accentPrimary = Color.parseColor("#5FD3BC"),
        accentSecondary = Color.parseColor("#232730"),
        borderColor = Color.parseColor("#3A3F4A"),
        ramBar = Color.parseColor("#5FD3BC"),
        diskBar = Color.parseColor("#C792EA"),
        netUpload = Color.parseColor("#89DDFF"),
        netDownload = Color.parseColor("#5FD3BC"),
        netGrid = Color.parseColor("#2A2E36"),
        success = Color.parseColor("#7CC576"),
        error = Color.parseColor("#E06C75")
    )

    /** Canonical Solarized Dark palette. */
    val SOLARIZED = Theme(
        id = "solarized",
        displayName = "Solarized",
        background = Color.parseColor("#002B36"),
        surface = Color.parseColor("#073642"),
        surfaceDark = Color.parseColor("#00212B"),
        bevelHighlight = Color.parseColor("#586E75"),
        bevelShadow = Color.parseColor("#001A22"),
        textPrimary = Color.parseColor("#93A1A1"),
        textLabel = Color.parseColor("#839496"),
        textHint = Color.parseColor("#586E75"),
        accentPrimary = Color.parseColor("#268BD2"),
        accentSecondary = Color.parseColor("#073642"),
        borderColor = Color.parseColor("#586E75"),
        ramBar = Color.parseColor("#DC322F"),
        diskBar = Color.parseColor("#6C71C4"),
        netUpload = Color.parseColor("#859900"),
        netDownload = Color.parseColor("#2AA198"),
        netGrid = Color.parseColor("#073642"),
        success = Color.parseColor("#859900"),
        error = Color.parseColor("#DC322F")
    )

    val ALL = listOf(CURRENT, LIGHT, DARK, SOLARIZED)

    fun byId(id: String): Theme? = ALL.find { it.id == id }
}
