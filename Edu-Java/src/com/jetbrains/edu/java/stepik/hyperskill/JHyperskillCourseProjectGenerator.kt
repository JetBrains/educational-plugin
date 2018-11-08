package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.getLesson

private const val PROJECT_PREFIX = "Project # "

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    return try {
      ProgressManager.getInstance().run(object : Task.WithResult<Boolean, Exception>(null, "Loading course", true) {
        override fun compute(indicator: ProgressIndicator): Boolean {
          val language = myCourse.languageById
          val hyperskillAccount = HyperskillSettings.INSTANCE.account
          if (hyperskillAccount == null) {
            LOG.error("User is not logged in to the Hyperskill")
            return false
          }
          val userInfo = hyperskillAccount.userInfo
          val hyperskillProject = userInfo.hyperskillProject
          if (hyperskillProject == null) {
            LOG.error("User didn't choose project on hyperskill")
            return false
          }
          val lessonId = hyperskillProject.lesson
          val projectId = hyperskillProject.id

          val stages = HyperskillConnector.getStages(projectId) ?: return false
          val lesson = getLesson(lessonId, language, stages) ?: return false
          lesson.name = hyperskillProject.title.removePrefix(PROJECT_PREFIX)

          myCourse.addLesson(FrameworkLesson(lesson))
          return true
        }
      })

    }
    catch (e: Exception) {
      LOG.warn(e)
      false
    }
  }

  companion object {
    private val LOG = Logger.getInstance(JHyperskillCourseProjectGenerator::class.java)
  }
}
