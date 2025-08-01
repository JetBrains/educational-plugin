package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.services.dialog.showDialogAndGetHost
import org.jetbrains.annotations.NonNls

class CourseStorageChangeHost : DumbAwareAction(EduCoreBundle.message("course.storage.change.host")) {
  override fun actionPerformed(e: AnActionEvent) {
    val selectedUrl = CourseStorageChangeHostDialog().showDialogAndGetHost()
    if (selectedUrl == null) {
      LOG.warn("Selected Course storage service URL item is null")
      return
    }

    val existingValue = CourseStorageServiceHost.getSelectedUrl()
    if (selectedUrl == existingValue) return

    CourseStorageServiceHost.setUrl(selectedUrl, existingValue)
    LOG.info("Course storage service URL was changed to $selectedUrl")
  }

  companion object {
    private val LOG: Logger = logger<CourseStorageChangeHost>()

    @NonNls
    const val ACTION_ID = "Educational.Student.CourseStorageChangeHost"
  }
}