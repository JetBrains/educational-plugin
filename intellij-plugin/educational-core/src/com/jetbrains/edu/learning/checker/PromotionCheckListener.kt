package com.jetbrains.edu.learning.checker

import com.intellij.ide.util.RunOnceUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import org.jetbrains.annotations.VisibleForTesting

class PromotionCheckListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (task.course.isStudy) {
      RunOnceUtil.runOnceForApp(STUDENT_PACK_PROMOTION_SHOWN_KEY) {
        EduNotificationManager.create(
          NotificationType.INFORMATION,
          EduCoreBundle.message("promotion.student.pack.notification.title"),
          EduCoreBundle.message("promotion.student.pack.notification.content")
        ).addAction(object : AnAction(EduCoreBundle.message("promotion.student.pack.notification.action")) {
          override fun actionPerformed(e: AnActionEvent) {
            EduBrowser.getInstance().browse(STUDENT_PACK_LINK)
            e.getData(Notification.KEY)?.expire()
          }
        }).notify(project)
      }
    }
  }
}

@VisibleForTesting
const val STUDENT_PACK_PROMOTION_SHOWN_KEY = "edu.student.pack.promotion.shown"
@VisibleForTesting
const val STUDENT_PACK_LINK = "https://jb.gg/academy-free-student-pack"
