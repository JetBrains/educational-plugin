package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.pushAvailable
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.showErrorNotification
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector

class CCPushSection : DumbAwareAction("Update Section on Stepik", "Update Section on Stepik", null) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.getData(CommonDataKeys.PROJECT)
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    if (project == null || selectedFiles == null || selectedFiles.size != 1) {
      return
    }
    val sectionDir = selectedFiles[0]
    if (!sectionDir.isDirectory) {
      return
    }

    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (course.courseMode != CCUtils.COURSE_MODE || !course.isRemote) return
    if (course.hasTopLevelLessons) return

    val section = course.getSection(sectionDir.name)
    if (section != null && course.id > 0) {
      e.presentation.isEnabledAndVisible = true
      if (section.id <= 0) {
        e.presentation.text = "Upload Section to Stepik"
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    if (project == null || selectedFiles == null || selectedFiles.size != 1) {
      return
    }
    val sectionDir = selectedFiles[0]

    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    val section = course.getSection(sectionDir.name) ?: return
    doPush(project, section, course)
    YamlFormatSynchronizer.saveRemoteInfo(section)
  }

  companion object {
    @JvmStatic
    fun doPush(project: Project, section: Section, course: EduCourse) {
      ProgressManager.getInstance().run(object : Task.Modal(project, "Uploading Section", true) {
        override fun run(indicator: ProgressIndicator) {
          indicator.text = "Uploading section to " + StepikNames.STEPIK_URL
          if (section.id > 0) {
            updateSection(section, course, project)
          }
          else {
            if (!pushAvailable(course, section, project)) return

            section.position = section.index
            val success = CCStepikConnector.postSection(project, section)
            if (success) {
              EduUtils.showNotification(project, "Section \"${section.name}\" posted",
                                        CCStepikConnector.openOnStepikAction("/course/${course.id}"))
            }
          }
        }
      })
    }

    private fun updateSection(section: Section, course: EduCourse, project: Project) {
      val sectionFromServerPosition = StepikConnector.getInstance().getSection(section.id)?.position ?: -1
      section.position = section.index
      val positionChanged = sectionFromServerPosition != section.position
      if (positionChanged) {
        showErrorNotification(project, "Failed to update section",
                              "It's impossible to update one section since it's position changed. Please, use 'Update course' action.")
        return
      }
      val updated = CCStepikConnector.updateSection(section, course, project)
      if (updated) {
        EduUtils.showNotification(project, "Section \"${section.name}\" updated",
                                  CCStepikConnector.openOnStepikAction("/course/${course.id}"))
      }
    }
  }
}