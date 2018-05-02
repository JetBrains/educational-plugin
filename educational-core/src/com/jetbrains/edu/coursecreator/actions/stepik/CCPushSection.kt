package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.stepik.StepikNames

class CCPushSection : DumbAwareAction("Update Section on Stepik", "Update Section on Stepik", null) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val view = e.getData(LangDataKeys.IDE_VIEW)
    val project = e.getData(CommonDataKeys.PROJECT)
    if (view == null || project == null) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? RemoteCourse ?: return
    if (course.courseMode != CCUtils.COURSE_MODE) return
    val directories = view.directories
    if (directories.isEmpty() || directories.size > 1) {
      return
    }

    val sectionDir = directories[0]
    if (sectionDir == null) {
      return
    }
    val section = course.getSection(sectionDir.name)
    if (section != null && course.id > 0) {
      e.presentation.isEnabledAndVisible = true
      if (section.id <= 0) {
        e.presentation.text = "Upload Section to Stepik"
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(LangDataKeys.IDE_VIEW)
    val project = e.getData(CommonDataKeys.PROJECT)
    if (view == null || project == null) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? RemoteCourse ?: return
    val directories = view.directories
    if (directories.isEmpty() || directories.size > 1) {
      return
    }

    val sectionDir = directories[0]
    if (sectionDir == null) {
      return
    }

    val section = course.getSection(sectionDir.name) ?: return
    doPush(project, section, course)
  }

  companion object {
    @JvmStatic
    fun doPush(project: Project,
                       section: Section,
                       course: RemoteCourse) {
      ProgressManager.getInstance().run(object : Task.Modal(project, "Uploading Section", true) {
        override fun run(indicator: ProgressIndicator) {
          indicator.text = "Uploading section to " + StepikNames.STEPIK_URL
          if (section.id > 0) {
            val updated = CCStepikConnector.updateSection(project, section)
            if (updated) {
              CCStepikConnector.showNotification(project, "Section \"${section.name}\" updated",
                                                 CCStepikConnector.seeOnStepikAction("/course/" + course.id))
            }
          }
          else {
            CCStepikConnector.postSection(project, section, indicator)
            CCStepikConnector.updateAdditionalSection(project)
            CCStepikConnector.showNotification(project, "Section \"${section.name}\" posted",
                                               CCStepikConnector.seeOnStepikAction("/course/" + course.id))
          }
        }
      })
    }

  }
}