package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ALL_FILES
import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequestHandler
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToTask
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

object HyperskillOpenInIdeRequestHandler : OpenInIdeRequestHandler<HyperskillOpenRequest>() {
  private val LOG = Logger.getInstance(HyperskillOpenInIdeRequestHandler::class.java)

  override val courseLoadingProcessTitle: String get() = EduCoreBundle.message("hyperskill.loading.project")

  private fun hasAndroidEnvironment(course: Course): Boolean = course.environment == EduNames.ANDROID

  override fun openInExistingProject(
    request: HyperskillOpenRequest,
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?
  ): Project? {
    when (request) {
      is HyperskillOpenStepRequestBase -> {
        val stepId = request.stepId
        val stepSource = getStepSource(stepId, request.isLanguageSelectedByUser)
        val isAndroidEnvRequired = stepSource.framework == EduNames.ANDROID
        val courseFilter: (Course) -> Boolean = if (isAndroidEnvRequired) ::hasAndroidEnvironment else { _ -> true }
        val (project, course) = findExistingProject(findProject, request, courseFilter) ?: return null
        val hyperskillCourse = course as HyperskillCourse
        hyperskillCourse.addProblemsWithTopicWithFiles(project, stepSource)
        hyperskillCourse.selectedProblem = stepId
        runInEdt {
          requestFocus()
          navigateToStep(project, hyperskillCourse, stepId)
        }
        synchronizeProjectOnStepOpening(project, hyperskillCourse, stepId)
        return project
      }

      is HyperskillOpenProjectStageRequest -> {
        val (project, course) = findExistingProject(findProject, request) ?: return null
        val hyperskillCourse = course as HyperskillCourse
        if (hyperskillCourse.getProjectLesson() == null) {
          computeUnderProgress(project, EduCoreBundle.message("hyperskill.loading.stages")) {
            HyperskillConnector.getInstance().loadStages(hyperskillCourse)
          }
          hyperskillCourse.init(false)
          val projectLesson = hyperskillCourse.getProjectLesson() ?: return null
          val courseDir = project.courseDir
          GeneratorUtils.createLesson(project, projectLesson, courseDir)
          GeneratorUtils.unpackAdditionalFiles(CourseInfoHolder.fromCourse(course, courseDir), ALL_FILES)
          YamlFormatSynchronizer.saveAll(project)
          course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.DEPENDENCIES_UPDATED)
          synchronizeProjectOnStageOpening(project, hyperskillCourse, projectLesson.taskList)
        }
        course.selectedStage = request.stageId
        runInEdt { openSelectedStage(hyperskillCourse, project) }
        return project
      }

    }
  }

  private fun navigateToStep(project: Project, course: Course, stepId: Int) {
    if (stepId == 0) {
      return
    }
    val task = getTask(course, stepId) ?: return
    navigateToTask(project, task)
  }

  private fun getTask(course: Course, stepId: Int): Task? {
    val taskRef = Ref<Task>()
    course.visitLessons { lesson: Lesson ->
      val task = lesson.getTask(stepId) ?: return@visitLessons
      taskRef.set(task)
    }
    return taskRef.get()
  }

  private fun findExistingProject(
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?,
    request: HyperskillOpenRequest,
    courseFilter: (Course) -> Boolean = { true }
  ): Pair<Project, Course>? {
    return when (request) {
      is HyperskillOpenProjectStageRequest -> findProject { it.matchesById(request.projectId) }
      is HyperskillOpenStepWithProjectRequest -> {
        val hyperskillLanguage = request.language
        val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage) ?: return null

        findProject {
          it.matchesById(request.projectId) && it.languageId == languageId && it.languageVersion == languageVersion && courseFilter(it)
        }
        ?: findProject { course -> course.isHyperskillProblemsCourse(hyperskillLanguage) }
      }

      is HyperskillOpenStepRequest -> {
        val hyperskillLanguage = request.language
        val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage) ?: return null
        findProject { it is HyperskillCourse && it.languageId == languageId && it.languageVersion == languageVersion && courseFilter(it) }
        ?: findProject { course -> course.isHyperskillProblemsCourse(hyperskillLanguage) && courseFilter(course) }
      }
    }
  }

  private fun Course.isHyperskillProblemsCourse(hyperskillLanguage: String) =
    this is HyperskillCourse && name in listOf(
      getProblemsProjectName(hyperskillLanguage),
      getLegacyProblemsProjectName(hyperskillLanguage)
    )

  private fun Course.matchesById(projectId: Int) = this is HyperskillCourse && hyperskillProject?.id == projectId

  fun createHyperskillCourse(
    request: HyperskillOpenRequest,
    hyperskillLanguage: String,
    hyperskillProject: HyperskillProject
  ): Result<HyperskillCourse, CourseValidationResult> {
    val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage)
                                        ?: return Err(
                                          ValidationErrorMessage(
                                            EduCoreBundle.message(
                                              "hyperskill.unsupported.language",
                                              hyperskillLanguage
                                            )
                                          )
                                        )

    if (!hyperskillProject.useIde) {
      return Err(ValidationErrorMessageWithHyperlinks(EduCoreBundle.message("hyperskill.project.not.supported", HYPERSKILL_PROJECTS_URL)))
    }

    val eduEnvironment = hyperskillProject.eduEnvironment
                         ?: return Err(ValidationErrorMessage("Unsupported environment ${hyperskillProject.environment}"))

    if (request is HyperskillOpenStepWithProjectRequest) {
      // This condition is about opening e.g. Python problem with chosen Kotlin's project,
      // otherwise - open Kotlin problem in current Kotlin project itself later below
      if (hyperskillLanguage != hyperskillProject.language) {
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))
      }

      // This is about opening Kotlin problem with currently chosen Android's project
      // But all Android projects are always Kotlin one's
      // So it should be possible to open problem in IntelliJ IDEA too e.g. (EDU-4641)
      if (eduEnvironment == EduNames.ANDROID && hyperskillLanguage == KOTLIN) {
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))
      }

      // EDU-5994: if Step has a field 'framework' == 'Android', it should be opened only in Android Studio
      // as an Android project.
      val stepSource = getStepSource(request.stepId, request.isLanguageSelectedByUser)
      // When the user has selected project on Hyperskill, the Plugin has to check if it is an Android project,
      // if not: a new one should be created.
      if (stepSource.framework == EduNames.ANDROID && eduEnvironment != EduNames.ANDROID) {
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion, EduNames.ANDROID))
      }

    }
    if (request is HyperskillOpenStepRequest) return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))

    // Android projects must be opened in Android Studio only
    if (eduEnvironment == EduNames.ANDROID && !EduUtilsKt.isAndroidStudio()) {
      return Err(ValidationErrorMessageWithHyperlinks(EduCoreBundle.message("rest.service.android.not.supported")))
    }

    return Ok(HyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment))
  }

  override fun getCourse(request: HyperskillOpenRequest, indicator: ProgressIndicator): Result<Course, CourseValidationResult> {
    if (request is HyperskillOpenStepRequest) {
      val newProject = HyperskillProject()
      val hyperskillLanguage = request.language
      val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, newProject).onError { return Err(it) }
      hyperskillCourse.addProblemsWithTopicWithFiles(null, getStepSource(request.stepId, request.isLanguageSelectedByUser))
      hyperskillCourse.selectedProblem = request.stepId
      return Ok(hyperskillCourse)
    }
    request as HyperskillOpenWithProjectRequestBase
    val hyperskillProject = HyperskillConnector.getInstance().getProject(request.projectId).onError {
      return Err(ValidationErrorMessage(it))
    }

    val hyperskillLanguage = if (request is HyperskillOpenStepWithProjectRequest) request.language else hyperskillProject.language

    val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, hyperskillProject).onError {
      return Err(it)
    }

    hyperskillCourse.validateLanguage(hyperskillLanguage).onError { return Err(it) }

    when (request) {
      is HyperskillOpenStepWithProjectRequest -> {
        hyperskillCourse.addProblemsWithTopicWithFiles(null, getStepSource(request.stepId, request.isLanguageSelectedByUser))
        hyperskillCourse.selectedProblem = request.stepId
      }

      is HyperskillOpenProjectStageRequest -> {
        indicator.text2 = EduCoreBundle.message("hyperskill.loading.stages")
        HyperskillConnector.getInstance().loadStages(hyperskillCourse)
        hyperskillCourse.selectedStage = request.stageId
      }
    }
    return Ok(hyperskillCourse)
  }

  @VisibleForTesting
  fun getStepSource(stepId: Int, isLanguageSelectedByUser: Boolean): HyperskillStepSource {
    val connector = HyperskillConnector.getInstance()
    val stepSource = connector.getStepSource(stepId).onError { error(it) }

    // Choosing language by user is allowed only for Data tasks, see EDU-4718
    if (isLanguageSelectedByUser && !stepSource.isDataTask()) {
      error("Language has been selected by user not for data task, but it must be specified for other tasks in request")
    }
    return stepSource
  }

  private fun HyperskillStepSource.isDataTask(): Boolean = block?.name == DataTask.DATA_TASK_TYPE

  private fun synchronizeProjectOnStepOpening(project: Project, course: HyperskillCourse, stepId: Int) {
    if (isUnitTestMode) {
      return
    }

    val task = course.getProblem(stepId) ?: return
    val tasks = task.lesson.taskList
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
  }

  private fun synchronizeProjectOnStageOpening(project: Project, course: HyperskillCourse, tasks: List<Task>) {
    if (isUnitTestMode) {
      return
    }
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
    HyperskillStartupActivity.synchronizeTopics(project, course)
  }
}