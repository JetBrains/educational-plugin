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
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.stepik.StepikConnector
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
            val sectionFromServer = StepikConnector.getSection(section.id)
            section.position = sectionPosition(course, section.name)
            val positionChanged = sectionFromServer.position != section.position
            val updated = CCStepikConnector.updateSection(project, section)
            section.stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
            for (lesson in section.lessons) {
              lesson.stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
              for (task in lesson.taskList) {
                task.stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
              }
            }

            if (positionChanged && section.position < course.sections.size) {
              updateSectionsPositions(project, course.sections, 1 + if (course.hasTopLevelLessons) 1 else 0)
              CCStepikConnector.updateAdditionalSection(project)
            }

            if (updated) {
              CCStepikConnector.showNotification(project, "Section \"${section.name}\" updated",
                                                 CCStepikConnector.openOnStepikAction("/course/" + course.id))
            }
          }
          else {
            section.position = sectionPosition(course, section.name)
            CCStepikConnector.postSection(project, section, indicator)
            CCStepikConnector.updateAdditionalSection(project)
            if (section.position < course.sections.size) {
              updateSectionsPositions(project, course.sections.slice(IntRange(section.position, course.sections.size - 1)),
                                      section.position + 1)
            }
            CCStepikConnector.showNotification(project, "Section \"${section.name}\" posted",
                                               CCStepikConnector.openOnStepikAction("/course/" + course.id))
          }
        }
      })
    }

    private fun sectionPosition(course: RemoteCourse,
                                sectionName: String): Int {
      var position = 1 + if (course.hasTopLevelLessons) 1 else 0
      for (s in course.sections) {
        if (s.name == sectionName) {
          break
        }

        if (s.id > 0) {
          position++
        }
      }

      return position
    }

    private fun updateSectionsPositions(project: Project,
                                        sectionsToUpdate: List<Section>,
                                        initialPosition: Int) {
      var position = initialPosition
      for (s in sectionsToUpdate) {
        if (s.id == 0) continue
        s.position = position++
        CCStepikConnector.updateSectionInfo(project, s)
        s.stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
      }
    }
  }


}