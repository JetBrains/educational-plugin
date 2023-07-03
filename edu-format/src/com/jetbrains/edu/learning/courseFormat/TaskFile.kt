package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Implementation of task file which contains task answer placeholders for student to type in and
 * which is visible to student in project view
 */
class TaskFile : EduFile {
  constructor()
  constructor(name: String, text: String) {
    this.name = name
    this.text = text
  }

  constructor(name: String, text: String, isVisible: Boolean) : this(name, text) {
    this.isVisible = isVisible
  }

  constructor(name: String, text: String, isVisible: Boolean, isLearnerCreated: Boolean) : this(name, text, isVisible) {
    this.isLearnerCreated = isLearnerCreated
  }

  var answerPlaceholders: List<AnswerPlaceholder>
    get() = _answerPlaceholders
    set(value) {
      _answerPlaceholders = value.toMutableList()
    }

  private var _answerPlaceholders = mutableListOf<AnswerPlaceholder>()
  @Transient
  private var _task: Task? = null

  var task: Task
    get() = _task ?: error("Task is null for TaskFile $name")
    set(value) {
      _task = value
    }

  fun initTaskFile(task: Task, isRestarted: Boolean) {
    this.task = task
    this.course = task.course
    for (answerPlaceholder in _answerPlaceholders) {
      answerPlaceholder.init(this, isRestarted)
    }

    sortAnswerPlaceholders()
  }

  fun addAnswerPlaceholder(answerPlaceholder: AnswerPlaceholder) {
    _answerPlaceholders.add(answerPlaceholder)
  }

  fun removeAnswerPlaceholder(answerPlaceholder: AnswerPlaceholder) {
    _answerPlaceholders.remove(answerPlaceholder)
  }

  fun getAnswerPlaceholder(offset: Int): AnswerPlaceholder? {
    return _answerPlaceholders.firstOrNull { offset in it.offset..it.endOffset }
  }

  fun removeAllPlaceholders() {
    _answerPlaceholders.clear()
  }

  fun sortAnswerPlaceholders() {
    _answerPlaceholders.sortWith(AnswerPlaceholderComparator)
    for (i in _answerPlaceholders.indices) {
      _answerPlaceholders[i].index = i
    }
  }

  fun hasFailedPlaceholders(): Boolean {
    return _answerPlaceholders.any { it.status == CheckStatus.Failed }
  }

  fun isValid(text: String): Boolean {
    return _answerPlaceholders.all { it.isValid(text.length) }
  }
}