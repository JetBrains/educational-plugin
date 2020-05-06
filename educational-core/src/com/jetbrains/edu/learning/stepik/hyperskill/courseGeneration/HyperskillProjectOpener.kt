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

  fun open(request: HyperskillOpenInProjectRequest): Result<Unit, String> {
    runInEdt {
      // We might perform heavy operations (including network access)
      // So we want to request focus and show progress bar so as it won't seem that IDE doesn't respond
      requestFocus()
    }
    if (openInOpenedProject(request)) return Ok(Unit)
    if (openInRecentProject(request)) return Ok(Unit)
    return openInNewProject(request)
  }

  private fun openInExistingProject(request: HyperskillOpenInProjectRequest,
                                    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?): Boolean {
    val (project, course) = findProject { it is HyperskillCourse && it.hyperskillProject?.id == request.projectId }
                            ?: return false
    val hyperskillCourse = course as HyperskillCourse
    when (request) {
      is HyperskillOpenStepRequest -> {
        val stepId = request.stepId
        hyperskillCourse.addProblemTaskWithFiles(project, stepId)
        hyperskillCourse.putUserData(HYPERSKILL_SELECTED_PROBLEM, request.stepId)
        runInEdt {
          requestFocus()
          EduUtils.navigateToStep(project, hyperskillCourse, stepId)
        }
      }
      is HyperskillOpenStageRequest -> {
        if (hyperskillCourse.getProjectLesson() == null) {
          computeUnderProgress(project, LOADING_PROJECT_STAGES) {
            HyperskillConnector.getInstance().loadStages(hyperskillCourse)
          }
          hyperskillCourse.init(null, null, false)
          val projectLesson = hyperskillCourse.getProjectLesson()!!
          val courseDir = hyperskillCourse.getDir(project)
          GeneratorUtils.createLesson(projectLesson, courseDir)
          GeneratorUtils.createAdditionalFiles(course, courseDir)
          YamlFormatSynchronizer.saveAll(project)
          HyperskillProjectComponent.synchronizeHyperskillProject(project)
          course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.DEPENDENCIES_UPDATED)
        }
        hyperskillCourse.putUserData(HYPERSKILL_SELECTED_STAGE, request.stageId)
        runInEdt { openSelectedStage(hyperskillCourse, project) }
      }
    }
    HyperskillProjectComponent.synchronizeHyperskillProject(project)
    return true
  }

  private fun openInOpenedProject(request: HyperskillOpenInProjectRequest): Boolean =
    openInExistingProject(request, EduBuiltInServerUtils::focusOpenProject)

  private fun openInRecentProject(request: HyperskillOpenInProjectRequest): Boolean =
    openInExistingProject(request, EduBuiltInServerUtils::openRecentProject)


  private fun openInNewProject(request: HyperskillOpenInProjectRequest): Result<Unit, String> {
    return getHyperskillCourseUnderProgress(request).map { hyperskillCourse ->
      runInEdt {
        requestFocus()
        HyperskillJoinCourseDialog(hyperskillCourse).show()
      }
    }
  }

  private fun getHyperskillCourseUnderProgress(request: HyperskillOpenInProjectRequest): Result<HyperskillCourse, String> {
    return computeUnderProgress(title = "Loading ${EduNames.JBA} Project") { indicator ->
      val hyperskillProject = when (val result = HyperskillConnector.getInstance().getProject(request.projectId)) {
        is Err -> return@computeUnderProgress Err(result.error)
        is Ok -> result.value
      }

      if (!hyperskillProject.useIde) {
        return@computeUnderProgress Err(HYPERSKILL_PROJECT_NOT_SUPPORTED)
      }
      val languageId = HYPERSKILL_LANGUAGES[hyperskillProject.language]
      if (languageId == null) {
        return@computeUnderProgress Err("Unsupported language ${hyperskillProject.language}")
      }
      val eduEnvironment = hyperskillProject.eduEnvironment
      if (eduEnvironment == null) {
        return@computeUnderProgress Err("Unsupported environment ${hyperskillProject.environment}")
      }
      val hyperskillCourse = HyperskillCourse(hyperskillProject, languageId, eduEnvironment)
      if (hyperskillCourse.configurator == null) {
        return@computeUnderProgress Err("The project isn't supported (language: ${hyperskillProject.language}). " +
                                        "Check if all needed plugins are installed and enabled")
      }
      when (request) {
        is HyperskillOpenStepRequest -> {
          hyperskillCourse.addProblemTask(request.stepId)
          hyperskillCourse.putUserData(HYPERSKILL_SELECTED_PROBLEM, request.stepId)
        }
        is HyperskillOpenStageRequest -> {
          indicator.text2 = LOADING_PROJECT_STAGES
          HyperskillConnector.getInstance().loadStages(hyperskillCourse)
          hyperskillCourse.putUserData(HYPERSKILL_SELECTED_STAGE, request.stageId)
        }
      }
      Ok(hyperskillCourse)
    }
  }

  private fun HyperskillCourse.addProblemTask(stepId: Int) {
    var lesson = getProblemsLesson()
    if (lesson == null) {
      lesson = createProblemsLesson()
    }

    val task = lesson.getTask(stepId)
    if (task == null) {
      lesson.createProblemTask(stepId)
    }
  }

  private fun HyperskillCourse.createProblemsLesson(): Lesson {
    val lesson = Lesson()
    lesson.name = HYPERSKILL_PROBLEMS
    lesson.index = this.items.size + 1
    lesson.course = this
    addLesson(lesson)
    return lesson
  }

  private fun Lesson.createProblemTask(stepId: Int): Task {
    val task = computeUnderProgress(title = "Loading ${EduNames.JBA} Code Challenge") {
      HyperskillConnector.getInstance().getCodeChallenges(course, this, listOf(stepId))
    }.firstOrNull() ?: error("Failed to load problem: id = $stepId")
    addTask(task)
    return task
  }

  private fun HyperskillCourse.addProblemTaskWithFiles(project: Project, stepId: Int) {
    var problemsLesson = getProblemsLesson()
    var createLessonDir = false
    if (problemsLesson == null) {
      problemsLesson = createProblemsLesson()
      createLessonDir = true
    }

    var task = problemsLesson.getTask(stepId)
    var createTaskDir = false
    if (task == null) {
      task = problemsLesson.createProblemTask(stepId)
      createTaskDir = true
    }

    problemsLesson.init(course, null, false)

    if (createLessonDir) {
      GeneratorUtils.createLesson(problemsLesson, course.getDir(project))
      YamlFormatSynchronizer.saveAll(project)
    }
    else if (createTaskDir) {
      GeneratorUtils.createTask(task, problemsLesson.getDir(project)!!)
      YamlFormatSynchronizer.saveItem(problemsLesson)
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
    frame?.toFront()
  }
}