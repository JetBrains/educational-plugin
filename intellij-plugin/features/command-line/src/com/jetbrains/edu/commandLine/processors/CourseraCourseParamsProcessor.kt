package com.jetbrains.edu.commandLine.processors

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.lti.LTIOnlineService
import com.jetbrains.edu.learning.marketplace.lti.LTISettingsDTO
import com.jetbrains.edu.learning.marketplace.lti.LTISettingsManager
import com.jetbrains.edu.learning.navigation.NavigationUtils

class CourseraCourseParamsProcessor : CourseParamsProcessor {
  override fun shouldApply(project: Project, course: Course, params: Map<String, String>): Boolean {
    return params.containsKey("launch_id") && params.containsKey("lms_description")
  }

  override fun processCourseParams(project: Project, course: Course, params: Map<String, String>): Boolean {
    val studyItemId = params["study_item_id"]?.toIntOrNull()
    if (studyItemId != null) {
      val itemToOpen = course.allTasks.firstOrNull {
        it.id == studyItemId
        || it.lesson.id == studyItemId
        || it.lesson.section?.id == studyItemId
      }
      if (itemToOpen != null) {
        invokeLater {
          NavigationUtils.navigateToTask(project, itemToOpen)
        }
      }
    }
    val settingsState = LTISettingsManager.instance(project).state
    val launchId = params["launch_id"] ?: return false
    val lmsDescription = params["lms_description"] ?: return false
    val ltiSettings = LTISettingsDTO(launchId, lmsDescription, LTIOnlineService.STANDALONE)
    settingsState.launchId = ltiSettings.launchId
    settingsState.lmsDescription = ltiSettings.lmsDescription
    settingsState.onlineService = ltiSettings.onlineService
    return true
  }
}