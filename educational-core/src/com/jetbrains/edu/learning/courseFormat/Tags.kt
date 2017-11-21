package com.jetbrains.edu.learning.courseFormat

import com.intellij.lang.Language
import com.intellij.ui.ColorUtil
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.languageColors.ProgrammingLanguageColorManager
import java.awt.*
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import java.util.*
import javax.swing.JComponent

private val DEFAULT_COLOR: Color = Color(70, 130, 180, 70)

open class Tag @JvmOverloads constructor(val text: String, val color: Color = DEFAULT_COLOR, private val searchOption: String = "tag") {
  fun getSearchText() : String = "$searchOption:$text".toLowerCase()

  fun accept(filter: String): Boolean {
    val textInLowerCase = text.toLowerCase(Locale.getDefault())
    if (textInLowerCase.contains(filter)) {
      return true
    }
    val searchPrefix = "$searchOption:"
    return filter.startsWith(searchPrefix) && textInLowerCase.contains(filter.substring(searchPrefix.length))
  }

    fun createComponent(isSelected: Boolean = false): JComponent {
        val label = JBLabel(text)
        label.border = RoundedBorderWithPadding(JBUI.scale(20), isSelected)
        return label
    }

    inner class RoundedBorderWithPadding(private val arcSize: Int, private val fillInside: Boolean) : RoundedLineBorder(color, arcSize, JBUI.scale(1)) {
        private val TEXT_PADDING: Int = 6

        override fun getBorderInsets(c: Component?, insets: Insets?) : Insets = JBUI.insets(TEXT_PADDING)

        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            g.color = UIUtil.getPanelBackground()
            if (fillInside) {
                g.color = ColorUtil.withAlpha(getLineColor(), 0.3)
                g.fillRoundRect(x, y, width - 1, height - 1, arcSize, arcSize)
            } else {
                val area = Area(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), (width - 1).toDouble(), (height - 1).toDouble(), arcSize.toDouble(), arcSize.toDouble()))
                area.subtract(Area(Rectangle(x + TEXT_PADDING, y + TEXT_PADDING, width - 2 * TEXT_PADDING, height - 2 * TEXT_PADDING)))
                (g as Graphics2D).fill(area)
            }
            super.paintBorder(c, g, x, y, width, height)
        }
    }
}

class ProgrammingLanguageTag(language: Language) :
        Tag(language.displayName, ProgrammingLanguageColorManager[language] ?: DEFAULT_COLOR, "programming_language")

class HumanLanguageTag(languageName: String): Tag(languageName, searchOption = "language")