package com.initlauncher.theme

import android.content.Context
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Applies a Theme to a live view tree by reading the `android:tag` role spec each themed
 * view declares in XML (e.g. "text=accentPrimary;bg=borderBox"). The view's original XML
 * `textColor`/`background` attributes are left in place as a static "Current"-theme fallback
 * for first paint; this overwrites them at runtime.
 */
object ThemeApplier {

    fun apply(root: View, theme: Theme) {
        applyToView(root, theme)
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                apply(root.getChildAt(i), theme)
            }
        }
    }

    private fun applyToView(view: View, theme: Theme) {
        val tag = view.tag as? String ?: return
        for (part in tag.split(";")) {
            val eq = part.indexOf('=')
            if (eq < 0) continue
            val key = part.substring(0, eq)
            val value = part.substring(eq + 1)
            when (key) {
                "text" -> (view as? TextView)?.setTextColor(theme.color(value))
                "hint" -> (view as? TextView)?.setHintTextColor(theme.color(value))
                "bg" -> {
                    val drawable = buildBackground(value, theme, view.context)
                    if (view is ProgressBar && value.startsWith("progress:")) {
                        view.progressDrawable = drawable
                    } else {
                        view.background = drawable
                    }
                }
            }
        }
    }

    private fun buildBackground(spec: String, theme: Theme, context: Context): Drawable {
        return when {
            spec == "borderBox" -> borderBoxDrawable(theme, context)
            spec == "appItem" -> appItemDrawable(theme, context)
            spec == "pinDotEmpty" -> pinDotDrawable(theme, filled = false, context)
            spec == "pinDotFilled" -> pinDotDrawable(theme, filled = true, context)
            spec.startsWith("plain:") -> ColorDrawable(theme.color(spec.removePrefix("plain:")))
            spec.startsWith("progress:") -> progressDrawable(theme, spec.removePrefix("progress:"), context)
            else -> ColorDrawable(theme.background)
        }
    }

    private fun px(dp: Int, context: Context): Int =
        (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    private fun solid(@androidx.annotation.ColorInt color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
    }

    /** Raised bevel border, matches drawable/border_box.xml. */
    fun borderBoxDrawable(theme: Theme, context: Context): Drawable {
        val inset = px(2, context)
        val shadow = solid(theme.bevelShadow)
        val highlight = solid(theme.bevelHighlight)
        val fill = solid(theme.surface)
        val layer = LayerDrawable(arrayOf(shadow, highlight, fill))
        layer.setLayerInset(0, 0, 0, 0, 0)
        layer.setLayerInset(1, 0, 0, inset, inset)
        layer.setLayerInset(2, inset, inset, inset, inset)
        return layer
    }

    /** Recessed bevel border, matches drawable/app_item_background.xml. */
    fun appItemDrawable(theme: Theme, context: Context): Drawable {
        val inset = px(2, context)
        val highlight = solid(theme.bevelHighlight)
        val shadow = solid(theme.bevelShadow)
        val fill = solid(theme.surfaceDark)
        val layer = LayerDrawable(arrayOf(highlight, shadow, fill))
        layer.setLayerInset(0, 0, 0, 0, 0)
        layer.setLayerInset(1, inset, inset, 0, 0)
        layer.setLayerInset(2, inset, inset, inset, inset)
        return layer
    }

    /** Square progress bar, matches drawable/progress_bar_square*.xml. */
    fun progressDrawable(theme: Theme, barRole: String, context: Context): Drawable {
        val background = solid(theme.surfaceDark)
        val bar = solid(theme.color(barRole))
        val clip = ClipDrawable(bar, Gravity.START, ClipDrawable.HORIZONTAL)
        val layer = LayerDrawable(arrayOf(background, clip))
        layer.setId(0, android.R.id.background)
        layer.setId(1, android.R.id.progress)
        return layer
    }

    fun pinDotDrawable(theme: Theme, filled: Boolean, context: Context): Drawable {
        return if (filled) {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(theme.borderColor)
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(theme.surfaceDark)
                setStroke(px(2, context), theme.textLabel)
            }
        }
    }
}
