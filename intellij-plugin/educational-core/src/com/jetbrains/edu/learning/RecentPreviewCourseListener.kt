package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.ext.isPreview
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class RecentPreviewCourseListener : ProjectManagerListener, AppLifecycleListener {

  override fun projectClosing(project: Project) {
    if (!isUnitTestMode && project.isStudentProject()) {
      YamlFormatSynchronizer.saveAll(project)
    }

    val course = StudyTaskManager.getInstance(project).course ?: return
    if (course.isPreview) {
      removeProjectFromRecentProjects(project)
    }
  }

  override fun appWillBeClosed(isRestart: Boolean) {
    val projects = ProjectManager.getInstance().openProjects
    for (project in projects) {
      val course = StudyTaskManager.getInstance(project).course ?: continue
      if (course.isPreview) {
        // force closing project -> IDE will not try to reopen course preview in the next session
        ProjectManager.getInstance().closeAndDispose(project)
        removeProjectFromRecentProjects(project)
      }
    }
  }

  private fun removeProjectFromRecentProjects(project: Project) {
    val basePath = (RecentProjectsManager.getInstance() as? RecentProjectsManagerBase)?.getProjectPath(project)?.toString() ?: project.basePath
    if (basePath != null) {
      RecentProjectsManager.getInstance().removePath(PathUtil.toSystemIndependentName(basePath))
      RecentProjectsManager.getInstance().updateLastProjectPath()
    }
  }
}
