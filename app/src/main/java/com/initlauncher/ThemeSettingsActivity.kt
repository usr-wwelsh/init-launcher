package com.initlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.initlauncher.theme.DefaultThemes
import com.initlauncher.theme.Theme
import com.initlauncher.theme.ThemeApplier
import com.initlauncher.theme.ThemeManager

class ThemeSettingsActivity : Activity() {

    private lateinit var themeList: LinearLayout
    private lateinit var editCustomButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContentView(R.layout.activity_theme_settings)

        themeList = findViewById(R.id.themeList)
        editCustomButton = findViewById(R.id.editCustomButton)

        editCustomButton.setOnClickListener {
            startActivity(Intent(this, CustomThemeEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        refreshList()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private fun applyTheme() {
        ThemeApplier.apply(findViewById(android.R.id.content), ThemeManager.getActiveTheme(this))
    }

    private fun refreshList() {
        val activeTheme = ThemeManager.getActiveTheme(this)
        val activeId = ThemeManager.getActiveThemeId(this)
        themeList.removeAllViews()
        val entries = DefaultThemes.ALL + listOfNotNull(ThemeManager.getCustomTheme(this))
        entries.forEach { theme ->
            themeList.addView(buildRow(theme, theme.id == activeId, activeTheme))
        }
    }

    private fun buildRow(theme: Theme, selected: Boolean, activeTheme: Theme): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_theme_row, themeList, false)
        ThemeApplier.apply(row, activeTheme)

        row.findViewById<TextView>(R.id.themeName).text = theme.displayName
        row.findViewById<TextView>(R.id.selectedMark).visibility =
            if (selected) View.VISIBLE else View.INVISIBLE

        val swatchColors = listOf(theme.background, theme.accentPrimary, theme.textPrimary, theme.borderColor, theme.success)
        val swatchIds = listOf(R.id.swatch0, R.id.swatch1, R.id.swatch2, R.id.swatch3, R.id.swatch4)
        swatchIds.forEachIndexed { i, id ->
            row.findViewById<View>(id).background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(swatchColors[i])
            }
        }

        row.setOnClickListener {
            ThemeManager.setActiveThemeId(this, theme.id)
            applyTheme()
            refreshList()
        }
        return row
    }
}
