package com.jetbrains.edu.coursecreator.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader

class StepikCourseUploader(val project: Project, val course: EduCourse) {

  fun updateCourse() {
    val remoteCourse = StepikConnector.getInstance().getCourseInfo(course.id) ?: return
    StepikCourseLoader.loadCourseStructure(remoteCourse)
    remoteCourse.init(null, null, false)
    pushChanges(remoteCourse)
  }

  private fun pushChanges(remoteCourse: EduCourse) {
    val changeRetriever = StepikChangeRetriever(project, course, remoteCourse)
    val changedItems = changeRetriever.getChangedItems()

    if (changedItems.isEmpty()) {
      EduUtils.showNotification(project, "Nothing to update", null)
      return
    }

    var success = processCourse(changedItems)
    success = processTopLevelSection(changedItems) && success
    success = processSections(changedItems) && success
    success = processLessons(changedItems) && success
    success = processTasks(changedItems) && success

    if (!success) {
      EduUtils.showNotification(project, "Failed to update the course", null)
    }
    else {
      course.updateDate = remoteCourse.updateDate
      EduUtils.showNotification(project, "Course is updated", openOnStepikAction("/course/${course.id}"))
    }
  }

  private fun processCourse(changedItems: StepikChangesInfo): Boolean {
    var success = true
    if (changedItems.isCourseInfoChanged) {
      success = updateCourseInfo(project, course) && success
    }
    if (changedItems.isCourseAdditionalFilesChanged) {
      success = updateAdditionalMaterials(project, course.id) && success
    }
    return success
  }

  private fun processTopLevelSection(changedItems: StepikChangesInfo): Boolean {
    var success = true

    if (changedItems.isTopLevelSectionRemoved) {
      StepikConnector.getInstance().deleteSection(course.sectionIds[0])
      course.sectionIds = emptyList()
    }

    if (changedItems.isTopLevelSectionNameChanged) {
      success = updateSectionForTopLevelLessons(course) && success
    }

    if (changedItems.isTopLevelSectionAdded) {
      val sectionId = postSectionForTopLevelLessons(project, course)
      success = sectionId != -1 && success
    }
    return success
  }

  private fun processSections(changedItems: StepikChangesInfo): Boolean {
    var success = true

    // delete old section
    changedItems.sectionsToDelete.forEach {
      StepikConnector.getInstance().deleteSection(it.id)
    }

    // post new section
    changedItems.newSections.forEach {
      it.position = it.index
      success = postSection(project, it) && success
    }

    // update section
    for (localSection in changedItems.sectionInfosToUpdate) {
      localSection.position = localSection.index
      success = updateSectionInfo(localSection) && success
    }

    return success
  }

  private fun processLessons(changedItems: StepikChangesInfo): Boolean {
    var success = true

    // delete old lesson
    changedItems.lessonsToDelete.forEach {
      StepikConnector.getInstance().deleteLesson(it.id)
      StepikConnector.getInstance().deleteUnit(it.unitId)
    }

    // post new lesson
    changedItems.newLessons.forEach {
      val section = it.section?.id ?: course.sectionIds.first()
      success = postLesson(project, it, it.index, section) && success
    }

    // update lesson
    for (localLesson in changedItems.lessonsInfoToUpdate) {
      val section = localLesson.section?.id ?: course.sectionIds.first()
      val lesson = updateLessonInfo(project, localLesson, false, section)
      success = lesson != null && success
    }

    return success
  }

  private fun processTasks(changedItems: StepikChangesInfo): Boolean {
    var success = true

    // delete old task
    changedItems.tasksToDelete.forEach {
      StepikConnector.getInstance().deleteTask(it.id)
    }

    // post new task
    changedItems.newTasks.forEach {
      success = postTask(project, it, it.lesson.id) && success
    }

    // update tasks
    for (localTask in changedItems.tasksToUpdate) {
      success = updateTask(project, localTask) && success
    }

    return success
  }
}
