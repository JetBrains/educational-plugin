package com.jetbrains.edu.learning.courseFormat

import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor

class AnswerPlaceholder {
  var offset: Int = -1

  /*
   * length of text to surround with visual placeholder
   * (placeholderText.length in student file; possibleAnswer.length in course creator file)
   */
  var length: Int = -1
  var index: Int = -1
  lateinit var initialState: MyInitialState

  var placeholderDependency: AnswerPlaceholderDependency? = null
    set(value) {
      field = value
      if (value != null) {
        field?.answerPlaceholder = this
      }
    }

  var isInitializedFromDependency: Boolean = false
  var possibleAnswer: String = "" // could be empty in course creator file
  var placeholderText: String = "" //  could be empty in student file (including task file preview in course creator mode)

  var selected: Boolean = false
  var status = CheckStatus.Unchecked

  @Transient
  lateinit var taskFile: TaskFile

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
      return placeholderDependency == null || placeholderDependency!!.isVisible || !isInitializedFromDependency
    }

  class MyInitialState {
    var length: Int = -1
    var offset: Int = -1

    @Suppress("unused") // used for deserialization
    constructor()
    constructor(initialOffset: Int, length: Int) {
      offset = initialOffset
      this.length = length
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
    if (status === CheckStatus.Solved) {
      var colorLight = ColorUtil.fromHex("26993D", JBColor.LIGHT_GRAY)
      colorLight = ColorUtil.toAlpha(colorLight, 90)
      var colorDark = ColorUtil.fromHex("47CC5E", JBColor.LIGHT_GRAY)
      colorDark = ColorUtil.toAlpha(colorDark, 82)
      return JBColor(colorLight, colorDark)
    }
    if (status === CheckStatus.Failed) {
      var colorLight = ColorUtil.fromHex("CC0000", JBColor.GRAY)
      colorLight = ColorUtil.toAlpha(colorLight, 64)
      var colorDark = ColorUtil.fromHex("FF7373", JBColor.GRAY)
      colorDark = ColorUtil.toAlpha(colorDark, 90)
      return JBColor(colorLight, colorDark)
    }
    return getDefaultPlaceholderColor()
  }

  private fun getDefaultPlaceholderColor(): JBColor {
    var colorLight = ColorUtil.fromHex("284B73", JBColor.GRAY)
    colorLight = ColorUtil.toAlpha(colorLight, 64)
    var colorDark = ColorUtil.fromHex("A1C1E6", JBColor.GRAY)
    colorDark = ColorUtil.toAlpha(colorDark, 72)
    return JBColor(colorLight, colorDark)
  }
}