package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel

abstract class HyperskillCheckActionTestBase : EduActionTestCase() {

  protected val mockConnector: MockHyperskillConnector
    get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
    CheckActionListener.registerListener(testRootDisposable)
  }

  override fun tearDown() {
    try {
      logOutFakeHyperskillUser()
    }
    finally {
      super.tearDown()
    }
  }

  protected fun checkCheckAction(task: Task, expectedStatus: CheckStatus, expectedMessage: String? = null) {
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
