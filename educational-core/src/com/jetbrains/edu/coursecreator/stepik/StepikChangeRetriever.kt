package com.jetbrains.edu.coursecreator.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.stepik.courseFormat.StepikLessonRemoteInfo
import com.jetbrains.edu.learning.stepik.courseFormat.ext.getLesson
import com.jetbrains.edu.learning.stepik.courseFormat.ext.id
import com.jetbrains.edu.learning.stepik.courseFormat.ext.stepId

@VisibleForTesting
data class StepikChangesInfo(var isCourseInfoChanged: Boolean = false,
                             var newSections: List<Section> = ArrayList(),
                             var sectionInfosToUpdate: List<Section> = ArrayList(),
                             var newLessons: List<Lesson> = ArrayList(),
                             var lessonsInfoToUpdate: List<Lesson> = ArrayList(),
                             var tasksToUpdateByLessonIndex: Map<Int, List<Task>> = HashMap(),
                             var tasksToPostByLessonIndex: Map<Int, List<Task>> = HashMap())

class StepikChangeRetriever(val project: Project, private val courseFromServer: StepikCourse) {

  fun getChangedItems(): StepikChangesInfo {
    val course = StudyTaskManager.getInstance(project).course as StepikCourse
    if (!isUnitTestMode) {
      setTaskFileTextFromDocuments()
    }
    val stepikChanges = StepikChangesInfo()

    stepikChanges.isCourseInfoChanged = courseInfoChanged(course, courseFromServer)

    val sectionIdsFromServer = courseFromServer.sections.map { it.id }
    stepikChanges.newSections = course.sections.filter { it.id == 0 }
    stepikChanges.sectionInfosToUpdate = sectionsInfoToUpdate(course, sectionIdsFromServer, courseFromServer)

    val serverLessonIds = lessonIds(courseFromServer)
    val allLessons = allLessons(course)

    stepikChanges.newLessons = allLessons.filter { it.id == 0 }
    stepikChanges.lessonsInfoToUpdate = lessonsInfoToUpdate(course, serverLessonIds, courseFromServer)

    val updateCandidates = allLessons.filter { lesson -> serverLessonIds.contains(lesson.id) }
    val lessonsById = allLessons(courseFromServer).associateBy({ it.id }, { it })
    stepikChanges.tasksToPostByLessonIndex = updateCandidates
      .filter { !stepikChanges.newLessons.contains(it) }
      .associateBy({ it.index },
                   { newTasks(lessonsById[it.id]!!, it) })
      .filterValues { !it.isEmpty() }
    stepikChanges.tasksToUpdateByLessonIndex = updateCandidates.associateBy({ it.index },
                                                                            {
                                                                              tasksToUpdate(lessonsById[it.id]!!, it)
                                                                            }).filterValues { !it.isEmpty() }

    return stepikChanges
  }

  private fun allLessons(course: StepikCourse) = course.lessons.plus(course.sections.flatMap { it.lessons })

  fun setStepikChangeStatuses() {
    val course = StudyTaskManager.getInstance(project).course as StepikCourse
    val stepikChanges = getChangedItems()

    if (stepikChanges.isCourseInfoChanged) {
      StepikCourseChangeHandler.infoChanged(course)
    }

    if (!stepikChanges.newSections.isEmpty()) {
      StepikCourseChangeHandler.contentChanged(course)
    }

    stepikChanges.sectionInfosToUpdate.forEach {
      StepikCourseChangeHandler.infoChanged(it)
    }

    stepikChanges.newLessons.forEach {
      StepikCourseChangeHandler.contentChanged(it.section ?: course)
    }

    stepikChanges.lessonsInfoToUpdate.forEach {
      StepikCourseChangeHandler.infoChanged(it)
    }

    stepikChanges.tasksToPostByLessonIndex.forEach { _, taskList ->
      if (!taskList.isEmpty()) {
        StepikCourseChangeHandler.contentChanged(taskList.single().lesson)
        taskList.forEach { it.stepId = 0 }
      }
    }

    stepikChanges.tasksToUpdateByLessonIndex.forEach { _, taskList ->
      taskList.forEach {
        StepikCourseChangeHandler.changed(it)
      }
    }
  }


  private fun setTaskFileTextFromDocuments() {
    val course = StudyTaskManager.getInstance(project).course as StepikCourse
    runInEdtAndWait {
      runReadAction {
        course.lessons
          .flatMap { it.taskList }
          .flatMap { it.taskFiles.values }
          .forEach { it.setText(EduUtils.createStudentFile(project, it.getVirtualFile(project)!!, it.task)!!.getText()) }
      }
    }
  }

  private fun taskIds(lessonFormServer: Lesson) = lessonFormServer.taskList.map { task -> task.stepId }

  private fun newTasks(lessonFormServer: Lesson, updateCandidate: Lesson): List<Task> {
    val onServerTaskIds = taskIds(lessonFormServer)
    return updateCandidate.taskList.filter { task -> !onServerTaskIds.contains(task.stepId) }
  }

  private fun lessonIds(latestCourseFromServer: StepikCourse) = latestCourseFromServer.lessons
    .plus(latestCourseFromServer.sections.flatMap { it.lessons })
    .map { lesson -> lesson.id }

  private fun courseInfoChanged(course: StepikCourse, latestCourseFromServer: StepikCourse): Boolean {
    return course.name != latestCourseFromServer.name ||
           course.description != latestCourseFromServer.description ||
           course.humanLanguage != latestCourseFromServer.humanLanguage ||
           course.languageID != latestCourseFromServer.languageID
  }

  private fun tasksToUpdate(lessonFormServer: Lesson, updateCandidate: Lesson): List<Task> {
    val onServerTaskIds = taskIds(lessonFormServer)
    val tasksUpdateCandidate = updateCandidate.taskList.filter { task -> task.stepId in onServerTaskIds }

    val taskById = lessonFormServer.taskList.associateBy({ it.stepId }, { it })
    return tasksUpdateCandidate.filter { !it.isEqualTo(taskById[it.stepId]) }
  }

  private fun lessonsInfoToUpdate(course: Course,
                                  serverLessonIds: List<Int>,
                                  latestCourseFromServer: StepikCourse): List<Lesson> {
    return course.lessons
      .asSequence()
      .filter { lesson -> serverLessonIds.contains(lesson.id) }
      .filter { updateCandidate ->
        val lessonFormServer = latestCourseFromServer.getLesson(updateCandidate.id)!!
        val remoteInfo = lessonFormServer.remoteInfo as StepikLessonRemoteInfo
        val updatedRemoteInfo = updateCandidate.remoteInfo as StepikLessonRemoteInfo
        lessonFormServer.index != updateCandidate.index ||
        lessonFormServer.name != updateCandidate.name ||
        remoteInfo.isPublic != updatedRemoteInfo.isPublic
      }
      .toList()
  }

  private fun sectionsInfoToUpdate(course: StepikCourse,
                                   sectionIdsFromServer: List<Int>,
                                   latestCourseFromServer: StepikCourse): List<Section> {
    val sectionsById = latestCourseFromServer.sections.associateBy({ it.id }, { it })
    return course.sections
      .filter { sectionIdsFromServer.contains(it.id) }
      .filter {
        val sectionFromServer = sectionsById[it.id]!!
        it.index != sectionFromServer.index ||
        it.name != sectionFromServer.name
      }
  }

  private fun AnswerPlaceholderDependency.isEqualTo(otherDependency: AnswerPlaceholderDependency?): Boolean {
    if (this === otherDependency) return true
    if (otherDependency == null) return false

    return isVisible == otherDependency.isVisible
           && fileName == otherDependency.fileName
           && lessonName == otherDependency.lessonName
           && placeholderIndex == otherDependency.placeholderIndex
           && sectionName == otherDependency.sectionName
  }

  private fun AnswerPlaceholder.isEqualTo(otherPlaceholder: AnswerPlaceholder): Boolean {
    if (this === otherPlaceholder) return true

    return offset == otherPlaceholder.offset
           && length == otherPlaceholder.length
           && index == otherPlaceholder.index
           && possibleAnswer == otherPlaceholder.possibleAnswer
           && hints == otherPlaceholder.hints
           && (placeholderDependency?.isEqualTo(otherPlaceholder.placeholderDependency) ?: otherPlaceholder.placeholderDependency == null)

  }

  private fun TaskFile.isEqualTo(otherTaskFile: TaskFile): Boolean {
    if (this === otherTaskFile) return true

    return name == otherTaskFile.name
           && getText() == otherTaskFile.getText()
           && answerPlaceholders.size == otherTaskFile.answerPlaceholders.size
           && answerPlaceholders.zip(otherTaskFile.answerPlaceholders).all { it.first.isEqualTo(it.second) }
  }

  private fun Task.isEqualTo(otherTask: Task?): Boolean {
    if (this === otherTask) return true
    if (otherTask == null) return false

    return descriptionText == otherTask.descriptionText
           && index == otherTask.index
           && name == otherTask.name
           && testsText == otherTask.testsText
           && compareFiles(taskFiles, otherTask.taskFiles)
           && compareFiles(additionalFiles, otherTask.additionalFiles)
  }

  private fun compareFiles(files: Map<String, Any>, otherFiles: Map<String, Any>): Boolean {
    if (files.size != otherFiles.size) {
      return false
    }

    for (entry in files.entries) {
      val name = entry.key
      val file = entry.value

      if (!otherFiles.containsKey(name)) {
        return false
      }

      val otherFile = otherFiles[name]
      return when (file) {
        is String -> file != otherFile
        is TaskFile -> file.isEqualTo(otherFile as TaskFile)
        else -> true
      }
    }

    return true
  }
}