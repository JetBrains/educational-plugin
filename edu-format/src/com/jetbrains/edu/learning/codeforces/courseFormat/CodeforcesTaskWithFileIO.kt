package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO


class CodeforcesTaskWithFileIO : CodeforcesTask {
  override lateinit var inputFileName: String
  override lateinit var outputFileName: String

  @Suppress("unused") //used for deserialization
  constructor()

  constructor(inputFileName: String, outputFileName: String) : super() {
    this.inputFileName = inputFileName
    this.outputFileName = outputFileName
  }

  override val itemType = CODEFORCES_TASK_TYPE_WITH_FILE_IO
}
