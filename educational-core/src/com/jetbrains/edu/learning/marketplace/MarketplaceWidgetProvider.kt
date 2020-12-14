package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LoginWidgetProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse

class MarketplaceWidgetProvider : LoginWidgetProvider() {
  override fun isWidgetAvailable(courseType: String,
                                 languageId: String?,
                                 courseMode: String?) = courseType == MARKETPLACE && EduNames.STUDY == courseMode

  override fun isWidgetAvailable(course: Course) = course is EduCourse && course.isMarketplace && course.isStudy

  override fun createLoginWidget(project: Project) = MarketplaceWidget(project)
}