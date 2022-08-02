package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

abstract class OutputTaskBase: Task {
  open val inputFileName: String = INPUT_PATTERN_NAME
  open val outputFileName: String = OUTPUT_PATTERN_NAME
  val latestOutputFileName: String = "latest_output.txt"

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override val supportSubmissions: Boolean
    get() = true

  companion object {
    const val OUTPUT_PATTERN_NAME = "output.txt"
    const val INPUT_PATTERN_NAME = "input.txt"
  }
}