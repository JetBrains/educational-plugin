package com.jetbrains.edu.learning.stepik.course

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import org.apache.commons.lang3.math.NumberUtils
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

abstract class ImportCoursePanel(private val courseConnector: EduCourseConnector, helpLabelText: String) {
  protected val courseLinkTextField = JTextField()
  val panel: JPanel
  private val helpLabel = JLabel(helpLabelText)

  val courseLink: String
    get() = courseLinkTextField.text

  val preferredFocusedComponent: JComponent
    get() = courseLinkTextField

  init {
    helpLabel.foreground = UIUtil.getLabelDisabledForeground()
    helpLabel.font = UIUtil.getLabelFont()

    val courseLink = JLabel(message("label.course.link"))
    panel = JPanel(BorderLayout())
    val nestedPanel = JPanel()
    val layout = GroupLayout(nestedPanel)
    layout.autoCreateGaps = true
    nestedPanel.layout = layout
    layout.setHorizontalGroup(
      layout.createSequentialGroup()
        .addComponent(courseLink)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(courseLinkTextField)
                    .addComponent(helpLabel))
    )
    layout.setVerticalGroup(
      layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(courseLink)
                    .addComponent(courseLinkTextField))
        .addComponent(helpLabel)
    )
    panel.add(nestedPanel, BorderLayout.NORTH)
    panel.preferredSize = JBUI.size(Dimension(400, 90))
    panel.minimumSize = panel.preferredSize
  }

  fun validate(): Boolean {
    val text = courseLinkTextField.text
    return text.isNotEmpty() && (NumberUtils.isDigits(text) || isValidLink(text))
  }

  protected fun isValidLink(text: String): Boolean = courseConnector.getCourseIdFromLink(text) != -1

  abstract fun setValidationListener(validationListener: ValidationListener?)

  interface ValidationListener {
    fun onLoggedIn(isLoggedIn: Boolean)
  }
}