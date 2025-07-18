package com.jetbrains.edu.learning.marketplace.awsTracks.course

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.StartCourseAction
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.awsTracks.AWS
import com.jetbrains.edu.learning.marketplace.awsTracks.api.AWSConnector
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

class StartAWSCourseAction : StartCourseAction(AWS) {
  override fun courseConnector(): CourseConnector = AWSConnector.getInstance()
  override fun createImportCourseDialog(): ImportCourseDialog = StartAWSCourseDialog(courseConnector())

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = AWSConnector.getInstance().isLoggedIn() && isFeatureEnabled(EduExperimentalFeatures.AWS_COURSES)
  }
}