package com.jetbrains.edu.learning.checker.details

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import javafx.embed.swing.JFXPanel

abstract class CheckDetailsView {

  abstract fun showOutput(message: String)

  abstract fun showCompilationResults(message: String)

  abstract fun showFailedToCheckMessage(message: String)

  abstract fun showJavaFXResult(title: String, panel: JFXPanel)

  abstract fun clear()

  companion object {
    @JvmStatic
    fun getInstance(project: Project): CheckDetailsView {
      if (!EduUtils.isEduProject(project)) {
        error("Attempt to get CheckDetailsViewImpl for non-edu project")
      }
      return ServiceManager.getService(project, CheckDetailsView::class.java)
    }
  }
}