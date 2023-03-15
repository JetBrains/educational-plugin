package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.GrayTextHtmlPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.*

class MarketplaceCoursePanel(disposable: Disposable): CoursePanel(disposable, true) {

  init {
    courseDetailsPanel.border = JBUI.Borders.empty(10, HORIZONTAL_MARGIN, 0, 0)
  }

  override fun addComponents() {
    with(content) {
      add(tagsPanel)
      add(titlePanel)
      add(authorsPanel)
      add(errorComponent)
      add(buttonsPanel)
      add(LegalTermsPanel())
      add(courseDetailsPanel)
      add(settingsPanel)
    }
  }

  override fun joinCourseAction(info: CourseInfo, mode: CourseMode) {
    MarketplacePlatformProvider().joinAction(info, mode, this)
  }

  private class LegalTermsPanel: NonOpaquePanel(), CourseSelectionListener {

    private val textPanel = GrayTextHtmlPanel("text")

    init {
      add(textPanel)
      border = JBUI.Borders.empty(2, 16, 0, 0)
    }

    override fun onCourseSelectionChanged(data: CourseBindData) {
      val course = data.course
      val authors = course.authorFullNames.joinToString()
      val license = course.license

      if (authors.isEmpty() || license.isNullOrEmpty()) {
        isVisible = false
        return
      }
      isVisible = true
      val text = EduCoreBundle.message("marketplace.course.selection.legal.note", authors, license, PLUGIN_MARKETPLACE_AGREEMENT)
      textPanel.setBody(text)
    }

    companion object {
      private const val PLUGIN_MARKETPLACE_AGREEMENT = "https://plugins.jetbrains.com/legal/terms-of-use"
    }
  }
}
