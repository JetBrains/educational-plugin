package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import junit.framework.TestCase

class XmlFormatTest: EduTestCase() {
  fun `test files order`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("B.txt")
          taskFile("C.txt")
          taskFile("A.txt")
        }
      }
    }
    val serializedCourse = StudyTaskManager.getInstance(project).serialize()
    val deserializedCourse = StudyTaskManager.getInstance(project).deserialize(serializedCourse)
    val task = (deserializedCourse.items[0] as Lesson).taskList[0]
    TestCase.assertEquals(listOf("B.txt", "C.txt", "A.txt"), task.taskFiles.values.map { it.name })
  }

  fun `test course with choice task`() {
    val expectedChoiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    courseWithFiles {
      lesson {
        choiceTask(choiceOptions = expectedChoiceOptions)
      }
    }
    val serializedCourse = StudyTaskManager.getInstance(project).serialize()
    val deserializedCourse = StudyTaskManager.getInstance(project).deserialize(serializedCourse)
    val task = (deserializedCourse.items[0] as Lesson).taskList[0]
    assertTrue(task is ChoiceTask)
    assertEquals(expectedChoiceOptions, (task as ChoiceTask).choiceOptions.associateBy({ it.text }, { it.status }))
  }
}