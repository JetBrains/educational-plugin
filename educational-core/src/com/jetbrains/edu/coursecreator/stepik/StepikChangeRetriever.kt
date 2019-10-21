package com.jetbrains.edu.coursecreator.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask

@VisibleForTesting
data class StepikChangesInfo(var isCourseInfoChanged: Boolean = false,
                             var isCourseAdditionalInfoChanged: Boolean = false,
                             var isTopLevelSectionNameChanged: Boolean = false,
                             var isTopLevelSectionRemoved: Boolean = false,
                             var isTopLevelSectionAdded: Boolean = false,
                             var sectionsToDelete: List<Section> = ArrayList(),
                             var newSections: List<Section> = ArrayList(),
                             var sectionInfosToUpdate: MutableList<Section> = ArrayList(),
                             var newLessons: MutableList<Lesson> = ArrayList(),
                             var lessonsInfoToUpdate: MutableList<Lesson> = ArrayList(),
                             var lessonsToDelete: MutableList<Lesson> = ArrayList(),
                             var tasksToUpdate: MutableList<Task> = ArrayList(),
                             var newTasks: MutableList<Task> = ArrayList(),
                             var tasksToDelete: MutableList<Task> = ArrayList()) {
  fun isEmpty(): Boolean {
    return !isCourseInfoChanged && !isCourseAdditionalInfoChanged && !isTopLevelSectionNameChanged && !isTopLevelSectionRemoved &&
           !isTopLevelSectionAdded && sectionsToDelete.isEmpty() && newSections.isEmpty() && sectionInfosToUpdate.isEmpty() &&
           newLessons.isEmpty() && lessonsInfoToUpdate.isEmpty() && lessonsToDelete.isEmpty() && tasksToUpdate.isEmpty() &&
           newTasks.isEmpty() && tasksToDelete.isEmpty()
  }
}

class StepikChangeRetriever(private val project: Project, course: EduCourse, private val remoteCourse: EduCourse) {
  val course: EduCourse = course.copyAs(EduCourse::class.java)

  fun getChangedItems(): StepikChangesInfo {
    val stepikChanges = StepikChangesInfo()
    processCourse(stepikChanges)
    processTopLevelSection(stepikChanges)
    processSections(stepikChanges)
    processLessons(stepikChanges, course.lessons, remoteCourse.lessons)

    return stepikChanges
  }

  private fun processCourse(stepikChanges: StepikChangesInfo) {
    stepikChanges.isCourseInfoChanged = courseInfoChanged()
    stepikChanges.isCourseAdditionalInfoChanged = courseAdditionalInfoChanged(course, remoteCourse, project)
  }

  private fun processTopLevelSection(stepikChanges: StepikChangesInfo) {
    stepikChanges.isTopLevelSectionNameChanged = course.name != remoteCourse.name
    stepikChanges.isTopLevelSectionRemoved = course.sectionIds.isNotEmpty() && course.hasSections && !course.hasTopLevelLessons
    stepikChanges.isTopLevelSectionAdded = course.sectionIds.isEmpty() && course.hasTopLevelLessons && !course.hasSections
  }

  private fun processSections(stepikChanges: StepikChangesInfo) {
    val localSectionIds = course.sections.map { it.id }
    val remoteSectionIds = remoteCourse.sections.map { it.id }
    stepikChanges.sectionsToDelete = remoteCourse.sections.filter { it.id !in localSectionIds && it.id !in course.sectionIds }
    stepikChanges.newSections = course.sections.filter { it.id !in remoteSectionIds }

    for (localSection in course.sections) {
      if (remoteSectionIds.contains(localSection.id)) {
        val remoteSection = remoteCourse.sections.singleOrNull { it.id == localSection.id } ?: continue
        if (sectionInfoChanged(localSection, remoteSection)) {
          stepikChanges.sectionInfosToUpdate.add(localSection)
        }
        processLessons(stepikChanges, localSection.lessons, remoteSection.lessons)
      }
    }
  }

  private fun processLessons(stepikChanges: StepikChangesInfo, localLessons: List<Lesson>, remoteLessons: List<Lesson>) {
    val localLessonIds = localLessons.map { it.id }
    val remoteLessonIds = remoteLessons.map { it.id }

    stepikChanges.lessonsToDelete.addAll(remoteLessons.filter { it.id !in localLessonIds })
    stepikChanges.newLessons.addAll(localLessons.filter { it.id !in remoteLessonIds })

    for (localLesson in localLessons) {
      if (remoteLessonIds.contains(localLesson.id)) {
        val remoteLesson = remoteLessons.singleOrNull { it.id == localLesson.id } ?: continue
        if (lessonInfoChanged(localLesson, remoteLesson)) {
          stepikChanges.lessonsInfoToUpdate.add(localLesson)
        }
        processTasks(stepikChanges, localLesson, remoteLesson)
      }
    }
  }

  private fun processTasks(stepikChanges: StepikChangesInfo, localLesson: Lesson, remoteLesson: Lesson?) {
    val localTasksIds = localLesson.taskList.map { it.id }
    val remoteTasks = remoteLesson?.taskList ?: emptyList()
    val remoteTasksIds = remoteTasks.map { it.id }

    stepikChanges.tasksToDelete.addAll(remoteTasks.filter { it.id !in localTasksIds })
    stepikChanges.newTasks.addAll(localLesson.taskList.filter { it.id !in remoteTasksIds })

    for (task in localLesson.taskList) {
      val localTask = task.copy()
      localTask.lesson = localLesson
      runInEdtAndWait {
        runReadAction {
          CourseArchiveCreator.loadActualTexts(project, localTask)
        }
      }
      if (remoteTasksIds.contains(localTask.id)) {
        val remoteTask = remoteTasks.singleOrNull { it.id == localTask.id } ?: continue
        if (taskInfoChanged(localTask, remoteTask) || taskContentChanged(localTask, remoteTask) || taskFilesChanged(localTask,
                                                                                                                    remoteTask)) {
          stepikChanges.tasksToUpdate.add(task)
        }
      }
    }
  }

  private fun courseInfoChanged(): Boolean {
    return course.name != remoteCourse.name ||
           course.description != remoteCourse.description ||
           course.humanLanguage != remoteCourse.humanLanguage ||
           course.languageID != remoteCourse.languageID ||
           course.formatVersion != remoteCourse.formatVersion
  }

  private fun courseAdditionalInfoChanged(course: EduCourse, remoteCourse: EduCourse, project: Project): Boolean {
    if (course.solutionsHidden != remoteCourse.solutionsHidden) return true
    val additionalFiles = CCUtils.collectAdditionalFiles(course, project)
    if (additionalFiles.size != remoteCourse.additionalFiles.size) return true
    for ((index, additionalFile) in additionalFiles.withIndex()) {
      val remoteAdditionalFile = remoteCourse.additionalFiles[index]
      if (!additionalFile.isEqualTo(remoteAdditionalFile)) return true
    }
    return false
  }

  private fun sectionInfoChanged(section: Section, remoteSection: Section): Boolean {
    return section.index != remoteSection.index ||
           section.name != remoteSection.name
  }

  private fun lessonInfoChanged(lesson: Lesson, remoteLesson: Lesson): Boolean {
    val localSection = lesson.section?.id ?: (lesson.course as EduCourse).sectionIds.first()
    val remoteSection = remoteLesson.section?.id ?: (remoteLesson.course as EduCourse).sectionIds.first()
    return lesson.index != remoteLesson.index ||
           lesson.name != remoteLesson.name ||
           localSection != remoteSection
  }

  private fun taskInfoChanged(task: Task, remoteTask: Task): Boolean {
    //choice tasks are being renamed to "Quiz" when uploading to Stepik
    return (task !is ChoiceTask && task.name != remoteTask.name) ||
           task.index != remoteTask.index ||
           task.descriptionText != remoteTask.descriptionText ||
           task.descriptionFormat != remoteTask.descriptionFormat ||
           task.feedbackLink.link != remoteTask.feedbackLink.link ||
           task.feedbackLink.type != remoteTask.feedbackLink.type ||
           task.lesson.id != remoteTask.lesson.id ||
           task.solutionHidden != remoteTask.solutionHidden
  }

  private fun taskContentChanged(localTask: Task, remoteTask: Task): Boolean {
    if (!(localTask is ChoiceTask && remoteTask is ChoiceTask)) {
      return false
    }
    return localTask.isMultipleChoice != remoteTask.isMultipleChoice
           || localTask.choiceOptions != remoteTask.choiceOptions
           || localTask.messageIncorrect != remoteTask.messageIncorrect
           || localTask.messageCorrect != remoteTask.messageCorrect
  }

  private fun taskFilesChanged(task: Task, remoteTask: Task): Boolean {
    val taskFiles = task.taskFiles
    val remoteTaskFiles = remoteTask.taskFiles
    if (taskFiles.size != remoteTaskFiles.size) return true

    for ((path, taskFile) in taskFiles) {
      val remoteTaskFile = remoteTaskFiles[path] ?: return true
      if (!taskFile.isEqualTo(remoteTaskFile)) return true
    }
    return false
  }

  private fun TaskFile.isEqualTo(otherTaskFile: TaskFile): Boolean {
    if (this === otherTaskFile) return true

    val answerPlaceholdersEqual = answerPlaceholders.size == otherTaskFile.answerPlaceholders.size &&
                                  answerPlaceholders.zip(otherTaskFile.answerPlaceholders).all { it.first.isEqualTo(it.second) }

    return name == otherTaskFile.name &&
           text == otherTaskFile.text &&
           isVisible == otherTaskFile.isVisible &&
           answerPlaceholdersEqual
  }

  private fun AnswerPlaceholder.isEqualTo(otherPlaceholder: AnswerPlaceholder): Boolean {
    if (this === otherPlaceholder) return true

    val placeholderDependencyEqual = placeholderDependency?.isEqualTo(otherPlaceholder.placeholderDependency) ?:
                                     (otherPlaceholder.placeholderDependency == null)

    return offset == otherPlaceholder.offset &&
           length == otherPlaceholder.length &&
           index == otherPlaceholder.index &&
           placeholderText == otherPlaceholder.placeholderText &&
           hints == otherPlaceholder.hints &&
           placeholderDependencyEqual
  }

  private fun AnswerPlaceholderDependency.isEqualTo(otherDependency: AnswerPlaceholderDependency?): Boolean {
    if (this === otherDependency) return true
    if (otherDependency == null) return false

    return isVisible == otherDependency.isVisible &&
           fileName == otherDependency.fileName &&
           lessonName == otherDependency.lessonName &&
           placeholderIndex == otherDependency.placeholderIndex &&
           sectionName == otherDependency.sectionName
  }
}