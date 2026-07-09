package com.initlauncher.theme

import androidx.annotation.ColorInt

data class Theme(
    val id: String,
    val displayName: String,
    @ColorInt val background: Int,
    @ColorInt val surface: Int,
    @ColorInt val surfaceDark: Int,
    @ColorInt val bevelHighlight: Int,
    @ColorInt val bevelShadow: Int,
    @ColorInt val textPrimary: Int,
    @ColorInt val textLabel: Int,
    @ColorInt val textHint: Int,
    @ColorInt val accentPrimary: Int,
    @ColorInt val accentSecondary: Int,
    @ColorInt val borderColor: Int,
    @ColorInt val ramBar: Int,
    @ColorInt val diskBar: Int,
    @ColorInt val netUpload: Int,
    @ColorInt val netDownload: Int,
    @ColorInt val netGrid: Int,
    @ColorInt val success: Int,
    @ColorInt val error: Int
) {
    /** Ordered (field name, color) pairs — drives the custom-theme editor and the IPC bundle. */
    fun fields(): List<Pair<String, Int>> = listOf(
        FIELD_BACKGROUND to background,
        FIELD_SURFACE to surface,
        FIELD_SURFACE_DARK to surfaceDark,
        FIELD_BEVEL_HIGHLIGHT to bevelHighlight,
        FIELD_BEVEL_SHADOW to bevelShadow,
        FIELD_TEXT_PRIMARY to textPrimary,
        FIELD_TEXT_LABEL to textLabel,
        FIELD_TEXT_HINT to textHint,
        FIELD_ACCENT_PRIMARY to accentPrimary,
        FIELD_ACCENT_SECONDARY to accentSecondary,
        FIELD_BORDER_COLOR to borderColor,
        FIELD_RAM_BAR to ramBar,
        FIELD_DISK_BAR to diskBar,
        FIELD_NET_UPLOAD to netUpload,
        FIELD_NET_DOWNLOAD to netDownload,
        FIELD_NET_GRID to netGrid,
        FIELD_SUCCESS to success,
        FIELD_ERROR to error
    )

    fun color(role: String): Int = when (role) {
        FIELD_BACKGROUND -> background
        FIELD_SURFACE -> surface
        FIELD_SURFACE_DARK -> surfaceDark
        FIELD_BEVEL_HIGHLIGHT -> bevelHighlight
        FIELD_BEVEL_SHADOW -> bevelShadow
        FIELD_TEXT_PRIMARY -> textPrimary
        FIELD_TEXT_LABEL -> textLabel
        FIELD_TEXT_HINT -> textHint
        FIELD_ACCENT_PRIMARY -> accentPrimary
        FIELD_ACCENT_SECONDARY -> accentSecondary
        FIELD_BORDER_COLOR -> borderColor
        FIELD_RAM_BAR -> ramBar
        FIELD_DISK_BAR -> diskBar
        FIELD_NET_UPLOAD -> netUpload
        FIELD_NET_DOWNLOAD -> netDownload
        FIELD_NET_GRID -> netGrid
        FIELD_SUCCESS -> success
        FIELD_ERROR -> error
        else -> throw IllegalArgumentException("Unknown theme role: $role")
    }

    fun withColor(role: String, @ColorInt value: Int): Theme = when (role) {
        FIELD_BACKGROUND -> copy(background = value)
        FIELD_SURFACE -> copy(surface = value)
        FIELD_SURFACE_DARK -> copy(surfaceDark = value)
        FIELD_BEVEL_HIGHLIGHT -> copy(bevelHighlight = value)
        FIELD_BEVEL_SHADOW -> copy(bevelShadow = value)
        FIELD_TEXT_PRIMARY -> copy(textPrimary = value)
        FIELD_TEXT_LABEL -> copy(textLabel = value)
        FIELD_TEXT_HINT -> copy(textHint = value)
        FIELD_ACCENT_PRIMARY -> copy(accentPrimary = value)
        FIELD_ACCENT_SECONDARY -> copy(accentSecondary = value)
        FIELD_BORDER_COLOR -> copy(borderColor = value)
        FIELD_RAM_BAR -> copy(ramBar = value)
        FIELD_DISK_BAR -> copy(diskBar = value)
        FIELD_NET_UPLOAD -> copy(netUpload = value)
        FIELD_NET_DOWNLOAD -> copy(netDownload = value)
        FIELD_NET_GRID -> copy(netGrid = value)
        FIELD_SUCCESS -> copy(success = value)
        FIELD_ERROR -> copy(error = value)
        else -> throw IllegalArgumentException("Unknown theme role: $role")
    }

    companion object {
        const val FIELD_BACKGROUND = "background"
        const val FIELD_SURFACE = "surface"
        const val FIELD_SURFACE_DARK = "surfaceDark"
        const val FIELD_BEVEL_HIGHLIGHT = "bevelHighlight"
        const val FIELD_BEVEL_SHADOW = "bevelShadow"
        const val FIELD_TEXT_PRIMARY = "textPrimary"
        const val FIELD_TEXT_LABEL = "textLabel"
        const val FIELD_TEXT_HINT = "textHint"
        const val FIELD_ACCENT_PRIMARY = "accentPrimary"
        const val FIELD_ACCENT_SECONDARY = "accentSecondary"
        const val FIELD_BORDER_COLOR = "borderColor"
        const val FIELD_RAM_BAR = "ramBar"
        const val FIELD_DISK_BAR = "diskBar"
        const val FIELD_NET_UPLOAD = "netUpload"
        const val FIELD_NET_DOWNLOAD = "netDownload"
        const val FIELD_NET_GRID = "netGrid"
        const val FIELD_SUCCESS = "success"
        const val FIELD_ERROR = "error"

        val ALL_FIELDS = listOf(
            FIELD_BACKGROUND, FIELD_SURFACE, FIELD_SURFACE_DARK, FIELD_BEVEL_HIGHLIGHT,
            FIELD_BEVEL_SHADOW, FIELD_TEXT_PRIMARY, FIELD_TEXT_LABEL, FIELD_TEXT_HINT,
            FIELD_ACCENT_PRIMARY, FIELD_ACCENT_SECONDARY, FIELD_BORDER_COLOR, FIELD_RAM_BAR,
            FIELD_DISK_BAR, FIELD_NET_UPLOAD, FIELD_NET_DOWNLOAD, FIELD_NET_GRID,
            FIELD_SUCCESS, FIELD_ERROR
        )
    }
}
