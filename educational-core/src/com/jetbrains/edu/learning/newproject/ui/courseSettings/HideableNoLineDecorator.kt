package com.jetbrains.edu.learning.newproject.ui.courseSettings

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

/**
 * Inspired by [com.intellij.ui.HideableDecorator]
 */
class HideableNoLineDecorator(private val myPanel: JPanel, @Nls(capitalization = Nls.Capitalization.Title) title: String) {
  var isExpanded = false
  var title: String
    get() = myTitledSeparator.text
    set(title) {
      myTitledSeparator.text = title
    }
  private val myTitledSeparator: NoLineTitledSeparator
  private var content: JComponent? = null
  private var myPreviousContentSize: Dimension? = null

  init {
    myTitledSeparator = NoLineTitledSeparator(title)
    val separatorPanel = JPanel(BorderLayout())
    separatorPanel.add(myTitledSeparator, BorderLayout.CENTER)
    myPanel.add(separatorPanel, BorderLayout.NORTH)
    myTitledSeparator.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    updateIcon()
    myTitledSeparator.addMouseListener(ExpandMouseAdapter())
  }

  fun setContentComponent(content: JComponent?) {
    if (content == null && this.content != null) {
      myPanel.remove(this.content)
    }
    this.content = content
    this.content?.let {
      it.isVisible = isExpanded
      myPanel.add(it, BorderLayout.CENTER)
    }
  }

  fun setOn(on: Boolean) {
    if (on) on() else off()
  }

  private fun on() {
    isExpanded = true
    updateIcon()
    myTitledSeparator.label.iconTextGap = 5
    content?.isVisible = true
    myPanel.invalidate()
    myPanel.repaint()
  }

  private fun off() {
    isExpanded = false
    updateIcon()
    content?.let {
      it.isVisible = false
      myPreviousContentSize = it.size
    }
    myPanel.invalidate()
    myPanel.repaint()
  }

  private fun updateIcon() {
    val icon = if (isExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
    myTitledSeparator.label.icon = icon
    myTitledSeparator.label.disabledIcon = IconLoader.getTransparentIcon(icon, 0.5f)
  }

  private fun registerMnemonic() {
    val mnemonicIndex = UIUtil.getDisplayMnemonicIndex(title)
    if (mnemonicIndex == -1) {
      return
    }

    myPanel.actionMap.put(ACTION_KEY, ExpandAction())
    val mnemonic = UIUtil.removeMnemonic(title).toUpperCase(Locale.getDefault())[mnemonicIndex]
    val inputMap = myPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val keyStroke = KeyStroke.getKeyStroke(mnemonic.toInt(), InputEvent.ALT_DOWN_MASK, false)
    inputMap.put(keyStroke, ACTION_KEY)
  }

  private inner class ExpandAction : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) = setOn(!isExpanded)
  }

  private inner class ExpandMouseAdapter : MouseAdapter() {
    override fun mouseReleased(e: MouseEvent) = setOn(!isExpanded)
  }

  /**
   * Inspired by [com.intellij.ui.TitledSeparator]
   */
  private inner class NoLineTitledSeparator(@Nls(capitalization = Nls.Capitalization.Title) text: String = "") : JPanel() {
    val label = JBLabel()
    var text: String
      get() = originalText
      set(text) {
        originalText = text
        label.text = if (text.startsWith("<html>")) text else UIUtil.replaceMnemonicAmpersand(originalText)
      }
    private var originalText: String = ""

    init {
      layout = BorderLayout()
      border = JBUI.Borders.empty(TOP_INSET, 0, BOTTOM_INSET, 0)
      isOpaque = false
      this.text = text
      this.add(label, BorderLayout.LINE_START)
    }

    override fun setEnabled(enabled: Boolean) {
      super.setEnabled(enabled)
      label.isEnabled = enabled
    }

    override fun addNotify() {
      super.addNotify()
      registerMnemonic()
    }
  }

  companion object {
    private const val ACTION_KEY = "Collapse/Expand on mnemonic"
    private const val TOP_INSET = 7
    private const val BOTTOM_INSET = 5
  }
}
