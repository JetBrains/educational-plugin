package com.jetbrains.edu.learning.stepik.api

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask.Companion.NUMBER_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask.Companion.STRING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_TASK_TYPE
import com.jetbrains.edu.learning.invokeAllWithProgress
import com.jetbrains.edu.learning.stepik.*
import java.util.concurrent.Executors

object StepikCourseLoader {
  private val THREAD_NUMBER = Runtime.getRuntime().availableProcessors()
  private val EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_NUMBER)

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
    val tasks = allSections.mapIndexed { index, section ->
      { loadItemTask(remoteCourse, section, index + 1) }
    }

    val sections = invokeAllWithProgress(tasks, EXECUTOR_SERVICE)
    return sections.sortedBy { it.index }
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
    val tasks = ArrayList<Task>()
    for (step in allStepSources) {
      val builder = StepikTaskBuilder(course, lesson, step)

      val type = step.block?.name ?: error("Can't get type from step source")
      if (!builder.isSupported(type)
          // TODO hack until EDU-4744 will be implemented
          || type == DATA_TASK_TYPE
          // TODO hack until EDU-4763 will be implemented
          || type == STRING_TASK_TYPE
          // TODO hack until EDU-4780 will be implemented
          || type == NUMBER_TASK_TYPE
      ) continue

      val task = builder.createTask(type) ?: continue
      tasks.add(task)
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
