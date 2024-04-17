package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import org.junit.Test


class ChoiceTaskCheckerTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    CheckActionListener.registerListener(testRootDisposable)
    CheckActionListener.reset()
  }

  @Test
  fun `test single choice solved`() {
    courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(0))
    launchAction()
  }

  @Test
  fun `test single choice failed`() {
    courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(1))
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Incorrect solution" }
    launchAction()
  }

  @Test
  fun `test multiple choice solved`() {
    courseWithFiles {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.CORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(1, 0))
    launchAction()
  }

  @Test
  fun `test multiple choice failed`() {
    courseWithFiles {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.CORRECT)) {
          taskFile("text.txt")
        }
      }
    }
    selectOptions(listOf(1))
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Incorrect solution" }
    launchAction()
  }

  @Test
  fun `test custom correct message`() {
    courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
      }
    }

    val message = "You did such a good job!"
    (findTask(0, 0) as ChoiceTask).messageCorrect = message

    selectOptions(listOf(0))
    CheckActionListener.expectedMessage { message }
    launchAction()
  }

  @Test
  fun `test custom incorrect message`() {
    courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
      }
    }

    val message = "Use formula of everything"
    (findTask(0, 0) as ChoiceTask).messageIncorrect = message
    selectOptions(listOf(1))
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { message }
    launchAction()
  }



  private fun selectOptions(optionsToSelect: List<Int>) {
    val task = findTask(0, 0)
    NavigationUtils.navigateToTask(project, task)
    (task as ChoiceTask).selectedVariants.addAll(optionsToSelect)
  }


  private fun launchAction() {
    val task = findTask(0, 0)
    testAction(CheckAction(task.getUICheckLabel()))
  }
}