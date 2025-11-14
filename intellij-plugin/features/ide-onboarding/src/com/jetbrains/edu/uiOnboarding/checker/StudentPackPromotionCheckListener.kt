package com.jetbrains.edu.uiOnboarding.checker

import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingService
import org.jetbrains.annotations.VisibleForTesting

class StudentPackPromotionCheckListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (task.course.isStudy) {
      RunOnceUtil.runOnceForApp(STUDENT_PACK_PROMOTION_SHOWN_KEY) {
        EduUiOnboardingService.getInstance(project).promoteStudentPack()
      }
    }
  }
}

@VisibleForTesting
const val STUDENT_PACK_PROMOTION_SHOWN_KEY = "edu.student.pack.promotion.shown"
const val STUDENT_PACK_LINK = "https://jb.gg/academy-free-student-pack"
