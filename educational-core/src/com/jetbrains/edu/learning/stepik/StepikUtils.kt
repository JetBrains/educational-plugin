/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("StepikUtils")

package com.jetbrains.edu.learning.stepik

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.net.URL

private const val PROMOTED_COURSES_LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/featured_courses.txt"
private const val IN_PROGRESS_COURSES_LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/in_progress_courses.txt"
private const val FEATURED_STEPIK_COURSES_LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/featured_stepik_courses.txt"

private val LOG = Logger.getInstance(StepikAuthorizer::class.java)

val featuredCourses = getCoursesIds(PROMOTED_COURSES_LINK)
val inProgressCourses = getCoursesIds(IN_PROGRESS_COURSES_LINK)
val featuredStepikCourses = getCourseIdsWithLanguage(FEATURED_STEPIK_COURSES_LINK)

fun setCourseLanguageEnvironment(info: EduCourse) {
  val courseFormat = info.type
  val languageIndex = courseFormat.indexOf(" ")
  if (languageIndex != -1) {
    val environmentIndex = courseFormat.indexOf(EduCourse.ENVIRONMENT_SEPARATOR, languageIndex + 1)
    if (environmentIndex != -1) {
      info.language = courseFormat.substring(languageIndex + 1, environmentIndex)
      info.environment = courseFormat.substring(environmentIndex + 1)
    }
    else {
      info.language = courseFormat.substring(languageIndex + 1)
    }
  }
  else {
    LOG.info(String.format("Language for course `%s` with `%s` type can't be set because it isn't \"pycharm\" course",
                           info.name, courseFormat))
  }
}

fun getStepikLink(task: Task, lesson: Lesson): String {
  return "${StepikNames.STEPIK_URL}/lesson/${lesson.id}/step/${task.index}"
}

fun updateCourseIfNeeded(project: Project, course: EduCourse) {
  val id = course.id
  if (id == 0 || !course.isStudy) {
    return
  }

  ProgressManager.getInstance().run(
    object : com.intellij.openapi.progress.Task.Backgroundable(project, "Checking for Course Updates") {
      override fun run(indicator: ProgressIndicator) {
        if (!course.isUpToDate()) {
          showUpdateAvailableNotification(project) {
            StepikCourseUpdater(course, project).updateCourse()
            SubmissionsManager.loadMissingSubmissions(course)
            StepikSolutionsLoader.getInstance(project).loadSolutionsInBackground()
          }
        }
      }
    })
}

fun showUpdateAvailableNotification(project: Project, updateAction: () -> Unit) {
  val notification = Notification("Update.course", "Course Updates",
                                  "Course is ready to <a href=\"update\">update</a>",
                                  NotificationType.INFORMATION,
                                  NotificationListener { notification, _ ->
                                    FileEditorManagerEx.getInstanceEx(project).closeAllFiles()
                                    ProgressManager.getInstance().runProcessWithProgressSynchronously(
                                      {
                                        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
                                        notification.expire()
                                        updateAction()
                                      },
                                      "Updating Course", true, project)
                                  })
  notification.notify(project)
}

private fun getCourseIdsWithLanguage(link: String): Map<Int, String> {
  val url = URL(link)
  val text = url.readText()
  return text.lines().associate {
    val partWithoutComment = it.split("#")[0]
    val idWithLanguage = partWithoutComment.split(" ")
    idWithLanguage[0].trim().toInt() to idWithLanguage[1].trim()
  }
}

private fun getCoursesIds(link: String): List<Int> {
  val url = URL(link)
  val text = url.readText()
  return text.lines().map { it.split("#")[0].trim().toInt() }
}

