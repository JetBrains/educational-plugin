package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequestHandler
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder.StepikTaskType.TEXT
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.Companion.SUPPORTED_STEP_TYPES
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

object HyperskillOpenInIdeRequestHandler : OpenInIdeRequestHandler<HyperskillOpenRequest>() {
  private val LOG = Logger.getInstance(HyperskillOpenInIdeRequestHandler::class.java)
  override val courseLoadingProcessTitle: String get() = EduCoreBundle.message("hyperskill.loading.project")

  override fun openInExistingProject(request: HyperskillOpenRequest,
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
          GeneratorUtils.createLesson(project, projectLesson, courseDir)
          GeneratorUtils.createAdditionalFiles(project, course, courseDir)
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
                                  request: HyperskillOpenRequest): Pair<Project, Course>? {
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

  fun createHyperskillCourse(request: HyperskillOpenRequest,
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

    if (eduEnvironment == EduNames.ANDROID) {
      if (request is HyperskillOpenStepRequest) {
        return Ok(HyperskillCourse(hyperskillLanguage, eduLanguage))
      }
      else if (!EduUtils.isAndroidStudio()) {
        return Err(EduCoreBundle.message("rest.service.android.not.supported"))
      }
    }

    return Ok(HyperskillCourse(hyperskillProject, eduLanguage, eduEnvironment))
  }

  override fun getCourse(request: HyperskillOpenRequest, indicator: ProgressIndicator): Result<Course, String> {
    val hyperskillProject = HyperskillConnector.getInstance().getProject(request.projectId).onError {
      return Err(it)
    }

    val hyperskillLanguage = if (request is HyperskillOpenStepRequest) request.language else hyperskillProject.language

    val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, hyperskillProject).onError {
      return Err(it)
    }

    hyperskillCourse.validateLanguage(hyperskillLanguage).onError { return Err(it) }

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
    return Ok(hyperskillCourse)
  }

  /**
   * TODO
   * Replace with [addProblemsWithTopicWithFiles] after [EduExperimentalFeatures.PROBLEMS_BY_TOPIC] feature becomes enabled by default
   * */
  @VisibleForTesting
  fun HyperskillCourse.addProblem(stepId: Int) {
    if (getProblem(stepId) != null) {
      LOG.info("Task with $stepId already exists in the course")
      return
    }

    val connector = HyperskillConnector.getInstance()
    val stepSource = connector.getStepSource(stepId).onError { error(it) }

    if (stepSource.canBeAddedWithTopic()) {
      return addProblemsWithTopic(stepSource).onError { error(it) }
    }

    var lesson = getProblemsLesson()
    if (lesson == null) {
      lesson = createProblemsLesson()
    }

    val task = lesson.getTask(stepId)
    if (task == null) {
      lesson.addProblem(stepSource)
    }
  }

  private fun HyperskillStepSource.canBeAddedWithTopic(): Boolean {
    return topic != null && isFeatureEnabled(EduExperimentalFeatures.PROBLEMS_BY_TOPIC)
  }

  private fun HyperskillCourse.createProblemsLesson(): Lesson {
    val lesson = Lesson()
    lesson.name = HYPERSKILL_PROBLEMS
    lesson.index = this.items.size + 1
    lesson.course = this
    addLesson(lesson)
    return lesson
  }

  /**
   * See [com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.getTopicsSection]
   */
  @VisibleForTesting
  fun HyperskillCourse.addProblemsWithTopic(stepSource: HyperskillStepSource): Result<Unit, String> {
    return computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.problems")) {
      var topicsSection = getTopicsSection()
      if (topicsSection == null) {
        topicsSection = createTopicsSection()
      }

      val (topicName, stepSources) = stepSource.getTopicAndRelatedSteps().onError {
        return@computeUnderProgress Err(it)
      }

      var topicLesson = topicsSection.getLesson { it.presentableName == topicName }
      if (topicLesson == null) {
        topicLesson = topicsSection.createTopicLesson(topicName)
      }

      topicLesson.addProblems(stepSources).onError {
        return@computeUnderProgress Err(it)
      }
      Ok(Unit)
    }
  }

  private fun Lesson.addProblem(stepSource: HyperskillStepSource): Result<Task, String> {
    val problems = addProblems(listOf(stepSource)).onError {
      return Err(it)
    }
    if (problems.isEmpty()) {
      return Err("Problem has not been added")
    }
    return Ok(problems.first())
  }

  private fun Lesson.addProblems(stepSources: List<HyperskillStepSource>): Result<List<Task>, String> {
    val existingTasksIds = items.map { it.id }
    val filteredStepSources = stepSources.filter { it.block!!.name in SUPPORTED_STEP_TYPES && it.id !in existingTasksIds }

    val tasks = HyperskillConnector.getTasks(course, this, filteredStepSources)
    tasks.forEach(this::addTask)
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

  private fun HyperskillStepSource.getTopicAndRelatedSteps(): Result<Pair<String, List<HyperskillStepSource>>, String> {
    val connector = HyperskillConnector.getInstance()
    val topicId = topic ?: return Err("Topic must not be null")

    val stepSources = connector.getStepsForTopic(topicId)
      .onError { return Err(it) }
      .filter { it.isRecommended || it.id == id }

    val theoryTitle = stepSources.find { it.block?.name == TEXT.type }?.title
    if (theoryTitle != null) {
      return Ok(Pair(theoryTitle, stepSources))
    }

    LOG.warn("Can't get theory step title for ${id} step")
    val problemTitle = title ?: return Err("Can't get title of ${id} step")
    return Ok(Pair(problemTitle, stepSources))
  }

  /** Method creates legacy problem without their topic. TODO replace with [addProblemsWithTopicWithFiles]
   * after [EduExperimentalFeatures.PROBLEMS_BY_TOPIC] feature is become enabled by default */
  private fun HyperskillCourse.addProblemWithFiles(project: Project, stepId: Int) {
    if (getProblem(stepId) != null) {
      LOG.info("Task with $stepId already exists in the course")
      return
    }

    val connector = HyperskillConnector.getInstance()
    val stepSource = connector.getStepSource(stepId).onError {
      error(it)
    }

    if (stepSource.canBeAddedWithTopic()) {
      return addProblemsWithTopicWithFiles(project, stepSource).onError { error(it) }
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
      task = problemsLesson.addProblem(stepSource).onError { error(it) }
      createTaskDir = true
    }

    problemsLesson.init(course, null, false)

    if (createLessonDir) {
      GeneratorUtils.createLesson(project, problemsLesson, course.getDir(project.courseDir))
      YamlFormatSynchronizer.saveAll(project)
    }
    else if (createTaskDir) {
      GeneratorUtils.createTask(project, task, problemsLesson.getDir(project.courseDir)!!)
      YamlFormatSynchronizer.saveItem(problemsLesson)
      YamlFormatSynchronizer.saveItem(task)
      YamlFormatSynchronizer.saveRemoteInfo(task)
    }

    if (createTaskDir) {
      course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    }
  }

  private fun HyperskillCourse.addProblemsWithTopicWithFiles(project: Project, stepSource: HyperskillStepSource): Result<Unit, String> {
    return computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.problems")) {
      var localTopicsSection = getTopicsSection()
      val createSectionDir = localTopicsSection == null
      if (localTopicsSection == null) {
        localTopicsSection = createTopicsSection()
      }

      val (topicNameSource, stepSources) = stepSource.getTopicAndRelatedSteps().onError { return@computeUnderProgress Err(it) }
      var localTopicLesson = localTopicsSection.getLesson { it.presentableName == topicNameSource }
      val createLessonDir = localTopicLesson == null
      if (localTopicLesson == null) {
        localTopicLesson = localTopicsSection.createTopicLesson(topicNameSource)
      }

      val tasks = localTopicLesson.addProblems(stepSources).onError { return@computeUnderProgress Err(it) }
      localTopicsSection.init(course, null, false)

      when {
        createSectionDir -> saveSectionDir(project, course, localTopicsSection, localTopicLesson, tasks)
        createLessonDir -> saveLessonDir(project, localTopicsSection, localTopicLesson, tasks)
        else -> saveTasks(project, localTopicLesson, tasks)
      }

      if (tasks.isNotEmpty()) {
        course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
      }
      Ok(Unit)
    }
  }

  private fun saveSectionDir(
    project: Project,
    course: Course,
    topicsSection: Section,
    topicLesson: Lesson,
    tasks: List<Task>
  ) {
    GeneratorUtils.createSection(project, topicsSection, course.getDir(project.courseDir))
    tasks.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) }
    YamlFormatSynchronizer.saveItem(topicLesson)
    YamlFormatSynchronizer.saveItem(topicsSection)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun saveLessonDir(
    project: Project,
    topicSection: Section,
    topicLesson: Lesson,
    tasks: List<Task>
  ) {
    val parentDir = topicSection.getDir(project.courseDir) ?: error("Can't get directory of Topics section")
    tasks.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) }
    GeneratorUtils.createLesson(project, topicLesson, parentDir)
    YamlFormatSynchronizer.saveItem(topicLesson)
    YamlFormatSynchronizer.saveItem(topicSection)
  }

  private fun saveTasks(
    project: Project,
    topicLesson: Lesson,
    tasks: List<Task>,
  ) {
    tasks.forEach { task ->
      topicLesson.getDir(project.courseDir)?.let { lessonDir ->
        GeneratorUtils.createTask(project, task, lessonDir)
        YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
      }
    }
    YamlFormatSynchronizer.saveItem(topicLesson)
  }

  private fun synchronizeProjectOnStepOpening(project: Project, course: HyperskillCourse, stepId: Int) {
    if (isUnitTestMode) {
      return
    }

    val task = course.getProblem(stepId) ?: return
    if (isFeatureEnabled(EduExperimentalFeatures.PROBLEMS_BY_TOPIC)) {
      val tasks = task.lesson.taskList.filter { it.itemType in SUPPORTED_STEP_TYPES }
      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
    }
    else {
      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, listOf(task), true)
    }
  }

  private fun synchronizeProjectOnStageOpening(project: Project, course: HyperskillCourse, tasks: List<Task>) {
    if (isUnitTestMode) {
      return
    }
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
    HyperskillStartupActivity.synchronizeTopics(project, course)
  }
}