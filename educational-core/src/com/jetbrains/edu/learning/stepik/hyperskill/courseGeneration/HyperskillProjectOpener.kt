package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

object HyperskillProjectOpener {

  fun openProject(projectId: Int, stageId: Int? = null, stepId: Int? = null): Result<Unit, String> {
    runInEdt {
      requestFocus()
    }
    if (openInOpenedProject(projectId, stageId, stepId)) return Ok(Unit)
    if (openInRecentProject(projectId, stageId, stepId)) return Ok(Unit)
    return openInNewProject(projectId, stageId, stepId)
  }

  private fun openInExistingProject(projectId: Int,
                                    stageId: Int?,
                                    stepId: Int?,
                                    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?): Boolean {
    val (project, course) = findProject { it is HyperskillCourse && it.hyperskillProject?.id == projectId }
                            ?: return false
    val hyperskillCourse = course as HyperskillCourse
    if (stepId != null) {
      hyperskillCourse.addProblemWithFiles(project, stepId)
      runInEdt {
        requestFocus()
        EduUtils.navigateToStep(project, hyperskillCourse, stepId)
      }
    }
    else {
      if (hyperskillCourse.getProjectLesson() == null) {
        computeUnderProgress(project, "Loading Project Stages") {
          val hyperskillProject = hyperskillCourse.hyperskillProject!!
          HyperskillConnector.getInstance().loadStages(hyperskillCourse, hyperskillProject.id, hyperskillProject)
        }
        hyperskillCourse.init(null, null, false)
        val projectLesson = hyperskillCourse.getProjectLesson()!!
        GeneratorUtils.createLesson(projectLesson, hyperskillCourse.getDir(project))
        YamlFormatSynchronizer.saveAll(project)
        HyperskillProjectComponent.synchronizeHyperskillProject(project)
      }
      hyperskillCourse.putUserData(HYPERSKILL_STAGE, stageId)
      runInEdt { openSelectedStage(hyperskillCourse, project, true) }
    }
    return true
  }

  private fun openInOpenedProject(projectId: Int, stageId: Int?, stepId: Int?): Boolean =
    openInExistingProject(projectId, stageId, stepId, EduBuiltInServerUtils::focusOpenProject)

  private fun openInRecentProject(projectId: Int, stageId: Int?, stepId: Int?): Boolean =
    openInExistingProject(projectId, stageId, stepId, EduBuiltInServerUtils::openRecentProject)


  private fun openInNewProject(projectId: Int, stageId: Int?, stepId: Int?): Result<Unit, String> {
    return getHyperskillCourseUnderProgress(projectId, stageId, stepId).map { hyperskillCourse ->
      runInEdt {
        requestFocus()
        HyperskillJoinCourseDialog(hyperskillCourse).show()
      }
    }
  }

  private fun getHyperskillCourseUnderProgress(projectId: Int, stageId: Int?, stepId: Int?): Result<HyperskillCourse, String> {
    return computeUnderProgress(title = "Loading ${EduNames.JBA} Project") { indicator ->
      val hyperskillProject = HyperskillConnector.getInstance().getProject(projectId) ?: return@computeUnderProgress Err(
        FAILED_TO_CREATE_PROJECT)

      if (!hyperskillProject.useIde) {
        Err(HYPERSKILL_PROJECT_NOT_SUPPORTED)
      }
      val languageId = HYPERSKILL_LANGUAGES[hyperskillProject.language]
      if (languageId == null) {
        return@computeUnderProgress Err("Unsupported language ${hyperskillProject.language}")
      }
      val hyperskillCourse = HyperskillCourse(hyperskillProject, languageId)
      if (hyperskillCourse.configurator == null) {
        Err("The project isn't supported (language: ${hyperskillProject.language}). " +
            "Check if all needed plugins are installed and enabled")
      }
      if (stepId != null) {
        hyperskillCourse.addProblem(stepId)
      }
      else {
        indicator.text2 = "Loading Project Stages"
        HyperskillConnector.getInstance().loadStages(hyperskillCourse, projectId, hyperskillProject)
        hyperskillCourse.putUserData(HYPERSKILL_STAGE, stageId)
      }
      Ok(hyperskillCourse)
    }
  }

  private fun HyperskillCourse.addProblem(stepId: Int): Pair<Lesson, Task> {
    val stepSource = computeUnderProgress(title = "Loading ${EduNames.JBA} Code Challenge") {
      HyperskillConnector.getInstance().getStepSource(stepId)
    } ?: error("Failed to load problem: id = $stepId")

    fun Lesson.addProblem(): Task {
      var task = getTask(stepSource.id)
      if (task == null) {
        task = HyperskillConnector.getInstance().getTasks(course, this, listOf(stepSource)).first().apply {
          index = taskList.size + 1
        }
        addTask(task)
      }
      return task
    }

    val lesson = findOrCreateProblemsLesson()
    return lesson to lesson.addProblem()
  }

  private fun HyperskillCourse.addProblemWithFiles(project: Project, stepId: Int) {
    val (lesson, task) = addProblem(stepId)
    lesson.init(course, null, false)
    val lessonDir = lesson.getDir(project)
    if (lessonDir == null) {
      GeneratorUtils.createLesson(lesson, course.getDir(project))
      YamlFormatSynchronizer.saveAll(project)
    }
    else if (task.getDir(project) == null) {
      GeneratorUtils.createTask(task, lessonDir)
      YamlFormatSynchronizer.saveItem(lesson)
      YamlFormatSynchronizer.saveItem(task)
      YamlFormatSynchronizer.saveRemoteInfo(task)
      course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    }
  }

  // We have to use visible frame here because project is not yet created
  // See `com.intellij.ide.impl.ProjectUtil.focusProjectWindow` implementation for more details
  private fun requestFocus() {
    val frame = WindowManager.getInstance().findVisibleFrame()
    if (frame is IdeFrame) {
      AppIcon.getInstance().requestFocus(frame)
    }
    frame.toFront()
  }
}