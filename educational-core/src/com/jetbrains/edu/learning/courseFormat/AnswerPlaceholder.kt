package com.jetbrains.edu.learning.courseFormat

import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import java.awt.Color

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

  fun initAnswerPlaceholder(file: TaskFile, isRestarted: Boolean) {
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

  val isVisible: Boolean
    get() {
      val placeholderVisible = placeholderDependency?.isVisible ?: true
      return placeholderVisible || !isInitializedFromDependency
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

  fun getColor(): JBColor {
    return when (status) {
      CheckStatus.Solved -> {
        val colorLight = createColor("26993D", 90, JBColor.LIGHT_GRAY)
        val colorDark = createColor("47CC5E", 82, JBColor.LIGHT_GRAY)
        JBColor(colorLight, colorDark)
      }
      CheckStatus.Failed -> {
        val colorLight = createColor("CC0000", 64, JBColor.GRAY)
        val colorDark = createColor("FF7373", 90, JBColor.GRAY)
        JBColor(colorLight, colorDark)
      }
      else -> getDefaultPlaceholderColor()
    }
  }

  private fun createColor(str: String, alpha: Int, defaultValue: Color): Color {
    val color = ColorUtil.fromHex(str, defaultValue)
    return ColorUtil.toAlpha(color, alpha)
  }

  private fun getDefaultPlaceholderColor(): JBColor {
    var colorLight = ColorUtil.fromHex("284B73", JBColor.GRAY)
    colorLight = ColorUtil.toAlpha(colorLight, 64)
    var colorDark = ColorUtil.fromHex("A1C1E6", JBColor.GRAY)
    colorDark = ColorUtil.toAlpha(colorDark, 72)
    return JBColor(colorLight, colorDark)
  }
}