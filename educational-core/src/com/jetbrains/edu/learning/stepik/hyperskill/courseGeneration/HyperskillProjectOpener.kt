package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Companion.pluginCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.getRequiredPluginsMessage
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.Companion.SUPPORTED_STEP_TYPES
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

object HyperskillProjectOpener {
  private val LOG = Logger.getInstance(HyperskillProjectOpener::class.java)

  fun open(request: HyperskillOpenInProjectRequest): Result<Boolean, String> {
    runInEdt {
      // We might perform heavy operations (including network access)
      // So we want to request focus and show progress bar so as it won't seem that IDE doesn't respond
      requestFocus()
    }
    if (openInOpenedProject(request)) return Ok(true)
    if (openInRecentProject(request)) return Ok(true)
    return openInNewProject(request)
  }

  private fun openInExistingProject(request: HyperskillOpenInProjectRequest,
                                    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?): Boolean {
    val (project, course) = findExistingProject(findProject, request) ?: return false
    val hyperskillCourse = course as HyperskillCourse
    when (request) {
      is HyperskillOpenStepRequest -> {
        val stepId = request.stepId
        hyperskillCourse.addProblemWithFiles(project, stepId)
        hyperskillCourse.dataHolder.putUserData(HYPERSKILL_SELECTED_PROBLEM, request.stepId)
        runInEdt {
          requestFocus()
          EduUtils.navigateToStep(project, hyperskillCourse, stepId)
        }
        synchronizeProjectOnStepOpening(project, hyperskillCourse, stepId)
      }
      is HyperskillOpenStageRequest -> {
        if (hyperskillCourse.getProjectLesson() == null) {
          computeUnderProgress(project, EduCoreBundle.message("hyperskill.loading.stages")) {
            HyperskillConnector.getInstance().loadStages(hyperskillCourse)
          }
          hyperskillCourse.init(null, null, false)
          val projectLesson = hyperskillCourse.getProjectLesson()!!
          val courseDir = hyperskillCourse.getDir(project.courseDir)
          GeneratorUtils.createLesson(projectLesson, courseDir)
          GeneratorUtils.createAdditionalFiles(course, courseDir)
          YamlFormatSynchronizer.saveAll(project)
          course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.DEPENDENCIES_UPDATED)
          synchronizeProjectOnStageOpening(project, hyperskillCourse, projectLesson.taskList)
        }
        hyperskillCourse.dataHolder.putUserData(HYPERSKILL_SELECTED_STAGE, request.stageId)
        runInEdt { openSelectedStage(hyperskillCourse, project) }
      }
    }
    return true
  }

  private fun findExistingProject(findProject: ((Course) -> Boolean) -> Pair<Project, Course>?,
                                  request: HyperskillOpenInProjectRequest): Pair<Project, Course>? {
    val projectId = request.projectId
    return when (request) {
      is HyperskillOpenStageRequest -> findProject { it.matchesById(projectId) }
      is HyperskillOpenStepRequest -> {
        val hyperskillLanguage = request.language
        val eduLanguage = HYPERSKILL_LANGUAGES[hyperskillLanguage] ?: return null

        findProject { it.matchesById(projectId) && it.language == eduLanguage }
        ?: findProject { course -> course.isHyperskillProblemsCourse(hyperskillLanguage) }
      }
    }
  }

  private fun Course.isHyperskillProblemsCourse(hyperskillLanguage: String) =
    this is HyperskillCourse && name in listOf(getProblemsProjectName(hyperskillLanguage),
                                               getLegacyProblemsProjectName(hyperskillLanguage))

  private fun Course.matchesById(projectId: Int) = this is HyperskillCourse && hyperskillProject?.id == projectId

  private fun openInOpenedProject(request: HyperskillOpenInProjectRequest): Boolean =
    openInExistingProject(request, HyperskillProjectManager.getInstance()::focusOpenProject)

  private fun openInRecentProject(request: HyperskillOpenInProjectRequest): Boolean =
    openInExistingProject(request, EduBuiltInServerUtils::openRecentProject)


  fun openInNewProject(request: HyperskillOpenInProjectRequest): Result<Boolean, String> {
    return getHyperskillCourseUnderProgress(request).map { hyperskillCourse ->
      getInEdt {
        requestFocus()
        HyperskillProjectManager.getInstance().newProject(hyperskillCourse)
      }
    }
  }

  fun createHyperskillCourse(request: HyperskillOpenInProjectRequest,
                             hyperskillLanguage: String,
                             hyperskillProject: HyperskillProject): Result<HyperskillCourse, String> {
    val eduLanguage = HYPERSKILL_LANGUAGES[hyperskillLanguage]
                      ?: return Err(EduCoreBundle.message("hyperskill.unsupported.language", hyperskillLanguage))

    if (request is HyperskillOpenStepRequest && hyperskillLanguage != hyperskillProject.language) {
      return Ok(HyperskillCourse(hyperskillLanguage, eduLanguage))
    }

    if (!hyperskillProject.useIde) {
      return Err(HYPERSKILL_PROJECT_NOT_SUPPORTED)
    }

    val eduEnvironment = hyperskillProject.eduEnvironment ?: return Err("Unsupported environment ${hyperskillProject.environment}")

    if (eduEnvironment == EduNames.ANDROID && !EduUtils.isAndroidStudio()) {
      return Err(EduCoreBundle.message("hyperskill.android.not.supported"))
    }

    return Ok(HyperskillCourse(hyperskillProject, eduLanguage, eduEnvironment))
  }

  private fun HyperskillCourse.validateLanguage(hyperskillLanguage: String): Result<Unit, String> {
    val pluginCompatibility = pluginCompatibility()
    if (pluginCompatibility is CourseCompatibility.PluginsRequired) {
      val requiredPluginsMessage = getRequiredPluginsMessage(pluginCompatibility.toInstallOrEnable)
      val helpLink = "https://www.jetbrains.com/help/idea/managing-plugins.html"
      return Err(
        """$requiredPluginsMessage<a href="$helpLink">${EduCoreBundle.message("course.dialog.error.plugin.install.and.enable")}.</a>"""
      )
    }

    if (configurator == null) {
      return Err(EduCoreBundle.message("hyperskill.language.not.supported",
                                       ApplicationNamesInfo.getInstance().productName,
                                       hyperskillLanguage.capitalize()))
    }
    return Ok(Unit)
  }

  private fun getHyperskillCourseUnderProgress(request: HyperskillOpenInProjectRequest): Result<HyperskillCourse, String> {
    return computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.project")) { indicator ->
      val hyperskillProject = HyperskillConnector.getInstance().getProject(request.projectId).onError {
        return@computeUnderProgress Err(it)
      }

      val hyperskillLanguage = if (request is HyperskillOpenStepRequest) request.language else hyperskillProject.language

      val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, hyperskillProject).onError {
        return@computeUnderProgress Err(it)
      }

      hyperskillCourse.validateLanguage(hyperskillLanguage).onError { return@computeUnderProgress Err(it) }

      when (request) {
        is HyperskillOpenStepRequest -> {
          hyperskillCourse.addProblem(request.stepId)
          hyperskillCourse.dataHolder.putUserData(HYPERSKILL_SELECTED_PROBLEM, request.stepId)
        }
        is HyperskillOpenStageRequest -> {
          indicator.text2 = EduCoreBundle.message("hyperskill.loading.stages")
          HyperskillConnector.getInstance().loadStages(hyperskillCourse)
          hyperskillCourse.dataHolder.putUserData(HYPERSKILL_SELECTED_STAGE, request.stageId)
        }
      }
      Ok(hyperskillCourse)
    }
  }

  /**
   * TODO
   * Replace with [addProblemsWithTopicWithFiles] after [EduExperimentalFeatures.PROBLEMS_BY_TOPIC] feature becomes enabled by default
   * */
  @VisibleForTesting
  fun HyperskillCourse.addProblem(stepId: Int) {
    if (isFeatureEnabled(EduExperimentalFeatures.PROBLEMS_BY_TOPIC)) {
      return addProblemsWithTopic(stepId).onError { error(it) }
    }

    var lesson = getProblemsLesson()
    if (lesson == null) {
      lesson = createProblemsLesson()
    }

    val task = lesson.getTask(stepId)
    if (task == null) {
      lesson.createProblem(stepId)
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

  private fun Lesson.createProblem(stepId: Int): Task {
    val task = computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.problems")) {
      HyperskillConnector.getInstance().getProblems(course, this, listOf(stepId)).firstOrNull()
    } ?: error("Failed to load problem: id = $stepId")
    addTask(task)
    return task
  }

  /**
   * See [com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.getTopicsSection]
   */
  @VisibleForTesting
  fun HyperskillCourse.addProblemsWithTopic(stepId: Int): Result<Unit, String> {
    if (getProblem(stepId) != null) {
      LOG.info("Task with $stepId already exists in the course")
      return Ok(Unit)
    }

    return computeUnderProgress(title = EduCoreBundle.message("hyperskill.problems.loading.code.problems")) {
      var topicsSection = getTopicsSection()
      if (topicsSection == null) {
        topicsSection = createTopicsSection()
      }

      val stepSource = HyperskillConnector.getInstance().getStepSource(stepId).onError {
        return@computeUnderProgress Err(it)
      }
      val topicName = stepSource.getTopicNameForProblem().onError {
        return@computeUnderProgress Err(it)
      }

      var topicLesson = topicsSection.getLesson(topicName)
      if (topicLesson == null) {
        topicLesson = topicsSection.createTopicLesson(topicName)
      }

      addProblemsFromTopic(stepSource, topicLesson).onError {
        return@computeUnderProgress Err(it)
      }
      Ok(Unit)
    }
  }

  private fun HyperskillCourse.addProblemsFromTopic(stepSource: HyperskillStepSource, topicLesson: Lesson): Result<List<Task>, String> {
    val topicId = stepSource.topic ?: return Err("Can't get topic in step source with ${stepSource.id} id")
    val connector = HyperskillConnector.getInstance()
    val stepSources = connector.getRecommendedStepsForTopic(topicId).onError {
      return Err(it)
    }
    val existingTasksIds = topicLesson.items.map { it.id }

    val tasks = connector
      .getTasks(this, topicLesson, stepSources.filter { it.block!!.name in SUPPORTED_STEP_TYPES && it.id !in existingTasksIds })
    tasks.forEach(topicLesson::addTask)
    return Ok(tasks)
  }

  private fun HyperskillCourse.createTopicsSection(): Section {
    val section = Section()
    section.name = HYPERSKILL_TOPICS
    section.index = items.size + 1
    section.course = this
    addSection(section)
    return section
  }

  private fun Section.createTopicLesson(name: String): Lesson {
    val lesson = Lesson()
    lesson.name = name
    lesson.index = this.items.size + 1
    lesson.section = this
    lesson.course = course
    addLesson(lesson)
    return lesson
  }

  private fun HyperskillStepSource.getTopicNameForProblem(): Result<String, String> {
    val theoryId = topicTheory ?: return Err("Can't get theory step id for ${id} step")
    val theorySource = HyperskillConnector.getInstance().getStepSource(theoryId).onError {
      return Err(it)
    }
    val topicName = theorySource.title
    return if (topicName == null) Err("Can't get topic name of ${id} step id") else Ok(topicName)
  }

  /** Method creates legacy problem without their topic. TODO replace with [addProblemsWithTopicWithFiles]
   * after [EduExperimentalFeatures.PROBLEMS_BY_TOPIC] feature is become enabled by default */
  private fun HyperskillCourse.addProblemWithFiles(project: Project, stepId: Int) {
    if (isFeatureEnabled(EduExperimentalFeatures.PROBLEMS_BY_TOPIC)) {
      return addProblemsWithTopicWithFiles(project, stepId).onError { error(it) }
    }

    var problemsLesson = getProblemsLesson()
    var createLessonDir = false
    if (problemsLesson == null) {
      problemsLesson = createProblemsLesson()
      createLessonDir = true
    }

    var task = problemsLesson.getTask(stepId)
    var createTaskDir = false
    if (task == null) {
      task = problemsLesson.createProblem(stepId)
      createTaskDir = true
    }

    problemsLesson.init(course, null, false)

    if (createLessonDir) {
      GeneratorUtils.createLesson(problemsLesson, course.getDir(project.courseDir))
      YamlFormatSynchronizer.saveAll(project)
    }
    else if (createTaskDir) {
      GeneratorUtils.createTask(task, problemsLesson.getDir(project.courseDir)!!)
      YamlFormatSynchronizer.saveItem(problemsLesson)
      YamlFormatSynchronizer.saveItem(task)
      YamlFormatSynchronizer.saveRemoteInfo(task)
    }

    if (createTaskDir) {
      course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    }
  }

  private fun HyperskillCourse.addProblemsWithTopicWithFiles(project: Project, stepId: Int): Result<Unit, String> {
    if (getProblem(stepId) != null) {
      LOG.info("Task with $stepId already exists in the course")
      return Ok(Unit)
    }

    return computeUnderProgress(title = EduCoreBundle.message("hyperskill.problems.loading.code.problems")) {
      var topicsSection = getTopicsSection()
      var createSectionDir = false
      if (topicsSection == null) {
        topicsSection = createTopicsSection()
        createSectionDir = true
      }

      val stepSource = HyperskillConnector.getInstance().getStepSource(stepId).onError {
        return@computeUnderProgress Err(it)
      }
      val topicName = stepSource.getTopicNameForProblem().onError {
        return@computeUnderProgress Err(it)
      }

      var topicLesson = topicsSection.getLesson(topicName)
      var createLessonDir = false
      if (topicLesson == null) {
        topicLesson = topicsSection.createTopicLesson(topicName)
        createLessonDir = true
      }

      val tasks = addProblemsFromTopic(stepSource, topicLesson).onError {
        return@computeUnderProgress Err(it)
      }

      topicsSection.init(course, null, false)

      when {
        createSectionDir -> {
          GeneratorUtils.createSection(topicsSection, course.getDir(project.courseDir))
          tasks.forEach { task ->
            YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
          }
          YamlFormatSynchronizer.saveItem(topicLesson)
          YamlFormatSynchronizer.saveItem(topicsSection)
          YamlFormatSynchronizer.saveItem(course)
        }
        createLessonDir -> {
          val parentDir = topicsSection.getDir(project.courseDir) ?: error("Can't get directory of Topics section")
          GeneratorUtils.createLesson(topicLesson, parentDir)
          tasks.forEach { task ->
            YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
          }
          YamlFormatSynchronizer.saveItem(topicLesson)
          YamlFormatSynchronizer.saveItem(topicsSection)
        }
        else -> {
          tasks.forEach { task ->
            GeneratorUtils.createTask(task, topicLesson.getDir(project.courseDir)!!)
            YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
          }
          YamlFormatSynchronizer.saveItem(topicLesson)
        }
      }

      if (tasks.isNotEmpty()) {
        course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
      }
      Ok(Unit)
    }
  }

  // We have to use visible frame here because project is not yet created
  // See `com.intellij.ide.impl.ProjectUtil.focusProjectWindow` implementation for more details
  fun requestFocus() {
    val frame = WindowManager.getInstance().findVisibleFrame()
    if (frame is IdeFrame) {
      AppIcon.getInstance().requestFocus(frame)
    }
    frame?.toFront()
  }

  private fun synchronizeProjectOnStepOpening(project: Project, course: HyperskillCourse, stepId: Int) {
    if (isUnitTestMode) {
      return
    }

    val task = course.getProblem(stepId) ?: return
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, listOf(task), true)
  }

  private fun synchronizeProjectOnStageOpening(project: Project, course: HyperskillCourse, tasks: List<Task>) {
    if (isUnitTestMode) {
      return
    }
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
    HyperskillStartupActivity.synchronizeTopics(project, course)
  }
}