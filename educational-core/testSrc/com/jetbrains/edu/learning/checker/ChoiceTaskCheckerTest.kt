package com.jetbrains.edu.learning.checker

import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.navigation.NavigationUtils


class ChoiceTaskCheckerTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    CheckActionListener.reset()
  }

  fun `test single choice solved`() {
    courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceTask.OptionStatus.CORRECT, "2" to ChoiceTask.OptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(0))
    launchAction()
  }

  fun `test single choice failed`() {
    courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceTask.OptionStatus.CORRECT, "2" to ChoiceTask.OptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(1))
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Incorrect" }
    launchAction()
  }

  fun `test multiple choice solved`() {
    courseWithFiles {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceTask.OptionStatus.CORRECT, "2" to ChoiceTask.OptionStatus.CORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(1, 0))
    launchAction()
  }

  fun `test multiple choice failed`() {
    courseWithFiles {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceTask.OptionStatus.CORRECT, "2" to ChoiceTask.OptionStatus.CORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(1))
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Incorrect" }
    launchAction()
  }



  private fun selectOptions(optionsToSelect: List<Int>) {
    val task = findTask(0, 0)
    NavigationUtils.navigateToTask(project, task)
    (task as ChoiceTask).selectedVariants.addAll(optionsToSelect)
  }


  private fun launchAction() {
    val action = CheckAction()
    val e = TestActionEvent(action)
    action.beforeActionPerformedUpdate(e)
    assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
    action.actionPerformed(e)
  }
}