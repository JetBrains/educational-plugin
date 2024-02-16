package com.jetbrains.edu.learning.courseFormat

class AnswerPlaceholder {
  var offset: Int = -1

  /*
   * length of text to surround with visual placeholder
   * (placeholderText.length in student file; possibleAnswer.length in course creator file)
   */
  var length: Int = -1
  var index: Int = -1

  private var _initialState: MyInitialState? = null
  var initialState: MyInitialState
    get() = _initialState ?: error("No initial state for answer placeholder")
    set(value) {
      _initialState = value
    }

  var placeholderDependency: AnswerPlaceholderDependency? = null
    set(value) {
      value?.answerPlaceholder = this
      field = value
    }

  var isInitializedFromDependency: Boolean = false
  var possibleAnswer: String = "" // could be empty in course creator file
  var placeholderText: String = "" //  could be empty in student file (including task file preview in course creator mode)

  var selected: Boolean = false
  var status: CheckStatus = CheckStatus.Unchecked

  /**
   * Stores the visibility for JSON and YAML.
   * To get real visibility, use [shouldBeVisible] instead.
   * [shouldBeVisible] also considers the visibility of the dependency if it is present.
   *
   * This is a temporary behaviour for the transition period, and in future [shouldBeVisible] will become a getter for this field.
   */
  var isVisible: Boolean = true
    @Deprecated(
      "This field does not store actual value about the visibility, it stores value to be serialized in json or yaml",
      replaceWith = ReplaceWith("shouldBeVisible")
    )
    get

  @Transient
  private var _taskFile: TaskFile? = null
  var taskFile: TaskFile
    get() = _taskFile ?: error("Task file is null for answer placeholder")
    set(value) {
      _taskFile = value
    }

  /*
   * Actual student's answer, used to restore state of task of framework lesson after navigation actions
   */
  var studentAnswer: String? = null

  constructor()
  constructor(offset: Int, placeholderText: String) {
    this.offset = offset
    length = placeholderText.length
    this.placeholderText = placeholderText
  }

  fun init(file: TaskFile, isRestarted: Boolean) {
    taskFile = file
    if (isRestarted) return
    if (placeholderDependency != null) {
      placeholderDependency?.answerPlaceholder = this
    }
    initialState = MyInitialState(offset, length)
    status = file.task.status
  }

  fun reset(revertStartOffset: Boolean) {
    if (revertStartOffset) {
      offset = initialState.offset
    }
    length = initialState.length
    status = CheckStatus.Unchecked
    isInitializedFromDependency = false
  }

  fun init() {
    initialState = MyInitialState(offset, placeholderText.length)
  }

  /**
   * Whether the placeholder should be visible by a learner.
   * Depends on the [shouldBeVisible], but also considers, whether the placeholder was initialized from dependency.
   */
  val isCurrentlyVisible: Boolean
    get() {
      var result = shouldBeVisible

      if (placeholderDependency != null) {
        result = result || !isInitializedFromDependency
      }

      return result
    }

  /**
   * Whether the placeholder should be visible or not.
   * Considers the [isVisible] field and the visibility of the [placeholderDependency].
   * Use it instead of [isVisible] because the latter stores information for yaml/json serialization.
   */
  val shouldBeVisible: Boolean
    get() {
      val dependency = placeholderDependency

      return if (dependency == null) {
        isVisible
      }
      else {
        isVisible && dependency.isVisible
      }
    }

  class MyInitialState {
    var length: Int = -1
    var offset: Int = -1

    @Suppress("unused") // used for deserialization
    constructor()
    constructor(initialOffset: Int, initialLength: Int) {
      offset = initialOffset
      length = initialLength
    }
  }

  fun isValid(textLength: Int): Boolean {
    return offset >= 0 && length >= 0 && endOffset <= textLength
  }

  val endOffset: Int
    get() {
      return offset + length
    }

  override fun toString(): String {
    val task = taskFile.task
    val lesson = task.lesson
    val section = lesson.section

    val sectionPrefix = if (section != null) "${section.name}#" else ""
    return "$sectionPrefix${lesson.name}#${task.name}#${taskFile.name}[$offset, $endOffset]"
  }
}
