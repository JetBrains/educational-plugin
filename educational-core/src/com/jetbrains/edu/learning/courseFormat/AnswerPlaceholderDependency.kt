package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import java.util.regex.Pattern

class AnswerPlaceholderDependency() {
  var sectionName: String? = null
  var lessonName: String = ""
  var taskName: String = ""
  var fileName: String = ""
  var placeholderIndex: Int = 0
  var isVisible: Boolean = true

  @Transient
  lateinit var answerPlaceholder: AnswerPlaceholder

  constructor(answerPlaceholder: AnswerPlaceholder,
              sectionName: String?,
              lessonName: String,
              taskName: String,
              fileName: String,
              placeholderIndex: Int,
              isVisible: Boolean) : this() {
    this.sectionName = sectionName
    this.lessonName = lessonName
    this.taskName = taskName
    this.fileName = fileName
    this.placeholderIndex = placeholderIndex
    this.answerPlaceholder = answerPlaceholder
    this.isVisible = isVisible
  }

  fun resolve(course: Course): AnswerPlaceholder? {
    val lesson = course.getLesson(sectionName, lessonName) ?: return null
    val task = lesson.getTask(taskName) ?: return null
    val taskFile = task.getTaskFile(fileName) ?: return null
    return if (!EduUtils.indexIsValid(placeholderIndex, taskFile.answerPlaceholders)) {
      null
    }
    else taskFile.answerPlaceholders[placeholderIndex]
  }

  override fun toString(): String {
    val section = if (sectionName != null) "$sectionName#" else ""
    return "$section$lessonName#$taskName#$fileName#${placeholderIndex + 1}"
  }

  class InvalidDependencyException : IllegalStateException {
    val customMessage: String

    constructor(dependencyText: String) : super(message("exception.placeholder.invalid.dependency.detailed", dependencyText)) {
      customMessage = message("exception.placeholder.invalid.dependency")
    }

    constructor(dependencyText: String, customMessage: String) : super(
      message("exception.placeholder.invalid.dependency.detailed.with.custom.message", dependencyText, customMessage)) {
      this.customMessage = customMessage
    }
  }

  companion object {
    private val DEPENDENCY_PATTERN = Pattern.compile("(([^#]+)#)?([^#]+)#([^#]+)#([^#]+)#(\\d+)")

    @JvmStatic
    @JvmOverloads
    @Throws(InvalidDependencyException::class)
    fun create(answerPlaceholder: AnswerPlaceholder, text: String, isVisible: Boolean = true): AnswerPlaceholderDependency? {
      if (text.isBlank()) {
        return null
      }
      val task = answerPlaceholder.taskFile.task
      val course = task.course
      val matcher = DEPENDENCY_PATTERN.matcher(text)
      if (!matcher.matches()) {
        throw InvalidDependencyException(text)
      }
      return try {
        val sectionName = matcher.group(2)
        val lessonName = matcher.group(3)
        val taskName = matcher.group(4)
        val file = FileUtil.toSystemIndependentName(matcher.group(5))
        val placeholderIndex = matcher.group(6).toInt() - 1
        val dependency = AnswerPlaceholderDependency(answerPlaceholder, sectionName, lessonName, taskName, file, placeholderIndex,
                                                     isVisible)
        val targetPlaceholder = dependency.resolve(course)
                                ?: throw InvalidDependencyException(text, message("exception.placeholder.non.existing"))
        if (targetPlaceholder.taskFile.task === task) {
          throw InvalidDependencyException(text, message("exception.placeholder.wrong.reference.to.source"))
        }
        if (refersToNextTask(task, targetPlaceholder.taskFile.task)) {
          throw InvalidDependencyException(text, message("exception.placeholder.wrong.reference.to.next"))
        }
        dependency
      }
      catch (e: NumberFormatException) {
        throw InvalidDependencyException(text)
      }
    }

    private fun refersToNextTask(sourceTask: Task, targetTask: Task): Boolean {
      val sourceLesson = sourceTask.lesson
      val targetLesson = targetTask.lesson
      if (sourceLesson === targetLesson) {
        return targetTask.index > sourceTask.index
      }
      return if (sourceLesson.section === targetLesson.section) {
        targetLesson.index > sourceLesson.index
      }
      else getIndexInCourse(targetLesson) > getIndexInCourse(sourceLesson)
    }

    private fun getIndexInCourse(lesson: Lesson): Int {
      val section = lesson.section
      return section?.index ?: lesson.index
    }
  }
}