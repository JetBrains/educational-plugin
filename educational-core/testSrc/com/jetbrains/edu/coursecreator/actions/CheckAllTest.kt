package com.jetbrains.edu.coursecreator.actions

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.withTestDialog

class CheckAllTest : EduActionTestCase() {
  fun `test all solved`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          checkResultFile(CheckStatus.Solved)
        }
      }
    }

    withTestDialog(EduTestDialog()) {
      testAction(dataContext(emptyArray()), CheckAllTasks())
    }.checkWasShown(CheckAllTasks.SUCCESS_MESSAGE)
  }

  fun `test failed tasks`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          checkResultFile(CheckStatus.Failed)
        }
      }
    }

    withTestDialog(EduTestDialog()) {
      testAction(dataContext(emptyArray()), CheckAllTasks())
    }.checkWasShown(CheckAllTasks.FAILED_MESSAGE)
  }
}