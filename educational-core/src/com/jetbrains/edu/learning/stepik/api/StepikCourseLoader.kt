package com.jetbrains.edu.learning.stepik.api

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ConcurrencyUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.*
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.course.stepikCourseFromRemote
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object StepikCourseLoader {
  private val LOG = Logger.getInstance(StepikCourseLoader::class.java)
  private val THREAD_NUMBER = Runtime.getRuntime().availableProcessors()
  private val EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_NUMBER)
  private const val PUBLIC_COURSES_THREADS_NUMBER = 4

  @JvmStatic
  fun getCourseInfos(): List<EduCourse> {
    LOG.info("Loading courses started...")
    val startTime = System.currentTimeMillis()
    val result = mutableListOf<EduCourse>()
    val tasks = mutableListOf<Callable<List<EduCourse>>>()
    tasks.add(Callable { getPublicCourseInfos() })
    tasks.add(Callable { getPrivateCourseInfos() })
    tasks.add(Callable { getListedStepikCourses() })

    ConcurrencyUtil.invokeAll(tasks, EXECUTOR_SERVICE)
      .filterNot { it.isCancelled }
      .mapNotNull { it.get() }
      .forEach { result.addAll(it) }

    setAuthors(result)
    setReviews(result)

    LOG.info("Loading courses finished...Took " + (System.currentTimeMillis() - startTime) + " ms")
    return result
  }

  @JvmStatic
  fun getPrivateCourseInfos(): List<EduCourse> {
    if (EduSettings.getInstance().user == null) {
      return emptyList()
    }
    val result = mutableListOf<EduCourse>()
    var currentPage = 1
    val indicator = ProgressManager.getInstance().progressIndicator
    while (true) {
      if (indicator != null && indicator.isCanceled) break
      val coursesList = StepikConnector.getInstance().getCourses(false, currentPage, true) ?: break

      val availableCourses = getAvailableCourses(coursesList)
      result.addAll(availableCourses)
      currentPage += 1
      if (!coursesList.meta.containsKey("has_next") || coursesList.meta["has_next"] == false) break
    }
    return result
  }

  private fun getPublicCourseInfos(): List<EduCourse> {
    val indicator = ProgressManager.getInstance().progressIndicator
    val tasks = mutableListOf<Callable<List<EduCourse>?>>()
    val minEmptyPageNumber = AtomicInteger(Integer.MAX_VALUE)

    for (i in 0 until PUBLIC_COURSES_THREADS_NUMBER) {
      tasks.add(Callable {
        val courses = mutableListOf<EduCourse>()
        var pageNumber = i + 1
        while (pageNumber < minEmptyPageNumber.get() && addCoursesFromStepik(courses, true, pageNumber, null, minEmptyPageNumber)) {
          if (indicator != null && indicator.isCanceled) {
            return@Callable null
          }
          pageNumber += PUBLIC_COURSES_THREADS_NUMBER
        }
        return@Callable courses
      })
    }

    val result = mutableListOf<EduCourse>()
    ConcurrencyUtil.invokeAll(tasks, EXECUTOR_SERVICE)
      .filterNot { it.isCancelled }
      .mapNotNull { it.get() }
      .forEach { result.addAll(it) }

    return result
  }

  private fun setAuthors(result: List<EduCourse>) {
    val allUsers = StepikConnector.getInstance().getUsers(result)
    val usersById = allUsers.associateBy { it.id }

    for (course in result) {
      val authors = course.instructors.mapNotNull { usersById[it] }
      course.authors = authors
    }
  }

  private fun setReviews(courses: List<EduCourse>) {
    val summaryIds = courses.map { it.reviewSummary }
    val reviewsByCourseId = StepikConnector.getInstance().getCourseReviewSummaries(summaryIds).associateBy { it.courseId }
    for (course in courses) {
      course.reviewScore = reviewsByCourseId[course.id]?.average ?: 0.0
    }
  }

  private fun getListedStepikCourses(): List<StepikCourse> {
    val courses = StepikConnector.getInstance().getCourses(featuredStepikCourses.keys.plus(inProgressCourses)) ?: return emptyList()
    return courses.mapNotNull { course ->
      val courseId = course.id
      featuredStepikCourses[courseId]?.let { course.language = it }
      if (course.compatibility == CourseCompatibility.Unsupported) return@mapNotNull null
      val remoteCourse = stepikCourseFromRemote(course) ?: return@mapNotNull null
      if (inProgressCourses.contains(courseId)) {
        remoteCourse.visibility = CourseVisibility.InProgressVisibility(inProgressCourses.indexOf(courseId))
      }
      remoteCourse
    }.filter { it.compatibility == CourseCompatibility.Compatible }
  }

  @JvmStatic
  fun loadCourseStructure(remoteCourse: EduCourse) {
    if (remoteCourse.items.isNotEmpty()) return
    fillItems(remoteCourse)
  }

  @JvmStatic
  fun fillItems(remoteCourse: EduCourse) {
    val sectionIds = remoteCourse.sectionIds
    val allSections = StepikConnector.getInstance().getSections(sectionIds)

    val realSections = allSections.filter { it.name != StepikNames.PYCHARM_ADDITIONAL }  // compatibility with old courses
    if (hasVisibleSections(realSections, remoteCourse.name)) {
      remoteCourse.sectionIds = emptyList()
      val sections = getOrderedListOfSections(realSections, remoteCourse)
      val items = unpackTopLevelLessons(remoteCourse, sections)
      items.forEachIndexed { index, item -> item.index = index + 1 }
      remoteCourse.items = items
    }
    else {
      addTopLevelLessons(remoteCourse, realSections)
    }
    fillAdditionalMaterials(remoteCourse, allSections.firstOrNull { it.name == StepikNames.PYCHARM_ADDITIONAL })
  }

  private fun fillAdditionalMaterials(course: EduCourse, additionalSection: Section?) {
    loadAndFillAdditionalCourseInfo(course)
    if (course.additionalFiles.isEmpty() && additionalSection != null) {
      // load the old way for compatibility with old courses
      if (additionalSection.units.size == 1) {
        val lesson = getLessonsFromUnits(course, additionalSection.units, false).firstOrNull()
        if (lesson != null) {
          val task = lesson.taskList.firstOrNull()
          if (task != null) {
            course.additionalFiles = task.taskFiles.values.toList()
          }
        }
      }
    }
  }

  private fun addTopLevelLessons(remoteCourse: EduCourse, allSections: List<Section>) {
    val unitIds = allSections.flatMap { section -> section.units }.distinct()
    if (unitIds.isNotEmpty()) {
      val lessons = getLessonsFromUnits(remoteCourse, unitIds, true)
      remoteCourse.addLessons(lessons)
      lessons.forEach { loadAndFillLessonAdditionalInfo(it) }
      remoteCourse.sectionIds = allSections.map { s -> s.id }
    }
  }

  fun getUnitsIds(remoteCourse: EduCourse): List<Int> {
    val sections = StepikConnector.getInstance().getSections(remoteCourse.sectionIds)
    return sections.flatMap { section -> section.units }.distinct()
  }

  private fun getOrderedListOfSections(allSections: List<Section>, remoteCourse: EduCourse): List<StudyItem> {
    val loadItemTasks = mutableListOf<Callable<StudyItem?>>()
    for ((index, section) in allSections.withIndex()) {
      loadItemTasks.add(Callable { loadItemTask(remoteCourse, section, index + 1) })
    }
    val sections = ArrayList<StudyItem>()
    ConcurrencyUtil.invokeAll(loadItemTasks, EXECUTOR_SERVICE)
      .filterNot { it.isCancelled }
      .mapNotNull { it.get() }
      .forEach { sections.add(it) }

    sections.sortBy { it.index }
    return sections
  }

  private fun hasVisibleSections(sections: List<Section>, courseName: String): Boolean {
    if (sections.isEmpty()) {
      return false
    }

    val firstSectionTitle = sections.first().name
    if (sections.size == 1 && firstSectionTitle == courseName) {
      return false
    }
    return true
  }

  private fun loadItemTask(remoteCourse: EduCourse, section: Section, index: Int): StudyItem? {
    val unitIds = section.units
    if (unitIds.size <= 0) {
      return null
    }
    val lessonsFromUnits = getLessonsFromUnits(remoteCourse, unitIds, false)

    lessonsFromUnits.forEachIndexed { i, lesson -> lesson.index = i + 1 }
    section.addLessons(lessonsFromUnits)
    section.index = index
    return section
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
        progressIndicator.isIndeterminate = false
        val readableIndex = lessonIndex + 1
        progressIndicator.checkCanceled()
        progressIndicator.text = "Loading lesson $readableIndex of $lessonCount"
        progressIndicator.fraction = readableIndex.toDouble() / lessonCount
      }
      val allStepSources = StepikConnector.getInstance().getStepSources(lesson.steps)

      if (allStepSources.isNotEmpty()) {
        val options = allStepSources[0].block!!.options
        if (options is PyCharmStepOptions && options.lessonType != null) {
          // TODO: find a better way to get framework lessons from stepik
          lesson = FrameworkLesson(lesson)
        }
      }
      val tasks = getTasks(remoteCourse, lesson, allStepSources)
      for (task in tasks) {
        lesson.addTask(task)
      }
      result.add(lesson)
    }

    return result
  }

  fun getLessonsFromUnitIds(unitIds: List<Int>): List<Lesson> {
    val units = StepikConnector.getInstance().getUnits(unitIds)
    val lessonIds = units.map { unit -> unit.lesson }
    val lessons = StepikConnector.getInstance().getLessons(lessonIds)

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
  private fun sortLessonsByUnits(units: List<StepikUnit>, lessons: List<Lesson>): List<Lesson> {
    val idToLesson = lessons.associateBy { it.id }
    return units.sortedBy { unit -> unit.section }.mapNotNull { idToLesson[it.lesson] }
  }

  fun getTasks(course: Course, lesson: Lesson, allStepSources: List<StepSource>): List<Task> {
    val user = EduSettings.getInstance().user
    val tasks = ArrayList<Task>()
    for (step in allStepSources) {
      val builder = StepikTaskBuilder(course, lesson, step, step.id, user?.id ?: -1)
      if (!builder.isSupported(step.block!!.name)) continue
      val task = builder.createTask(step.block!!.name)
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
        item.lessons.forEach { loadAndFillLessonAdditionalInfo(it) }
      }
      else {
        itemsWithTopLevelLessons.add(item)
      }
    }
    return itemsWithTopLevelLessons
  }
}
