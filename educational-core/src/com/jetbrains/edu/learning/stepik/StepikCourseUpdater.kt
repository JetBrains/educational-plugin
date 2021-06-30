package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduCourseUpdater
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader.loadCourseStructure

class StepikCourseUpdater(project: Project, course: EduCourse) : EduCourseUpdater(project, course) {

  override fun setUpdated(courseFromServer: EduCourse) {
    super.setUpdated(courseFromServer)
    course.setUpdated(courseFromServer)
  }

  override fun updateSections(courseFromServer: EduCourse) {
    super.updateSections(courseFromServer)
    copyItemsCustomNames(courseFromServer.sections, course.sections)
  }

  // on Stepik top-level lessons are wrapped with fake sections, which should be skipped,
  // because this lessons will be processed in updateLessons() function
  override fun sectionShouldBeSkipped(sectionId: Int): Boolean {
    return if (course.lessons.isNotEmpty()) {
      sectionId == course.sectionIds[0]
    }
    else false
  }

  override fun updateLessons(courseFromServer: EduCourse) {
    super.updateLessons(courseFromServer)
    processLessonsAfterUpdate(courseFromServer.lessons, course.lessons.associateBy { it.id })
  }

  override fun processLessonsAfterUpdate(lessonsFromServer: List<Lesson>, courseLessonsById: Map<Int, Lesson>) {
    for (lessonFromServer in lessonsFromServer) {
      val courseLesson = courseLessonsById[lessonFromServer.id]
      if (courseLesson != null) {
        copyItemCustomName(lessonFromServer, courseLesson)
        copyItemsCustomNames(lessonFromServer.taskList, courseLesson.taskList)
      }
    }
  }

  private fun copyItemsCustomNames(itemsFromServer: List<StudyItem>, items: List<StudyItem>) {
    val itemsById = items.associateBy { it.id }
    for (itemFromServer in itemsFromServer) {
      val item = itemsById[itemFromServer.id]
      if (item != null) {
        copyItemCustomName(itemFromServer, item)
      }
    }
  }

  @Suppress("DEPRECATION")
  private fun copyItemCustomName(itemFromServer: StudyItem, item: StudyItem) {
    if (item.customPresentableName == null) return
    // if itemFromServer.customPresentableName is not null, that means that it was already set together with correct item name
    // with itemFromServer.name == item.presentableName we are checking that item was not renamed on remote
    if (itemFromServer.customPresentableName == null && itemFromServer.name == item.presentableName) {
      itemFromServer.customPresentableName = item.customPresentableName
      itemFromServer.name = item.name
    }
  }

  override fun taskChanged(newTask: Task, task: Task): Boolean = task.updateDate.before(newTask.updateDate)

  override fun courseFromServer(currentCourse: EduCourse, courseInfo: EduCourse?): EduCourse? {
    val remoteCourse = courseInfo ?: StepikConnector.getInstance().getCourseInfo(currentCourse.id, true)
    if (remoteCourse != null) {
      loadCourseStructure(remoteCourse)
      addTopLevelLessons(remoteCourse)
      return remoteCourse
    }
    return null
  }

  // On Stepik top-level lessons section is named after a course
  // In case it was renamed on stepik, its lessons  won't be parsed as top-level
  // so we need to copy them manually
  private fun addTopLevelLessons(courseFromServer: Course?) {
    if (courseFromServer!!.sections.isNotEmpty() && course.sectionIds.isNotEmpty()) {
      if (courseFromServer.sections[0].id == course.sectionIds[0]) {
        courseFromServer.addLessons(courseFromServer.sections[0].lessons)
      }
    }
  }
}
