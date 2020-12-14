package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LoginWidgetProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse

class StepikWidgetProvider : LoginWidgetProvider() {

  override fun createLoginWidget(project: Project) = StepikWidget(project)

  override fun isWidgetAvailable(courseType: String, languageId: String?, courseMode: String?): Boolean {
    return (courseType == EduNames.PYCHARM || courseType == StepikNames.STEPIK_TYPE) && EduNames.STUDY == courseMode
  }

  override fun isWidgetAvailable(course: Course) = course is EduCourse && !course.isMarketplace && course.isStudy
}
