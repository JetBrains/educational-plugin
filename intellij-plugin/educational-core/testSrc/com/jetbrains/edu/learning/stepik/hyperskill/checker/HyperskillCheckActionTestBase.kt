package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.edu.learning.waitUntilIndexesAreReady

abstract class HyperskillCheckActionTestBase : HyperskillActionTestBase() {

  override fun setUp() {
    super.setUp()
    CheckActionListener.registerListener(testRootDisposable)
  }

  protected fun checkCheckAction(task: Task, expectedStatus: CheckStatus, expectedMessage: String? = null) {
    // `testAction` won't wait when indexes are ready because `CheckAction` is `DumbAware`.
    // But here we want to check `CheckAction` only in smart mode, so wait for it manually
    waitUntilIndexesAreReady(project)

    when (expectedStatus) {
      CheckStatus.Unchecked -> CheckActionListener.shouldSkip()
      CheckStatus.Solved -> CheckActionListener.reset()
      CheckStatus.Failed -> CheckActionListener.shouldFail()
    }
    if (expectedMessage != null) {
      CheckActionListener.expectedMessage { expectedMessage }
    }

    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }
}
