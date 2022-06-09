package com.jetbrains.edu.coursecreator.stepik

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import junit.framework.TestCase.assertTrue

class StepikCompareCourseTest : EduTestCase() {

  fun `test the same course`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
          choiceTask(isMultipleChoice = false, choiceOptions = choiceOptions)
        }
      }
    }.asRemote()

    val expectedChangedItems = StepikChangesInfo()
    checkChangedItems(localCourse, localCourse, expectedChangedItems)
  }

  fun `test new lesson`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val newLesson = addNewLesson(project, "lesson2", 2, localCourse, project.courseDir)
    val expectedInfo = StepikChangesInfo(newLessons = arrayListOf(newLesson))

    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test new section`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask { }
          outputTask { }
          theoryTask { }
        }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as EduCourse
    val newSection = addNewSection(project, "section1", 2, localCourse, project.courseDir)
    val expectedInfo = StepikChangesInfo(newSections = arrayListOf(newSection))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change lesson name`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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

  fun `test change lesson custom presentable name`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    localCourse.lessons.single().customPresentableName = "renamed"
    val expectedInfo = StepikChangesInfo(lessonAdditionalInfosToUpdate = arrayListOf(localCourse.lessons.single()))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change lesson index`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    localCourse.init(false)
    val expectedInfo = StepikChangesInfo(lessonsInfoToUpdate = localCourse.lessons.toMutableList())
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change section name`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask { }
          outputTask { }
          theoryTask { }
        }
      }
    }.asRemote()


    val courseFromServer = localCourse.copy() as EduCourse
    val newName = "renamed"
    val changedSection = localCourse.sections.single()
    runWriteAction {
      changedSection.getDir(project.courseDir)!!.rename(this, newName)
    }
    changedSection.name = newName
    val expectedInfo = StepikChangesInfo(sectionInfosToUpdate = arrayListOf(changedSection))

    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change section index`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    localCourse.init(false)
    val expectedInfo = StepikChangesInfo(sectionInfosToUpdate = localCourse.sections.toMutableList())

    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test add new task`() {
    val courseFromServer = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
      }
    }.asRemote()

    val localCourse = course(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    // It also checks that we do not use Stepik Lesson Attachments API (lessonAdditionalInfosToUpdate is empty)
    val expectedInfo = StepikChangesInfo(newTasks = mutableListOf(localCourse.lessons.single().taskList[2]))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test add new non-plugin task`() {
    val courseFromServer = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
      }
    }.asRemote()

    val localCourse = course(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
        videoTask(sources = mapOf())
      }
    }.asRemote()

    val expectedInfo = StepikChangesInfo(newTasks = mutableListOf(localCourse.lessons.single().taskList[1]),
                                         lessonAdditionalInfosToUpdate = localCourse.lessons.toMutableList())
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task name`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList[0]

    val newName = "renamed"
    runWriteAction {
      changedTask.getDir(project.courseDir)!!.rename(this, newName)
    }
    changedTask.name = newName

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change non-plugin task name`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        choiceTask(choiceOptions = choiceOptions) { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val lesson = localCourse.lessons.single()
    val changedTask = lesson.taskList[0]

    val newName = "renamed"
    runWriteAction {
      changedTask.getDir(project.courseDir)!!.rename(this, newName)
    }
    changedTask.name = newName

    val expectedInfo = StepikChangesInfo(lessonAdditionalInfosToUpdate = mutableListOf(lesson),
                                         tasksToUpdate = lesson.taskList.toMutableList())
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change non-plugin task custom name`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        choiceTask(choiceOptions = choiceOptions) { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val lesson = localCourse.lessons.single()
    val changedTask = lesson.taskList[0]

    val newName = "renamed"
    changedTask.customPresentableName = newName

    val expectedInfo = StepikChangesInfo(lessonAdditionalInfosToUpdate = mutableListOf(lesson))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task index`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val lesson = localCourse.lessons.single()
    lesson.items = listOf<StudyItem>(lesson.taskList[2], lesson.taskList[1], lesson.taskList[0])
    localCourse.init(false)

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(lesson.taskList[0], lesson.taskList[2]))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task type`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        videoTask(sources = mapOf())
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val lesson = localCourse.lessons.single()
    val newTask = EduTask("task")
    newTask.id = 1
    lesson.items = listOf<StudyItem>(newTask)
    localCourse.init(false)

    val expectedInfo = StepikChangesInfo(lessonAdditionalInfosToUpdate = localCourse.lessons.toMutableList(),
                                         tasksToUpdate = lesson.taskList.toMutableList())
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test add task file`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    val newFileName = "new.txt"

    runWriteAction {
      changedTask.getDir(project.courseDir)!!.createChildData(this, newFileName)
    }
    changedTask.addTaskFile(newFileName)

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test add task file to choice task`() {
    val choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        choiceTask(isMultipleChoice = false, choiceOptions = choiceOptions)
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList[0]
    val newFileName = "new.txt"

    runWriteAction {
      changedTask.getDir(project.courseDir)!!.createChildData(this, newFileName)
    }
    changedTask.addTaskFile(newFileName)

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask),
                                         lessonAdditionalInfosToUpdate = localCourse.lessons.toMutableList())
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task description`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask { }
        outputTask { }
        theoryTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList[0]

    val taskDescriptionFile = changedTask.getDir(project.courseDir)!!.findChild(TASK_MD)
                              ?: error("Failed to find task description file")

    runWriteAction {
      VfsUtil.saveText(taskDescriptionFile, "new text")
    }
    changedTask.descriptionText = "new text"

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change task file name`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()

    runWriteAction {
      VfsUtil.saveText(findFileInTask(0, 0, "taskFile.txt"), "text")
    }
    changedTask.taskFiles.values.single().text = "text"

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test add placeholder`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile.txt", "text")
        }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    val changedTask = localCourse.lessons.single().taskList.single()

    val taskFile = changedTask.taskFiles.values.single()
    val placeholder = AnswerPlaceholder()
    placeholder.offset = 0
    placeholder.length = 4
    placeholder.placeholderText = "type here"
    placeholder.taskFile = taskFile
    taskFile.addAnswerPlaceholder(placeholder)

    val expectedInfo = StepikChangesInfo(tasksToUpdate = mutableListOf(changedTask))
    checkChangedItems(localCourse, courseFromServer, expectedInfo)
  }

  fun `test change placeholder offset`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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

  fun `test change options in choice task`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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

  fun `test course with hidden solutions`() {
    val localCourse = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse
    localCourse.solutionsHidden = true

    val expectedChangedItems = StepikChangesInfo(isCourseAdditionalInfoChanged = true)
    checkChangedItems(localCourse, courseFromServer, expectedChangedItems)
  }

  fun `test tasks with hidden solution`() {
    val localCourse = course(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") { }
        eduTask("task2") { }
      }
    }.asRemote()

    val courseFromServer = localCourse.copy() as EduCourse

    val localLesson = localCourse.getLesson("lesson1")!!
    localLesson.getTask("task1")!!.solutionHidden = true
    localLesson.getTask("task2")!!.solutionHidden = false

    val expectedChangedItems = StepikChangesInfo(tasksToUpdate = localLesson.taskList.toMutableList())
    checkChangedItems(localCourse, courseFromServer, expectedChangedItems)
  }

  private fun checkChangedItems(localCourse: EduCourse, courseFromServer: EduCourse, expected: StepikChangesInfo) {
    val actual = StepikChangeRetriever(project, localCourse, courseFromServer).getChangedItems()
    assertEquals(message("isCourseAdditionalInfoChanged", expected.isCourseAdditionalInfoChanged),
                 expected.isCourseAdditionalInfoChanged,
                 actual.isCourseAdditionalInfoChanged)
    assertEquals(message("isCourseInfoChanged", expected.isCourseInfoChanged),
                 expected.isCourseInfoChanged,
                 actual.isCourseInfoChanged)

    compareContent(expected.newSections, actual.newSections)
    compareContent(expected.sectionsToDelete, actual.sectionsToDelete)
    compareContent(expected.sectionInfosToUpdate, actual.sectionInfosToUpdate)

    compareContent(expected.newLessons, actual.newLessons)
    compareContent(expected.lessonsToDelete, actual.lessonsToDelete)
    compareContent(expected.lessonsInfoToUpdate, actual.lessonsInfoToUpdate)
    compareContent(expected.lessonAdditionalInfosToUpdate, actual.lessonAdditionalInfosToUpdate)

    compareContent(expected.newTasks, actual.newTasks)
    compareContent(expected.tasksToDelete, actual.tasksToDelete)
    compareContent(expected.tasksToUpdate, actual.tasksToUpdate)

    assertEquals(message("isTopLevelSectionAdded", expected.isTopLevelSectionAdded),
                 expected.isTopLevelSectionAdded,
                 actual.isTopLevelSectionAdded)
    assertEquals(message("isTopLevelSectionNameChanged", expected.isTopLevelSectionNameChanged),
                 expected.isTopLevelSectionNameChanged,
                 actual.isTopLevelSectionNameChanged)
    assertEquals(message("isTopLevelSectionNameChanged", expected.isTopLevelSectionNameChanged),
                 expected.isTopLevelSectionNameChanged,
                 actual.isTopLevelSectionRemoved)
  }

  private fun message(name: String, expectedValue: Boolean) = " `$name` expected to be `$expectedValue`"
}

private fun compareContent(expectedContent: List<StudyItem>, actualContent: List<StudyItem>) {
  val message = "Content mismatch. Expected: ${expectedContent.asStringOfItemNames()}. Actual: ${actualContent.asStringOfItemNames()}"
  assertTrue(message, expectedContent.sameTo(actualContent))
}

private fun List<StudyItem>.asStringOfItemNames() = if (isEmpty()) "empty list" else joinToString(prefix = "[", postfix = "]") { it.name }

private infix fun <T : StudyItem> Collection<T>.sameTo(collection: Collection<T>): Boolean {
  if (collection.size != this.size) return false
  val pairList = collection.zip(this)
  return pairList.all { (elt1, elt2) -> elt1.name == elt2.name }
}

internal fun addNewLesson(
  project: Project,
  name: String,
  index: Int,
  parent: LessonContainer,
  virtualFile: VirtualFile
): Lesson {
  val course = course {
    lesson(name) {
      eduTask {
        taskFile("fizz.kt")
      }
    }
  }

  val newLesson = course.getLesson(name)!!
  newLesson.index = index
  newLesson.init(parent, false)
  parent.addLesson(newLesson)
  if (parent is Course) {
    GeneratorUtils.createLesson(project, newLesson, virtualFile)
  }
  else {
    val sectionDir = virtualFile.findChild(parent.name)
    GeneratorUtils.createLesson(project, newLesson, sectionDir!!)
  }

  return newLesson
}

internal fun addNewSection(
  project: Project,
  name: String,
  index: Int,
  courseToInit: Course,
  virtualFile: VirtualFile
): Section {
  val course = course {
    section(name) {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }
  }

  val newSection = course.getSection(name)!!
  newSection.index = index
  courseToInit.addSection(newSection)
  courseToInit.init(false)
  GeneratorUtils.createSection(project, newSection, virtualFile)
  return newSection
}