package com.jetbrains.edu.learning.stepik.api

import com.google.common.annotations.VisibleForTesting
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

object StepikCourseLoader {
  private val LOG = Logger.getInstance(StepikCourseLoader ::class.java)
  private val THREAD_NUMBER = Runtime.getRuntime().availableProcessors()
  private val EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_NUMBER)

  fun getLessons(remoteCourse: EduCourse, sectionId: Int): List<Lesson> {
    val section = StepikNewConnector.getSection(sectionId) ?: return emptyList()
    return getLessonsFromUnits(remoteCourse, section.units, true)
  }

  fun getCourseInfos(): List<EduCourse> {
    LOG.info("Loading courses started...")
    val startTime = System.currentTimeMillis()
    val result = ContainerUtil.newArrayList<EduCourse>()
    val tasks = ContainerUtil.newArrayList<Callable<List<EduCourse>>>()
    tasks.add(Callable { getCourseInfos(true) })
    tasks.add(Callable { getCourseInfos(false) })
    tasks.add(Callable { getInProgressCourses() })

    ConcurrencyUtil.invokeAll(tasks, EXECUTOR_SERVICE)
      .filterNot { it.isCancelled }
      .mapNotNull { it.get() }
      .forEach { result.addAll(it) }

    setAuthors(result)

    LOG.info("Loading courses finished...Took " + (System.currentTimeMillis() - startTime) + " ms")
    return result
  }

  private fun setAuthors(result: List<EduCourse>) {
    val allUsers = StepikMultipleRequestsConnector.getUsers(result)
    val usersById = allUsers.associateBy { it.id }

    for (course in result) {
      val authors = course.instructors.mapNotNull { usersById[it] }
      course.authors = authors
    }
  }

  private fun getCourseInfos(isPublic: Boolean): List<EduCourse> {
    val result = mutableListOf<EduCourse>()
    var currentPage = 1
    val enrolled = if (isPublic) null else true
    val indicator = ProgressManager.getInstance().progressIndicator
    while (true) {
      if (indicator != null && indicator.isCanceled) break
      val coursesList = StepikNewConnector.getCourses(isPublic, currentPage, enrolled)
      if (coursesList == null) break

      val availableCourses = getAvailableCourses(coursesList)
      result.addAll(availableCourses)
      currentPage += 1
      if (!coursesList.meta.containsKey("has_next") || coursesList.meta["has_next"] == false) break
    }
    return result
  }

  private fun getInProgressCourses(): List<EduCourse> {
    val result = ContainerUtil.newArrayList<EduCourse>()
    for (courseId in inProgressCourses) {
      val info = StepikNewConnector.getCourseInfo(courseId, false) ?: continue
      val compatibility = info.compatibility
      if (compatibility === CourseCompatibility.UNSUPPORTED) continue
      val visibility = CourseVisibility.InProgressVisibility(inProgressCourses.indexOf(info.id))
      info.visibility = visibility
      result.add(info)
    }
    return result
  }

  fun loadCourseStructure(remoteCourse: EduCourse) {
    if (!remoteCourse.items.isEmpty()) return
    fillItems(remoteCourse)
  }

  fun fillItems(remoteCourse: EduCourse) {
    val sectionIds = remoteCourse.sectionIds
    val allSections = StepikMultipleRequestsConnector.getSections(sectionIds)

    if (hasVisibleSections(allSections, remoteCourse.name)) {
      remoteCourse.sectionIds = emptyList()
      val sections = getOrderedListOfSections(allSections, remoteCourse)
      val items = unpackTopLevelLessons(remoteCourse, sections)
      items.forEachIndexed { index, item ->
        item.index = index + 1
      }
      remoteCourse.items = items
    }
    else {
      addTopLevelLessons(remoteCourse, allSections)
    }
  }

  private fun addTopLevelLessons(remoteCourse: EduCourse, allSections: List<Section>) {
    val unitIds = allSections.map { section -> section.units }
    if (unitIds.isNotEmpty()) {
      val lessons = getLessons(remoteCourse)
      remoteCourse.addLessons(lessons)
      remoteCourse.sectionIds = allSections.map { s -> s.id }
      lessons.filter { lesson -> lesson.isAdditional }.forEach { lesson ->
        remoteCourse.additionalMaterialsUpdateDate = lesson.updateDate
      }
    }
  }

  private fun getLessons(remoteCourse: EduCourse): List<Lesson> {
    val unitIds = getUnitsIds(remoteCourse)
    return if (unitIds.isNotEmpty()) {
      getLessonsFromUnits(remoteCourse, unitIds, true)
    }
    else emptyList()
  }

  fun getUnitsIds(remoteCourse: EduCourse): List<Int> {
    val sections = StepikMultipleRequestsConnector.getSections(remoteCourse.sectionIds)
    return sections.flatMap { section -> section.units }.distinct()
  }

  private fun getOrderedListOfSections(allSections: List<Section>, remoteCourse: EduCourse): List<StudyItem> {
    val loadItemTasks = ContainerUtil.newArrayList<Callable<StudyItem?>>()
    for ((index, section) in allSections.withIndex()) {
      loadItemTasks.add(Callable { loadItemTask(remoteCourse, section, index + 1) })
    }
    val sections = ArrayList<StudyItem>()
    ConcurrencyUtil.invokeAll(loadItemTasks, EXECUTOR_SERVICE)
      .filterNot { it.isCancelled }
      .mapNotNull { it.get() }
      .forEach { sections.add(it.index - 1, it) }

    return sections
  }

  private fun hasVisibleSections(sections: List<Section>, courseName: String): Boolean {
    if (sections.isEmpty()) {
      return false
    }
    val firstSectionTitle = sections.first().name
    if (firstSectionTitle == null || firstSectionTitle != courseName) return true

    if (sections.size == 1) {
      return false
    }
    val secondSectionTitle = sections[1].name
    return !(sections.size == 2 &&
             (secondSectionTitle == EduNames.ADDITIONAL_MATERIALS || secondSectionTitle == StepikNames.PYCHARM_ADDITIONAL))
  }

  private fun loadItemTask(remoteCourse: EduCourse, section: Section, index: Int): StudyItem? {
    val unitIds = section.units
    if (unitIds.size <= 0) {
      return null
    }
    val lessonsFromUnits = getLessonsFromUnits(remoteCourse, unitIds, false)
    if (section.name == StepikNames.PYCHARM_ADDITIONAL) {
      val lesson = lessonsFromUnits.first()
      lesson.index = index
      remoteCourse.additionalMaterialsUpdateDate = lesson.updateDate
      return lesson
    }
    else {
      lessonsFromUnits.forEachIndexed{ i, lesson ->
        lesson.index = i + 1
      }
      section.addLessons(lessonsFromUnits)
      section.index = index
      return section
    }
  }

  @VisibleForTesting
  fun getLessonsFromUnits(remoteCourse: EduCourse, unitIds: List<Int>, updateIndicator: Boolean): List<Lesson> {
    val progressIndicator = ProgressManager.getInstance().progressIndicator
    val result = mutableListOf<Lesson>()
    val lessonsFromUnits = getLessonsFromUnitIds(unitIds)

    val lessonCount = lessonsFromUnits.size
    for (lessonIndex in 0 until lessonCount) {
      var lesson = lessonsFromUnits[lessonIndex]
      lesson.unitId = unitIds[lessonIndex]
      if (progressIndicator != null && updateIndicator) {
        val readableIndex = lessonIndex + 1
        progressIndicator.checkCanceled()
        progressIndicator.text = "Loading lesson $readableIndex from $lessonCount"
        progressIndicator.fraction = readableIndex.toDouble() / lessonCount
      }
      val allStepSources = StepikMultipleRequestsConnector.getStepSources(lesson.steps, remoteCourse.languageID)

      if (!allStepSources.isEmpty()) {
        val options = allStepSources[0].block.options
        if (options?.lessonType != null) {
          // TODO: find a better way to get framework lessons from stepik
          lesson = FrameworkLesson(lesson)
        }
      }
      val tasks = getTasks(remoteCourse.languageById!!, lesson, allStepSources)
      lesson.taskList.addAll(tasks)
      result.add(lesson)
    }

    return result
  }

  fun getLessonsFromUnitIds(unitIds: List<Int>): List<Lesson> {
    val units = StepikMultipleRequestsConnector.getUnits(unitIds)
    val lessonIds = units.map { unit -> unit.lesson }
    val lessons = StepikMultipleRequestsConnector.getLessons(lessonIds)

    for ((i, lesson) in lessons.withIndex()) {
      val unit = units[i]
      if (!lesson.updateDate.isSignificantlyAfter(unit.updateDate)) {
        lesson.updateDate = unit.updateDate
      }
    }

    return sortLessonsByUnits(units, lessons)
  }

  /**
   * Stepik sorts result of multiple requests by id, but in some cases unit-wise and lessonId-wise order differ.
   * So we need to sort lesson by units to keep correct course structure
   */
  private fun sortLessonsByUnits(units: List<StepikWrappers.Unit>, lessons: List<Lesson>): List<Lesson> {
    val idToLesson = lessons.associateBy { it.id }
    return units.sortedBy { unit -> unit.section }.mapNotNull { idToLesson[it.lesson] }
  }

  fun getTasks(language: Language, lesson: Lesson, allStepSources: List<StepikSteps.StepSource>): List<Task> {
    val user = EduSettings.getInstance().user
    val tasks = ArrayList<Task>()
    for (step in allStepSources) {
      val builder = StepikTaskBuilder(language, lesson, step, step.id, user?.id ?: -1)
      if (!builder.isSupported(step.block.name)) continue
      val task = builder.createTask(step.block.name)
      if (task != null) {
        tasks.add(task)
      }
    }
    return tasks
  }

  private fun unpackTopLevelLessons(remoteCourse: EduCourse, sections: List<StudyItem>): ArrayList<StudyItem> {
    val itemsWithTopLevelLessons = ArrayList<StudyItem>()
    for (item in sections) {
      if (item is Section && item.getName() == remoteCourse.name) {
        remoteCourse.sectionIds = listOf(item.getId())
        itemsWithTopLevelLessons.addAll(item.lessons)
      }
      else {
        itemsWithTopLevelLessons.add(item)
      }
    }
    return itemsWithTopLevelLessons
  }
}
