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
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikProfilePath
import com.jetbrains.edu.learning.submissions.SubmissionsManager

private val LOG = Logger.getInstance(StepikAuthorizer::class.java)

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
  return "${StepikNames.getStepikUrl()}/lesson/${lesson.id}/step/${task.index}"
}

/**
 * Pass [courseFromStepik] to avoid additional network request to get remote course info
 * if you already have up to date result of such request.
 * In case of `null`, remote course info will be retrieved via [com.jetbrains.edu.learning.stepik.api.StepikConnector.getCourseInfo].
 * Don't pass [courseFromStepik], if `[updateCourseOnStepik]` may be called after an indeterminate amount of time (e.g. in notification action)
 * after retrieving of remote course object. It may lead to outdated course info even after update
 */
fun updateCourseOnStepik(project: Project, course: EduCourse, courseFromStepik: EduCourse? = null) {
  StepikCourseUpdater(project, course).updateCourse(courseFromStepik)
  SubmissionsManager.getInstance(project).getSubmissions(course.allTasks)
  StepikSolutionsLoader.getInstance(project).loadSolutionsInBackground()
}

fun showUpdateAvailableNotification(project: Project, updateAction: () -> Unit) {
  Notification("EduTools",
               EduCoreBundle.message("update.content"),
               EduCoreBundle.message("update.content.request"),
               NotificationType.INFORMATION)
    .setListener(notificationListener(project, updateAction))
    .notify(project)
}

fun notificationListener(project: Project,
                         updateAction: () -> Unit): NotificationListener {
  return NotificationListener { notification, _ ->
    FileEditorManagerEx.getInstanceEx(project).closeAllFiles()
    notification.expire()
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        updateAction()
      },
      "Updating Course", true, project)
  }
}

fun notifyStepikUnauthorized(project: Project, specificMessage: String) {
  Notification("EduTools", specificMessage, EduCoreBundle.message("stepik.auth.error.message"), NotificationType.ERROR)
    .notify(project)
}

val StepikUser.profileUrl: String get() = "${getStepikProfilePath()}${userInfo.id}"
