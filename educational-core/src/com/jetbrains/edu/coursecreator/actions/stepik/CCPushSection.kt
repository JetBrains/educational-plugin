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
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.FAILED_TITLE
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
    val view = e.getData(LangDataKeys.IDE_VIEW)
    val project = e.getData(CommonDataKeys.PROJECT)
    if (view == null || project == null) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (course.courseMode != CCUtils.COURSE_MODE || !course.isRemote) return
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
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
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
            section.position = sectionPosition(course, section.name)
            CCStepikConnector.postSection(project, section, indicator)
            if (section.position < course.sections.size) {
              updateSectionsPositions(project, course.sections.slice(IntRange(section.position, course.sections.size - 1)),
                                      section.position + 1)
            }
            EduUtils.showNotification(project, "Section \"${section.name}\" posted",
                                      CCStepikConnector.openOnStepikAction("/course/${course.id}"))
          }
        }
      })
    }

    private fun updateSection(section: Section, course: EduCourse, project: Project) {
      val sectionFromServerPosition = StepikConnector.getSection(section.id)?.position ?: -1
      section.position = sectionPosition(course, section.name)
      val positionChanged = sectionFromServerPosition != section.position
      val updated = CCStepikConnector.updateSection(section, course, project)

      if (positionChanged && section.position < course.sections.size) {
        updateSectionsPositions(project, course.sections, 1 + if (course.hasTopLevelLessons) 1 else 0)
      }

      if (updated) {
        EduUtils.showNotification(project, "Section \"${section.name}\" updated",
                                  CCStepikConnector.openOnStepikAction("/course/${course.id}"))
      }
    }

    private fun sectionPosition(course: EduCourse, sectionName: String): Int {
      val position = 1 + if (course.hasTopLevelLessons) 1 else 0

      val sectionsCount = course.sections
        .takeWhile { it.name != sectionName }
        .count { it.id > 0 }
      return position + sectionsCount
    }

    private fun updateSectionsPositions(project: Project, sectionsToUpdate: List<Section>, initialPosition: Int) {
      var position = initialPosition
      for (section in sectionsToUpdate) {
        if (section.id == 0) continue
        section.position = position++
        val updatedSectionInfo = CCStepikConnector.updateSectionInfo(section)
        if (updatedSectionInfo == null) {
          showErrorNotification(project, FAILED_TITLE, "Failed to update section " + section.id)
        }
      }
    }
  }

}