package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.regex.Pattern

class AnswerPlaceholderDependency() {
  var sectionName: String? = null
  var lessonName: String = ""
  var taskName: String = ""
  var fileName: String = ""
  var placeholderIndex: Int = 0

  // isVisible is used only for backward compatibility.
  // It might have been written to json/yaml in older versions of the plugin
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
    return taskFile.answerPlaceholders.getOrNull(placeholderIndex)
  }

  override fun toString(): String {
    val section = if (sectionName != null) "$sectionName#" else ""
    return "$section$lessonName#$taskName#$fileName#${placeholderIndex + 1}"
  }

  class InvalidDependencyException : IllegalStateException {
    val customMessage: String

    constructor(dependencyText: String) : super("'$dependencyText' is not a valid placeholder dependency") {
      customMessage = message("exception.placeholder.invalid.dependency")
    }

    constructor(
      dependencyText: String,
      customMessage: String
    ) : super("'$dependencyText' is not a valid placeholder dependency\n$customMessage") {
      this.customMessage = customMessage
    }
  }

  companion object {
    private val DEPENDENCY_PATTERN = Pattern.compile("(([^#]+)#)?([^#]+)#([^#]+)#([^#]+)#(\\d+)")

    @Throws(InvalidDependencyException::class)
    fun create(answerPlaceholder: AnswerPlaceholder, text: String): AnswerPlaceholderDependency? {
      if (text.isBlank()) {
        return null
      }
      val task = answerPlaceholder.taskFile.task
      val matcher = DEPENDENCY_PATTERN.matcher(text)
      if (!matcher.matches()) {
        throw InvalidDependencyException(text)
      }

      val sectionName = matcher.group(2)
      val lessonName = matcher.group(3)
      val taskName = matcher.group(4)
      val filePath = toSystemIndependent(matcher.group(5))

      val placeholderIndex = try {
        matcher.group(6).toInt() - 1
      }
      catch (e: NumberFormatException) {
        throw InvalidDependencyException(text)
      }

      val dependency = AnswerPlaceholderDependency(answerPlaceholder, sectionName, lessonName, taskName, filePath, placeholderIndex, true)
      val targetPlaceholder = dependency.resolve(task.course)
                              ?: throw InvalidDependencyException(text, message("exception.placeholder.non.existing"))
      if (targetPlaceholder.taskFile.task == task) {
        throw InvalidDependencyException(text, message("exception.placeholder.wrong.reference.to.source"))
      }
      if (refersToNextTask(task, targetPlaceholder.taskFile.task)) {
        throw InvalidDependencyException(text, message("exception.placeholder.wrong.reference.to.next"))
      }
      return dependency
    }

    private fun toSystemIndependent(path: String) = path.replace('\\', '/')

    private fun refersToNextTask(sourceTask: Task, targetTask: Task): Boolean {
      val sourceLesson = sourceTask.lesson
      val targetLesson = targetTask.lesson
      if (sourceLesson == targetLesson) {
        return targetTask.index > sourceTask.index
      }
      return if (sourceLesson.section == targetLesson.section) {
        targetLesson.index > sourceLesson.index
      }
      else getIndexInCourse(targetLesson) > getIndexInCourse(sourceLesson)
    }

    private fun getIndexInCourse(lesson: Lesson): Int {
      return lesson.section?.index ?: lesson.index
    }
  }
}
