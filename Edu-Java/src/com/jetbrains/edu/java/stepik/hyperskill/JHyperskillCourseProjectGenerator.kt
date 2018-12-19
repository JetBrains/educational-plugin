package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

private const val PROJECT_PREFIX = "Project # "

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    assert(myCourse is HyperskillCourse)
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

          if ((myCourse as HyperskillCourse).stages.isEmpty()) {
            val stages = HyperskillConnector.getStages(projectId) ?: return false
            (myCourse as HyperskillCourse).stages = stages
          }
          val stages = (myCourse as HyperskillCourse).stages
          val lesson = getLesson(myCourse as HyperskillCourse, lessonId, language)
          if (lesson == null) {
            LOG.warn("Project doesn't contain framework lesson")
            return false
          }
          if (lesson.taskList.size != stages.size) {
            LOG.warn("Course has ${stages.size} stages, but ${lesson.taskList.size} tasks")
            return false
          }

          lesson.taskList.forEachIndexed { index, task ->
            task.feedbackLink = feedbackLink(projectId, stages[index])
          }
          lesson.name = hyperskillProject.title.removePrefix(PROJECT_PREFIX)

          myCourse.addLesson(lesson)
          return true
        }
      })

    }
    catch (e: Exception) {
      LOG.warn(e)
      false
    }
  }

  fun feedbackLink(project: Int, stage: HyperskillStage): FeedbackLink {
    return FeedbackLink("$HYPERSKILL_PROJECTS_URL/$project/stages/${stage.id}/implement")
  }

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    HyperskillConnector.fillTopics(myCourse as HyperskillCourse, project)
  }

  companion object {
    private val LOG = Logger.getInstance(JHyperskillCourseProjectGenerator::class.java)
  }
}
