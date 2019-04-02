package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.java.JLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.getLesson
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    assert(myCourse is HyperskillCourse)
    return try {
      ProgressManager.getInstance().run(object : Task.WithResult<Boolean, Exception>(null, "Loading hyperskill project", true) {
        override fun compute(indicator: ProgressIndicator): Boolean {
          val language = myCourse.languageById
          val hyperskillAccount = HyperskillSettings.INSTANCE.account
          if (hyperskillAccount == null) {
            LOG.error("User is not logged in to the Hyperskill")
            return false
          }
          val hyperskillCourse = myCourse as HyperskillCourse
          val hyperskillProject = hyperskillCourse.hyperskillProject
          if (!hyperskillProject.useIde) {
            LOG.error("Selected project is not supported")
            return false
          }
          val projectId = hyperskillProject.id

          if (hyperskillCourse.stages.isEmpty()) {
            val stages = HyperskillConnector.getStages(projectId) ?: return false
            hyperskillCourse.stages = stages
          }
          val stages = hyperskillCourse.stages
          val lesson = getLesson(hyperskillCourse, hyperskillProject.ideFiles, language)
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
            task.name = stages[index].title
          }
          lesson.name = hyperskillProject.title

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

  override fun getJdk(settings: JdkProjectSettings): Sdk? =
    super.getJdk(settings) ?: JLanguageSettings.findSuitableJdk(myCourse, settings.model)

  override fun loadSolutions(project: Project, course: Course) {
    if (course.isStudy && course is HyperskillCourse && HyperskillSettings.INSTANCE.account != null) {
      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }
  }

  companion object {
    private val LOG = Logger.getInstance(JHyperskillCourseProjectGenerator::class.java)
  }
}
