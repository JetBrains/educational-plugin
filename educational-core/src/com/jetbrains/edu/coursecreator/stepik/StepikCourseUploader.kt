package com.jetbrains.edu.coursecreator.stepik

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikConnector
import com.jetbrains.edu.learning.stepik.StepikNames
import java.util.*
import kotlin.collections.ArrayList

class StepikCourseUploader(val project: Project, val course: RemoteCourse) {
  private var courseInfoToUpdate = false

  private var sectionsToPush: MutableList<Section> = ArrayList()
  private var sectionsToDelete: MutableList<Int> = ArrayList()
  private var sectionsInfoToUpdate: MutableList<Section> = ArrayList()

  private val lessonsToPush: MutableList<Lesson> = ArrayList()
  private var lessonsToDelete: MutableList<Int> = ArrayList()
  private var lessonsInfoToUpdate: MutableList<Lesson> = ArrayList()
  private var lessonsToMove: MutableList<Lesson> = ArrayList()

  private val tasksToPush: MutableList<Task> = ArrayList()
  private var tasksToDelete: MutableList<Int> = ArrayList()
  private var tasksToUpdate: MutableList<Task> = ArrayList()

  fun updateCourse() {
    val lastUpdateDate = course.lastUpdateDate()

    processCourseChanges(lastUpdateDate)
    processSectionChanges(lastUpdateDate)
    processLessonChanges(lastUpdateDate)
    processTaskChanges()

    if (isUpToDate()) {
      val notification = Notification("upload.course", "Nothing to upload", "All course items is up to date", NotificationType.INFORMATION)
      notification.notify(project)
    }
    else {
      pushChanges()
      course.setUpdated()

      // TODO: after merging changes about isUpToDateExtension, inline this in course#setUpdated
      // fix for the case when we deleted section that was changed the last
      course.updateDate = lastUpdateDate
      course.setStatusRecursively(StepikChangeStatus.UP_TO_DATE)
      showNotification(project, "Course is updated", openOnStepikAction("/course/" + course.id))
    }
  }

  private fun pushChanges() {
    if (courseInfoToUpdate) {
      updateCourseInfo(project, course)
    }

    updateSections()
    updateLessons()
    updateTasks()
    updateAdditionalMaterials(project, course.id)
  }

  private fun updateTasks() {
    tasksToPush.forEach {
      postTask(project, it, it.lesson.id)
    }

    tasksToDelete.forEach {
      deleteTask(it)
    }

    tasksToUpdate.forEach {
      updateTask(project, it)
    }
  }

  private fun updateLessons() {
    lessonsToPush.forEach {
      val sectionId: Int = sectionId(it)

      val posted = postLessonInfo(project, it, sectionId, it.index)
      it.id = posted.id
      it.unitId = posted.unitId
      for (task in it.taskList) {
        postTask(project, task, it.id)
      }
    }

    lessonsToMove.forEach {
      val sectionId = if (it.section == null) getTopLevelSectionId(project, course) else it.section!!.id
      deleteUnit(it.unitId)
      it.unitId = postUnit(it.id, it.index, sectionId, project)
    }

    lessonsToDelete.forEach {
      deleteLesson(it)
    }

    lessonsInfoToUpdate.forEach {
      updateLessonInfo(project, it, false, sectionId(it))
    }
  }

  private fun sectionId(it: Lesson): Int {
    return if (it.section != null) {
      it.section!!.id
    }
    else {
      val topLevelSectionId = getTopLevelSectionId(project, course)
      if (topLevelSectionId == -1) {
        val sectionId = postSectionForTopLevelLessons(project, course)
        course.sectionIds.add(sectionId)
        sectionId
      }
      else {
        topLevelSectionId
      }
    }
  }

  private fun updateSections() {
    sectionsToPush.forEach {
      it.position = it.index
      val sectionId = postSectionInfo(project, copySection(it), course.id)
      it.id = sectionId
    }

    sectionsToDelete.forEach {
      deleteSection(it)
    }

    sectionsInfoToUpdate.forEach {
      updateSectionInfo(project, it)
    }
  }

  private fun processCourseChanges(lastUpdateDate: Date) {
    when (course.stepikChangeStatus) {
      StepikChangeStatus.INFO -> {
        courseInfoToUpdate = true
      }

      StepikChangeStatus.CONTENT -> {
        processCourseContentChanged(lastUpdateDate)
      }

      StepikChangeStatus.INFO_AND_CONTENT -> {
        courseInfoToUpdate = true
        processCourseContentChanged(lastUpdateDate)
      }
      StepikChangeStatus.UP_TO_DATE -> {
        // do nothing
      }
    }
  }

  private fun processSectionChanges(lastUpdateDate: Date) {
    val pushCandidates = course.sections.filter { it.stepikChangeStatus != StepikChangeStatus.UP_TO_DATE }
    val sectionsFromStepik = StepikConnector.getSections(
      pushCandidates.map { it.id }.filter { it != 0 }.map { it.toString() }.toTypedArray())

    val deleteCandidates = ArrayList<Int>()
    for ((section, sectionFromServer) in pushCandidates.zip(sectionsFromStepik)) {
      when (section.stepikChangeStatus) {
        StepikChangeStatus.INFO -> {
          sectionsInfoToUpdate.add(section)
        }

        StepikChangeStatus.CONTENT -> {
          processSectionContentChanged(section, deleteCandidates, sectionFromServer)
        }

        StepikChangeStatus.INFO_AND_CONTENT -> {
          sectionsInfoToUpdate.add(section)
          processSectionContentChanged(section, deleteCandidates, sectionFromServer)
        }
        else -> {
          // do nothing
        }
      }
    }

    lessonsToDelete.addAll(StepikConnector.getUnits(
      deleteCandidates.map { it.toString() }.toTypedArray()).filter { it.updateDate <= lastUpdateDate }.map { it.id })
  }

  private fun processSectionContentChanged(section: Section,
                                           deleteCandidates: ArrayList<Int>,
                                           sectionFromServer: Section) {
    lessonsToPush.addAll(section.lessons.filter { it.id == 0 })
    val lessonFromServerIds = sectionFromServer.units
    lessonsToMove.addAll(section.lessons.filter { it.id > 0 }.filter { it.unitId !in lessonFromServerIds })

    val localSectionUnits = section.lessons.map { it.unitId }
    val allLocalUnits = course.allLessons().map { it.unitId }
    deleteCandidates.addAll(sectionFromServer.units.filter {
      val isMoved = it in allLocalUnits
      it !in localSectionUnits && !isMoved
    })
  }

  private fun processLessonChanges(lastUpdateDate: Date) {
    val pushCandidates = course.allLessons().filter { it.stepikChangeStatus != StepikChangeStatus.UP_TO_DATE }
    val lessonsFromStepik = StepikConnector.getLessonsFromUnits(course, pushCandidates.map { it.unitId.toString() }.toTypedArray(), false)

    val deleteCandidates = ArrayList<Int>()
    val allSteps = course.allLessons().flatMap { it.taskList }.map { it.stepId }
    for ((lesson, lessonFromServer) in pushCandidates.zip(lessonsFromStepik)) {
      when (lesson.stepikChangeStatus) {
        StepikChangeStatus.INFO -> {
          lessonsInfoToUpdate.add(lesson)
        }

        StepikChangeStatus.CONTENT -> {
          processLessonContentChanged(lesson, lessonFromServer, allSteps, deleteCandidates, lastUpdateDate)
        }

        StepikChangeStatus.INFO_AND_CONTENT -> {
          lessonsInfoToUpdate.add(lesson)
          processLessonContentChanged(lesson, lessonFromServer, allSteps, deleteCandidates, lastUpdateDate)
        }
        else -> {
          // do nothing
        }
      }
    }

    lessonsToDelete.addAll(StepikConnector.getUnits(
      deleteCandidates.map { it.toString() }.toTypedArray()).filter { it.updateDate <= lastUpdateDate }.map { it.id })
  }

  private fun processLessonContentChanged(lesson: Lesson,
                                          lessonFromServer: Lesson,
                                          allSteps: List<Int>,
                                          deleteCandidates: ArrayList<Int>,
                                          lastUpdateDate: Date) {
    tasksToPush.addAll(lesson.taskList.filter { it.stepId == 0 })

    val localSteps = lesson.taskList.map { it.stepId }
    for (step in lessonFromServer.steps) {
      val isMoved = step in allSteps
      if (step !in localSteps && !isMoved) {
        deleteCandidates.add(step)
      }
    }

    val stringIds = deleteCandidates.map { it.toString() }.toTypedArray()
    val stepSources = StepikConnector.getStepSources(stringIds)
    val tasksFromStep = StepikConnector.getTasks(course, stringIds, stepSources)
    tasksToDelete.addAll(tasksFromStep.filter { it.updateDate <= lastUpdateDate }.map { it.stepId })
  }

  private fun processTaskChanges() {
    val allTasks = course.allLessons().flatMap { it.taskList }
    tasksToUpdate.addAll(allTasks.filter { it.stepikChangeStatus != StepikChangeStatus.UP_TO_DATE })
  }

  private fun processCourseContentChanged(lastUpdateDate: Date) {
    val courseInfo = getCourseInfo(course.id.toString())!!
    val allLessons = course.allLessons().map { it.id }
    val hasTopLevelLessons = !course.lessons.isEmpty()
    if (hasTopLevelLessons) {
      lessonsToPush.addAll(course.lessons.filter { it.id == 0 })
      // process lessons moved to top-level

      val section = StepikConnector.getSection(courseInfo.sectionIds[0])
      val lessonsFromSection = StepikConnector.getLessonsFromUnits(courseInfo, section.units.map { it.toString() }.toTypedArray(), false)
      val topLevelLessonsIds = course.lessons.map { it.id }
      for (lesson in lessonsFromSection) {
        if (lesson.id !in topLevelLessonsIds) {
          val isMoved = lesson.id in allLessons
          if (!isMoved && lesson.updateDate <= lastUpdateDate) {
            lessonsToDelete.add(lesson.id)
          }
        }
      }
    }
    else {
      course.sectionIds = emptyList()
    }
    sectionsToPush.addAll(course.sections.filter { it.id == 0 })

    for (sectionToPush in sectionsToPush) {
      lessonsToMove.addAll(sectionToPush.lessons.filter { it.id > 0 })
      lessonsToPush.addAll(sectionToPush.lessons.filter { it.id == 0 })
    }

    val remoteSectionIds = courseInfo.sectionIds.subList(0, courseInfo.sectionIds.size - 1)
    val sections = StepikConnector.getSections(remoteSectionIds.map { it.toString() }.toTypedArray())
    val localSectionIds = course.sections.map { it.id }
    for (section in sections) {
      if (section.name == StepikNames.PYCHARM_ADDITIONAL) {
        continue
      }
      if ((section.id !in localSectionIds && section.id !in course.sectionIds) && section.updateDate <= lastUpdateDate) {
        sectionsToDelete.add(section.id)
      }
    }
  }

  private fun isUpToDate(): Boolean {
    return !courseInfoToUpdate
        && sectionsToPush.isEmpty()
        && sectionsToDelete.isEmpty()
        && sectionsInfoToUpdate.isEmpty()
        && lessonsToPush.isEmpty()
        && lessonsToDelete.isEmpty()
        && lessonsToMove.isEmpty()
        && lessonsInfoToUpdate.isEmpty()
        && tasksToPush.isEmpty()
        && tasksToUpdate.isEmpty()
        && tasksToDelete.isEmpty()
  }
}


private fun RemoteCourse.lastUpdateDate(): Date {
  var lastUpdateDate = updateDate
  allLessons().filter { it.id > 0 }.forEach { lesson ->
    if (lastUpdateDate < lesson.updateDate) {
      lastUpdateDate = lesson.updateDate
    }

    lesson.taskList.filter { it.stepId > 0 }.forEach {
      if (lastUpdateDate < it.updateDate) {
        lastUpdateDate = it.updateDate
      }
    }
  }

  sections.filter { it.id > 0 }.forEach {
    if (lastUpdateDate < it.updateDate) {
      lastUpdateDate = it.updateDate
    }
  }

  return lastUpdateDate
}

private fun RemoteCourse.allLessons() = lessons.plus(sections.flatMap { it.lessons })

private fun RemoteCourse.setStatusRecursively(status: StepikChangeStatus) {
  stepikChangeStatus = status
  for (item in items) {
    item.stepikChangeStatus = status

    if (item is Lesson) {
      item.setStatusRecursively(status)
    }

    if (item is Section) {
      for (lesson in item.lessons) {
        lesson.setStatusRecursively(status)
      }
    }
  }
}

private fun Lesson.setStatusRecursively(status: StepikChangeStatus) {
  taskList.forEach {
    it.stepikChangeStatus = status
  }
}