package com.initlauncher

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.initlauncher.theme.Theme
import com.initlauncher.theme.ThemeApplier
import com.initlauncher.theme.ThemeManager

class CustomThemeEditActivity : Activity() {

    private lateinit var container: LinearLayout
    private lateinit var editingTheme: Theme

    private val sections = listOf(
        "BASE" to listOf(
            Theme.FIELD_BACKGROUND, Theme.FIELD_SURFACE, Theme.FIELD_SURFACE_DARK,
            Theme.FIELD_BEVEL_HIGHLIGHT, Theme.FIELD_BEVEL_SHADOW
        ),
        "TEXT" to listOf(Theme.FIELD_TEXT_PRIMARY, Theme.FIELD_TEXT_LABEL, Theme.FIELD_TEXT_HINT),
        "ACCENTS" to listOf(Theme.FIELD_ACCENT_PRIMARY, Theme.FIELD_ACCENT_SECONDARY, Theme.FIELD_BORDER_COLOR),
        "STATUS" to listOf(Theme.FIELD_SUCCESS, Theme.FIELD_ERROR),
        "GRAPHS" to listOf(
            Theme.FIELD_RAM_BAR, Theme.FIELD_DISK_BAR, Theme.FIELD_NET_UPLOAD,
            Theme.FIELD_NET_DOWNLOAD, Theme.FIELD_NET_GRID
        )
    )

    private val fieldLabels = mapOf(
        Theme.FIELD_BACKGROUND to "Background",
        Theme.FIELD_SURFACE to "Surface",
        Theme.FIELD_SURFACE_DARK to "Surface (recessed)",
        Theme.FIELD_BEVEL_HIGHLIGHT to "Bevel highlight",
        Theme.FIELD_BEVEL_SHADOW to "Bevel shadow",
        Theme.FIELD_TEXT_PRIMARY to "Text primary",
        Theme.FIELD_TEXT_LABEL to "Text label",
        Theme.FIELD_TEXT_HINT to "Text hint",
        Theme.FIELD_ACCENT_PRIMARY to "Accent primary",
        Theme.FIELD_ACCENT_SECONDARY to "Accent secondary",
        Theme.FIELD_BORDER_COLOR to "Border",
        Theme.FIELD_RAM_BAR to "RAM graph",
        Theme.FIELD_DISK_BAR to "Disk graph",
        Theme.FIELD_NET_UPLOAD to "Net upload",
        Theme.FIELD_NET_DOWNLOAD to "Net download",
        Theme.FIELD_NET_GRID to "Net grid",
        Theme.FIELD_SUCCESS to "Success",
        Theme.FIELD_ERROR to "Error"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContentView(R.layout.activity_custom_theme_edit)

        editingTheme = (ThemeManager.getCustomTheme(this) ?: ThemeManager.getActiveTheme(this))
            .copy(id = "custom", displayName = "Custom")

        container = findViewById(R.id.colorRowContainer)
        findViewById<TextView>(R.id.saveButton).setOnClickListener {
            ThemeManager.saveCustomTheme(this, editingTheme)
            Toast.makeText(this, "Custom theme saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        applyTheme()
        buildRows()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private fun applyTheme() {
        ThemeApplier.apply(findViewById(android.R.id.content), ThemeManager.getActiveTheme(this))
    }

    private fun buildRows() {
        container.removeAllViews()
        val activeTheme = ThemeManager.getActiveTheme(this)
        val pad = dp(10)
        sections.forEach { (title, fields) ->
            container.addView(TextView(this).apply {
                text = title
                setTextColor(activeTheme.textLabel)
                textSize = 11f
                typeface = Typeface.MONOSPACE
                setPadding(0, pad, 0, dp(4))
            })
            fields.forEach { field -> container.addView(colorRow(field, activeTheme)) }
        }
    }

    private fun colorRow(field: String, activeTheme: Theme): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            isClickable = true
            isFocusable = true
        }
        val label = TextView(this).apply {
            text = fieldLabels[field] ?: field
            setTextColor(activeTheme.textPrimary)
            typeface = Typeface.MONOSPACE
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val swatch = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            background = swatchDrawable(editingTheme.color(field), activeTheme.borderColor)
        }
        row.addView(label)
        row.addView(swatch)
        row.setOnClickListener { showEditDialog(field, swatch) }
        return row
    }

    private fun swatchDrawable(color: Int, borderColor: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        setStroke(dp(1), borderColor)
    }

    private fun showEditDialog(field: String, swatch: View) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(String.format("#%06X", 0xFFFFFF and editingTheme.color(field)))
        }
        AlertDialog.Builder(this)
            .setTitle(fieldLabels[field] ?: field)
            .setMessage("Hex color, e.g. #3B3E47")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val parsed = parseHex(input.text.toString().trim())
                if (parsed != null) {
                    editingTheme = editingTheme.withColor(field, parsed)
                    swatch.background = swatchDrawable(parsed, ThemeManager.getActiveTheme(this).borderColor)
                } else {
                    Toast.makeText(this, "Invalid hex color, expected #RRGGBB", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun parseHex(text: String): Int? {
        val cleaned = if (text.startsWith("#")) text else "#$text"
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(cleaned)) return null
        return try { Color.parseColor(cleaned) } catch (e: IllegalArgumentException) { null }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
