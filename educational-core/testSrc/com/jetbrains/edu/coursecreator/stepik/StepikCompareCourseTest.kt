package com.jetbrains.edu.coursecreator.stepik

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.TaskFile

class StepikCompareCourseTest : EduTestCase() {

  fun `test the same course`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
      section {
        lesson {
          eduTask { }
          eduTask { }
        }
      }
    }.asRemote()

    val expectedChangedItems = StepikChangesInfo()
    checkChangedItems(localCourse, expectedChangedItems)
  }

  fun `test new lesson`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val newLesson = addNewLesson("lesson2", 2, localCourse, localCourse, EduUtils.getCourseDir(project))
    val expectedInfo = StepikChangesInfo(newLessons = arrayListOf(newLesson))

    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test new section`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as RemoteCourse
    val newSection = addNewSection("section1", 2, localCourse, EduUtils.getCourseDir(project))
    val expectedInfo = StepikChangesInfo(newSections = arrayListOf(newSection), newLessons = newSection.lessons)

    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change lesson name`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as RemoteCourse
    localCourse.lessons.single().name = "renamed"
    val expectedInfo = StepikChangesInfo(lessonsInfoToUpdate = arrayListOf(localCourse.lessons.single()))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change lesson index`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as RemoteCourse
    localCourse.lessons.single().index = 2
    val expectedInfo = StepikChangesInfo(lessonsInfoToUpdate = arrayListOf(localCourse.lessons.single()))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change section name`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask { }
          outputTask { }
          theoryTask { }
        }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as RemoteCourse
    localCourse.sections.single().name = "renamed"
    val expectedInfo = StepikChangesInfo(sectionInfosToUpdate = arrayListOf(localCourse.sections.single()))

    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change section index`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask { }
          outputTask { }
          theoryTask { }
        }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as RemoteCourse
    localCourse.sections.single().index = 2
    val expectedInfo = StepikChangesInfo(sectionInfosToUpdate = arrayListOf(localCourse.sections.single()))

    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test add new task`() {
    val courseFromServer = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
      }
    }.asRemote()

    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val expectedInfo = StepikChangesInfo(tasksToPostByLessonIndex = mapOf(1 to listOf(localCourse.lessons.single().taskList[2])))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change task name`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.name = "renamed"

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change task index`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.index = 2

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test add task file`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.taskFiles["new.txt"] = TaskFile()

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change task description`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.descriptionText = "new text"

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test add additional file`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.additionalFiles["file.txt"] = "additional file"

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change task file name`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().name = "renamed.txt"

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change task file text`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().text = "text"

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test add placeholder`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().answerPlaceholders.add(0, AnswerPlaceholder())

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change placeholder offset`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.offset = 10

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change placeholder answer`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.possibleAnswer = "new answer"

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change placeholder length`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.length = 1

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test change placeholder index`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.index = 2

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test add placeholder dependency`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.placeholderDependency = AnswerPlaceholderDependency()

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test hints size`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    localCourse.lessons.single().taskList.single().taskFiles.values.single().answerPlaceholders.single().hints = listOf("hint1")
    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().answerPlaceholders.single().hints = listOf("hint1", "hint2")

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test hints value`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    localCourse.lessons.single().taskList.single().taskFiles.values.single().answerPlaceholders.single().hints = listOf("hint1")
    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().answerPlaceholders.single().hints = listOf("hint2")

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test tests text size`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
        }
      }
    }.asRemote()

    localCourse.lessons.single().taskList.single().testsText = mapOf("test1" to "test text")
    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.testsText = mapOf("test1" to "test text", "test2" to "test text")

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test tests texts`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
        }
      }
    }.asRemote()

    localCourse.lessons.single().taskList.single().testsText = mapOf("test1" to "text text")
    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.testsText = mapOf("test1" to "text text changed")

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }

  fun `test tests names`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
        }
      }
    }.asRemote()

    localCourse.lessons.single().taskList.single().testsText = mapOf("test1" to "text text")
    val courseFromServer = localCourse.copy() as RemoteCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.testsText = mapOf("test1-changed" to "text text")

    val expectedInfo = StepikChangesInfo(tasksToUpdateByLessonIndex = mapOf(1 to listOf(changedTask)))
    checkChangedItems(courseFromServer, expectedInfo)
  }
  private fun checkChangedItems(courseFromServer: RemoteCourse, expected: StepikChangesInfo) {
    val actual = StepikChangeRetriever(project, courseFromServer).getChangedItems()
    assertEquals(expected, actual)
  }
}