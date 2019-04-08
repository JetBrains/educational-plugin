package com.jetbrains.edu.learning

import com.jetbrains.edu.coursecreator.CCUtils
import junit.framework.TestCase

class HintsToTaskDescriptionMigrationTest : EduTestCase() {

  fun `test one placeholder hint`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Tests.java", "<p>test</p>") {
            placeholder(0, hints = listOf("hint"))
          }
        }
      }
    }
    val task = course.findTask("lesson1", "task1")

    val placeholder = task.taskFiles.values.first().answerPlaceholders.first()
    val hint = placeholder.hints.first()
    EduProjectComponent.getInstance(project).moveHintsToTaskDescription(course)

    TestCase.assertEquals("Placeholder hints must be deleted", placeholder.hints.size, 0)

    assertTrue("Task text doesn't contain hint: ${task.descriptionText}", task.descriptionText.contains("<div class='hint'>$hint</div>"))
  }

  fun `test multiple placeholder hints`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>test</p>") {
            placeholder(0, hints = listOf("hint1", "hint2"))
          }
        }
      }
    }
    val task = course.findTask("lesson1", "task1")

    val placeholder = task.taskFiles.values.first().answerPlaceholders.first()
    val hints = placeholder.hints
    EduProjectComponent.getInstance(project).moveHintsToTaskDescription(course)

    TestCase.assertEquals("Placeholder hints must be deleted", placeholder.hints.size, 0)

    for (hint in hints) {
      assertTrue("Task text doesn't contain hint: ${task.descriptionText}", task.descriptionText.contains("<div class='hint'>$hint</div>"))
    }
  }

  fun `test placeholders hints`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>test</p>") {
            placeholder(0, hints = listOf("hint1"))
            placeholder(0, hints = listOf("hint2"))
          }
        }
      }
    }
    val task = course.findTask("lesson1", "task1")

    val answerPlaceholders = task.taskFiles.values.first().answerPlaceholders
    val hints = answerPlaceholders.flatMap { it.hints }
    EduProjectComponent.getInstance(project).moveHintsToTaskDescription(course)

    for (placeholder in answerPlaceholders) {
      TestCase.assertEquals("Placeholder hints must be deleted", placeholder.hints.size, 0)
    }

    for (hint in hints) {
      assertTrue("Task text doesn't contain hint: ${task.descriptionText}", task.descriptionText.contains("<div class='hint'>$hint</div>"))
    }
  }
}