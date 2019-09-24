package com.jetbrains.edu.coursecreator.stepik

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.integration.stepik.addNewLesson
import com.jetbrains.edu.integration.stepik.addNewSection
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask

class StepikCompareCourseTest : EduTestCase() {

  fun `test the same course`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask { }
          outputTask { }
          theoryTask { }
        }
      }
      section {
        lesson {
          eduTask { }
          eduTask { }
        }
      }
    }.asRemote()

    val expectedChangedItems = StepikChangesInfo()
    checkChangedItems(localCourse, localCourse, expectedChangedItems)
  }

  fun `test new lesson`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val newLesson = addNewLesson("lesson2", 2, localCourse, localCourse, project.courseDir)
    val expectedInfo = StepikChangesInfo(newLessons = arrayListOf(newLesson))

    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test new section`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask { }
          outputTask { }
          theoryTask { }
        }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as EduCourse
    val newSection = addNewSection("section1", 2, localCourse, project.courseDir)
    val expectedInfo = StepikChangesInfo(newSections = arrayListOf(newSection))

    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change lesson name`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as EduCourse
    localCourse.lessons.single().name = "renamed"
    val expectedInfo = StepikChangesInfo(lessonsInfoToUpdate = arrayListOf(localCourse.lessons.single()))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change lesson index`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
      lesson {
        eduTask { }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as EduCourse
    localCourse.items = mutableListOf<StudyItem>(localCourse.lessons[1], localCourse.lessons[0])
    val expectedInfo = StepikChangesInfo(lessonsInfoToUpdate = localCourse.lessons)
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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


    val courseFromServer = localCourse.copy() as EduCourse
    localCourse.sections.single().name = "renamed"
    val expectedInfo = StepikChangesInfo(sectionInfosToUpdate = arrayListOf(localCourse.sections.single()))

    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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
      section { }
    }.asRemote()


    val courseFromServer = localCourse.copy() as EduCourse
    localCourse.items = mutableListOf<StudyItem>(localCourse.sections[1], localCourse.sections[0])
    val expectedInfo = StepikChangesInfo(sectionInfosToUpdate = localCourse.sections)

    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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

    val expectedInfo = StepikChangesInfo(newTasks = mutableListOf(localCourse.lessons.single().taskList[2]))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task name`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.name = "renamed"

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task index`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val lesson = localCourse.lessons.single()
    lesson.items = listOf<StudyItem>(lesson.taskList[2], lesson.taskList[1], lesson.taskList[0])

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(lesson.taskList[0], lesson.taskList[2]))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test add task file`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.taskFiles["new.txt"] = TaskFile()

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task description`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    changedTask.descriptionText = "new text"

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task file name`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().name = "renamed.txt"

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task file text`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().setText("text")

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test add placeholder`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().answerPlaceholders.add(0, AnswerPlaceholder())

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.offset = 10

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change placeholder text`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("Task.txt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.placeholderText = "new answer"

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.length = 1

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    val changedPlaceholder = changedTask.taskFiles.values.single().answerPlaceholders.single()
    changedPlaceholder.placeholderDependency = AnswerPlaceholderDependency()

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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
    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().answerPlaceholders.single().hints = listOf("hint1", "hint2")

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
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
    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()
    changedTask.taskFiles.values.single().answerPlaceholders.single().hints = listOf("hint2")

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change options in choice task`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT,
                                                                  "2" to ChoiceOptionStatus.INCORRECT))
      }
    }.asRemote()
    val courseFromServer = localCourse.copy() as EduCourse
    (localCourse.lessons.single().taskList.single() as ChoiceTask).choiceOptions = listOf(ChoiceOption("1", ChoiceOptionStatus.CORRECT),
                                                                                          ChoiceOption("2", ChoiceOptionStatus.CORRECT))
    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(courseFromServer.lessons.single().taskList.single()))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change multiple choice in choice task`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(isMultipleChoice = false, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT,
                                                                   "2" to ChoiceOptionStatus.INCORRECT))
      }
    }.asRemote()
    val courseFromServer = localCourse.copy() as EduCourse
    (localCourse.lessons.single().taskList.single() as ChoiceTask).isMultipleChoice = true
    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(courseFromServer.lessons.single().taskList.single()))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test choice task with changed correct message`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(isMultipleChoice = false, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT,
                                                                   "2" to ChoiceOptionStatus.INCORRECT))
      }
    }.asRemote()
    val courseFromServer = localCourse.copy() as EduCourse
    (localCourse.lessons.single().taskList.single() as ChoiceTask).messageCorrect = "correct"
    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(courseFromServer.lessons.single().taskList.single()))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test choice task with changed incorrect message`() {
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(isMultipleChoice = false, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT,
                                                                   "2" to ChoiceOptionStatus.INCORRECT))
      }
    }.asRemote()
    val courseFromServer = localCourse.copy() as EduCourse
    (localCourse.lessons.single().taskList.single() as ChoiceTask).messageIncorrect = "incorrect"
    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(courseFromServer.lessons.single().taskList.single()))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test choice task nothing changed`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    val localCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(isMultipleChoice = false, choiceOptions = choiceOptions)
      }
    }.asRemote()
    val courseFromServer = localCourse.copy() as EduCourse

    val taskFromRemote = courseFromServer.lessons.single().taskList.single()
    //choice tasks are being renamed when uploading to Stepik
    taskFromRemote.name = "Quiz"

    val expectedInfo = StepikChangesInfo()
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  private fun checkChangedItems(localCourse: EduCourse, courseFromServer: EduCourse, expected: StepikChangesInfo) {
    val actual = StepikChangeRetriever(project, localCourse, courseFromServer).getChangedItems()
    assertEquals(expected.isCourseAttachmentsChanged, actual.isCourseAttachmentsChanged)
    assertEquals(expected.isCourseInfoChanged, actual.isCourseInfoChanged)
    assertTrue(expected.newSections.sameContentWith(actual.newSections))
    assertTrue(expected.sectionsToDelete.sameContentWith(actual.sectionsToDelete))
    assertTrue(expected.sectionInfosToUpdate.sameContentWith(actual.sectionInfosToUpdate))
    assertTrue(expected.newLessons.sameContentWith(actual.newLessons))
    assertTrue(expected.lessonsToDelete.sameContentWith(actual.lessonsToDelete))
    assertTrue(expected.lessonsInfoToUpdate.sameContentWith(actual.lessonsInfoToUpdate))
    assertTrue(expected.newTasks.sameContentWith(actual.newTasks))
    assertTrue(expected.tasksToDelete.sameContentWith(actual.tasksToDelete))
    assertTrue(expected.tasksToUpdate.sameContentWith(actual.tasksToUpdate))
    assertEquals(expected.isTopLevelSectionAdded, actual.isTopLevelSectionAdded)
    assertEquals(expected.isTopLevelSectionNameChanged, actual.isTopLevelSectionNameChanged)
    assertEquals(expected.isTopLevelSectionRemoved, actual.isTopLevelSectionRemoved)
  }
}

infix fun <T : StudyItem> Collection<T>.sameContentWith(collection: Collection<T>): Boolean {
  if (collection.size != this.size) return false
  val pairList = collection.zip(this)
  return pairList.all { (elt1, elt2) -> elt1.name == elt2.name}
}
