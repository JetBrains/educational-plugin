package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO
import com.jetbrains.edu.learning.messages.EduCoreBundle

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

  override fun getCheckAction(): CheckAction = CheckAction(EduCoreBundle.lazyMessage("codeforces.copy.and.submit"))
}
