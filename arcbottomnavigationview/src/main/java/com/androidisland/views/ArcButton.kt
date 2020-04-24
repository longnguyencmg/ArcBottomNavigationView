package com.androidisland.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton

/**
 * Created by Farshad Tahmasbi on June 22,2019.
 * Copyright(c) 2019, All rights reserved.
 * https://github.com/FarshadTahmasbi/ArcBottomNavigationView
 * Email: farshad.tmb@gmail.com
 */
class ArcButton(context: Context?) : MaterialButton(
    ContextThemeWrapper(context, R.style.ArcTheme),
    null,
    R.attr.materialButtonStyle
) {
    private var iconSize = 0
    private var icon: Drawable? = null
    private var iconTintMode: PorterDuff.Mode? = PorterDuff.Mode.SRC_IN
    private var iconTint: ColorStateList? = null
    private var iconLeft = 0
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (iconSize == 0) {
            setIconSize(w / 2)
        }
    }

    override fun setIconSize(iconSize: Int) {
        require(iconSize >= 0) { "iconSize cannot be less than 0" }
        if (this.iconSize != iconSize) {
            this.iconSize = iconSize
            updateIcon()
        }
    }

    override fun getIconSize(): Int {
        return iconSize
    }

    override fun setIcon(icon: Drawable?) {
        if (this.icon !== icon) {
            this.icon = icon
            updateIcon()
        }
    }

    override fun getIcon(): Drawable {
        return icon!!
    }

    override fun setIconTint(iconTint: ColorStateList?) {
        if (this.iconTint !== iconTint) {
            this.iconTint = iconTint
            updateIcon()
        }
    }

    override fun getIconTint(): ColorStateList {
        return iconTint!!
    }

    override fun setIconTintMode(iconTintMode: PorterDuff.Mode) {
        if (this.iconTintMode != iconTintMode) {
            this.iconTintMode = iconTintMode
            updateIcon()
        }
    }

    override fun getIconTintMode(): PorterDuff.Mode? {
        return iconTintMode
    }

    private fun updateIcon() {
        if (this.icon != null) {
            this.icon = this.icon!!.mutate()
            DrawableCompat.setTintList(this.icon!!, this.iconTint)
            if (this.iconTintMode != null) {
                DrawableCompat.setTintMode(this.icon!!, this.iconTintMode!!)
            }
            val width =
                if (this.iconSize != 0) this.iconSize else this.icon!!.intrinsicWidth
            val height =
                if (this.iconSize != 0) this.iconSize else this.icon!!.intrinsicHeight
            iconLeft = (measuredWidth - width) / 2
            if (iconLeft < 0) iconLeft = 0
            this.icon!!.setBounds(iconLeft, 0, width + iconLeft, height)
        }
        TextViewCompat.setCompoundDrawablesRelative(
            this,
            this.icon,
            null as Drawable?,
            null as Drawable?,
            null as Drawable?
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        val iconWidth =
            if (this.iconSize != 0) this.iconSize else this.icon!!.intrinsicWidth
        iconLeft = (measuredWidth - iconWidth) / 2
        updateIcon()
    }
}