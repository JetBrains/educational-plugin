package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class RecentPreviewCourseListener : ProjectManagerListener, AppLifecycleListener {

  override fun projectClosing(project: Project) {
    if (!isUnitTestMode && EduUtils.isStudentProject(project)) {
      YamlFormatSynchronizer.saveAll(project)
    }

    if (PropertiesComponent.getInstance(project).getBoolean(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW)) {
      removeProjectFromRecentProjects(project)
    }
  }

  override fun appWillBeClosed(isRestart: Boolean) {
    val projects = ProjectManager.getInstance().openProjects
    for (project in projects) {
      if (PropertiesComponent.getInstance(project).getBoolean(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW)) {
        // force closing project -> IDE will not try to reopen course preview in the next session
        ProjectManager.getInstance().closeAndDispose(project)
        removeProjectFromRecentProjects(project)
      }
    }
  }

  private fun removeProjectFromRecentProjects(project: Project) {
    val basePath = project.basePath
    if (basePath != null) {
      RecentProjectsManager.getInstance().removePath(basePath)
      RecentProjectsManager.getInstance().updateLastProjectPath()
    }
  }
}
