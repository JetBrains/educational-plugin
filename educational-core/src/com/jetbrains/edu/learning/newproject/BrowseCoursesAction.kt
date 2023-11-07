package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.dialogs.BrowseCoursesDialog
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.NonNls

class BrowseCoursesAction : DumbAwareAction(EduCoreBundle.message("browse.courses"),
                                            EduCoreBundle.message("browse.courses.description"),
                                            null) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = !RemoteEnvHelper.isRemoteDevServer()
  }
  
  override fun actionPerformed(e: AnActionEvent) {
    EduCounterUsageCollector.courseSelectionViewOpened(e.place)
    BrowseCoursesDialog().show()
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.BrowseCourses"
  }
}
