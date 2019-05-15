package com.jetbrains.edu.coursecreator.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader

class StepikCourseUploader(val project: Project, val course: EduCourse) {

  fun updateCourse() {
    val remoteCourse = StepikConnector.getCourseInfo(course.id) ?: return
    StepikCourseLoader.loadCourseStructure(remoteCourse)
    remoteCourse.init(null, null, false)
    pushChanges(remoteCourse)
  }

  private fun pushChanges(remoteCourse: EduCourse) {
    val changeRetriever = StepikChangeRetriever(project, course, remoteCourse)
    val changedItems = changeRetriever.getChangedItems()

    var pushed = processCourseElement(changedItems)
    pushed = processTopLevelSection(changedItems) || pushed
    pushed = processSections(changedItems) || pushed
    pushed = processLessons(changedItems) || pushed
    pushed = processTasks(changedItems) || pushed

    if (!pushed) {
      EduUtils.showNotification(project, "Nothing to upload", null)
    }
    else {
      course.updateDate = remoteCourse.updateDate
      EduUtils.showNotification(project, "Course is updated", openOnStepikAction("/course/${course.id}"))
    }
  }

  private fun processCourseElement(changedItems: StepikChangesInfo): Boolean {
    var pushed = false
    if (changedItems.isCourseInfoChanged) {
      pushed = updateCourseInfo(project, course)
    }
    if (changedItems.isCourseAdditionalFilesChanged) {
      updateAdditionalMaterials(project, course.id)
      pushed = true
    }
    return pushed
  }

  private fun processTopLevelSection(changedItems: StepikChangesInfo): Boolean {
    var pushed = false
    if (changedItems.isTopLevelSectionNameChanged) {
      pushed = updateSectionForTopLevelLessons(course)
    }
    if (changedItems.isTopLevelSectionRemoved) {
      StepikConnector.deleteSection(course.sectionIds[0])
      course.sectionIds = emptyList()
      pushed = true
    }
    if (changedItems.isTopLevelSectionAdded) {
      postSectionForTopLevelLessons(project, course)
      pushed = true
    }
    return pushed
  }

  private fun processSections(changedItems: StepikChangesInfo): Boolean {
    var pushed = false

    // delete old section
    changedItems.sectionsToDelete.forEach {
      StepikConnector.deleteSection(it.id)
      pushed = true
    }

    // post new section
    changedItems.newSections.forEach {
      it.position = it.index
      postSection(project, it, null)
      pushed = true
    }

    // update section
    for (localSection in changedItems.sectionInfosToUpdate) {
      localSection.position = localSection.index
      updateSectionInfo(localSection)
      pushed = true
    }

    return pushed
  }

  private fun processLessons(changedItems: StepikChangesInfo): Boolean {
    var pushed = false

    // delete old lesson
    changedItems.lessonsToDelete.forEach {
      StepikConnector.deleteLesson(it.id)
      StepikConnector.deleteUnit(it.unitId)
      pushed = true
    }

    // post new lesson
    changedItems.newLessons.forEach {
      val section = it.section?.id ?: course.sectionIds.first()
      postLesson(project, it, it.index, section)
      pushed = true
    }

    // update lesson
    for (localLesson in changedItems.lessonsInfoToUpdate) {
      val section = localLesson.section?.id ?: course.sectionIds.first()
      updateLessonInfo(project, localLesson, false, section)
      pushed = true
    }

    return pushed
  }

  private fun processTasks(changedItems: StepikChangesInfo): Boolean {
    var pushed = false

    // delete old task
    changedItems.tasksToDelete.forEach {
      StepikConnector.deleteTask(it.id)
      pushed = true
    }

    // post new task
    changedItems.newTasks.forEach {
      postTask(project, it, it.lesson.id)
      pushed = true
    }

    // update tasks
    for (localTask in changedItems.tasksToUpdate) {
      updateTask(project, localTask)
      pushed = true
    }

    return pushed
  }
}
