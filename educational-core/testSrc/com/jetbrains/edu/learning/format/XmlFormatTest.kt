package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Lesson
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
}