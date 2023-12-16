package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.BrowserUtil
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls
import javax.swing.JPanel

interface ContextHelpProvider {
  fun getLinkToHelp(): String? = "${EduNames.HELP_URL}/${getHelpRelativePath()}"

  fun getHelpRelativePath(): String = ""

  @Nls
  fun getTooltipText(): String

  fun createContextHelpComponent(): JPanel {
    val linkToHelp = getLinkToHelp()
    val questionMarkLabel = if (linkToHelp.isNullOrEmpty()) {
      ContextHelpLabel.create(getTooltipText())
    }
    else {
      ContextHelpLabel.createWithLink(null, getTooltipText(), EduCoreBundle.message("course.dialog.learn.more")) {
        BrowserUtil.browse(linkToHelp)
      }
    }
    return Wrapper(questionMarkLabel).apply { border = JBUI.Borders.empty(0, 6) }
  }
}