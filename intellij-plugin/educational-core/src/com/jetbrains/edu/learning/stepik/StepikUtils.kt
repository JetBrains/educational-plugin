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

import com.intellij.notification.NotificationListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduErrorNotification
import com.jetbrains.edu.learning.notification.EduInformationNotification
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikProfilePath


fun getStepikLink(task: Task, lesson: Lesson): String {
  return "${StepikNames.getStepikUrl()}/lesson/${lesson.id}/step/${task.index}"
}

fun showUpdateAvailableNotification(project: Project, updateAction: () -> Unit) {
  EduInformationNotification(
    EduCoreBundle.message("update.content"),
    EduCoreBundle.message("update.content.request"),
  )
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
      EduCoreBundle.message("push.course.updating.progress.text"), true, project)
  }
}

fun notifyStepikUnauthorized(project: Project, specificMessage: String) {
  EduErrorNotification(specificMessage, EduCoreBundle.message("stepik.auth.error.message"))
    .notify(project)
}

val StepikUser.profileUrl: String get() = "${getStepikProfilePath()}${userInfo.id}"
