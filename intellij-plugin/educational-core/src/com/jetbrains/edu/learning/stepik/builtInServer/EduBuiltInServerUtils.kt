package com.jetbrains.edu.learning.stepik.builtInServer

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadRemoteInfo
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlMapper
import java.io.File

object EduBuiltInServerUtils {

  fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val openProjects = ProjectManager.getInstance().openProjects
    for (project in openProjects) {
      if (project.isDefault) continue
      val course = project.course ?: continue
      if (!coursePredicate(course)) continue
      project.invokeLater { project.requestFocus() }
      return project to course
    }
    return null
  }

  private fun openProject(projectPath: String): Project? {
    var project: Project? = null
    ApplicationManager.getApplication().invokeAndWait {
      project = ProjectUtil.openProject(projectPath, null, true)
      project?.requestFocus()
    }
    return project
  }

  private fun Project.requestFocus() = ProjectUtil.focusProjectWindow(this, true)

  fun openRecentProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val recentPaths = RecentProjectsManagerBase.getInstanceEx().getRecentPaths()

    for (projectPath in recentPaths) {
      val course = getCourseFromYaml(projectPath) ?: continue
      if (coursePredicate(course)) {
        val project = openProject(projectPath) ?: continue
        val realProjectCourse = project.course ?: continue
        return project to realProjectCourse
      }
    }
    return null
  }

  private fun getCourseFromYaml(projectPath: String): Course? {
    val projectFile = File(PathUtil.toSystemDependentName(projectPath))
    val projectDir = VfsUtil.findFile(projectFile.toPath(), true) ?: return null
    val remoteInfoConfig = projectDir.findChild(REMOTE_COURSE_CONFIG) ?: return null
    val localCourseConfig = projectDir.findChild(COURSE_CONFIG) ?: return null
    return runReadAction {
      val localCourse = ProgressManager.getInstance().computeInNonCancelableSection<Course, Exception> {
        YamlDeserializer.deserializeItem(localCourseConfig.name, YamlMapper.basicMapper(), VfsUtil.loadText(localCourseConfig) ) as? Course
      } ?: return@runReadAction null
      localCourse.loadRemoteInfo(remoteInfoConfig)
      localCourse
    }
  }
}
