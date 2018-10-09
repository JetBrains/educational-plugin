package com.jetbrains.edu.java.learning.stepik.alt

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.stepik.alt.HyperskillConnector
import com.jetbrains.edu.learning.stepik.alt.HyperskillSettings
import com.jetbrains.edu.learning.stepik.alt.getLesson

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    return try {
      val language = myCourse.languageById
      val userInfo = HyperskillSettings.instance.account?.userInfo ?: return false
      val lessonId = userInfo.project?.lesson ?: return false
      val projectId = myCourse.id

      val stages = HyperskillConnector.getStages(projectId) ?: return false
      val lesson = getLesson(lessonId, language, stages) ?: return false
      lesson.name = userInfo.project?.title?.removePrefix("Project # ") // TODO: better place for prefix

      myCourse.addLesson(FrameworkLesson(lesson))
      true
    }
    catch (e: Exception) {
      LOG.warn(e)
      false
    }
  }

  companion object {
    @JvmStatic
    private val LOG = Logger.getInstance(JHyperskillCourseProjectGenerator::class.java)
  }
}
