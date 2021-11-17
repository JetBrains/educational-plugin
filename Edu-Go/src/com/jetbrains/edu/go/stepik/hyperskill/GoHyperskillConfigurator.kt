package com.jetbrains.edu.go.stepik.hyperskill

import com.jetbrains.edu.go.GoConfigurator
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class GoHyperskillConfigurator : HyperskillConfigurator<GoProjectSettings>(GoConfigurator()) {
  override fun getMockFileName(text: String): String = MAIN_GO

  /**
   * Go projects from JetBrains Academy with 'edu' tasks inside
   * (which are [com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask])
   * are being remotely checked (not locally as usual!). Checks are made on remote side in Docker and some tests are used there.
   * Such `some` tests could be written in ANY language like Python, Java, etc.
   * But usually we rely on thought that tests are written in the same language (Go in the current case) and found by specific name of file
   * (in the current case it was [com.jetbrains.edu.go.GoConfigurator.TEST_GO]) or specific name of folder with tests inside.
   * Right now we made an agreement with JBA to consider `tests.py` file and files inside `test` folder as tests.
   * TODO This solution is TEMPORARY and must be removed after https://youtrack.jetbrains.com/issue/EDU-4527 is being implemented.
   *
   * also see source bug https://youtrack.jetbrains.com/issue/EDU-4714
   */
  override val testFileName: String
    get() = TESTS_PY

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  companion object {
    private const val TESTS_PY = "tests.py"
  }
}