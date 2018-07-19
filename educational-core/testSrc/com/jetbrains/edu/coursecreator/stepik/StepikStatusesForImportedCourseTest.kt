package com.jetbrains.edu.coursecreator.stepik

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus.*
import com.jetbrains.edu.learning.courseFormat.TaskFile

class StepikStatusesForImportedCourseTest : EduTestCase() {
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

    StepikChangeRetriever(project, localCourse).setStepikChangeStatuses()
    checkOtherItemsUpToDate(localCourse)
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
    addNewLesson("lesson2", 2, localCourse, localCourse, EduUtils.getCourseDir(project))
    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(localCourse, CONTENT)
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
    addNewSection("section1", 2, localCourse, EduUtils.getCourseDir(project))

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(localCourse, CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(localCourse.lessons.single(), INFO)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(localCourse.lessons.single(), INFO)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(localCourse.sections.single(), INFO)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(localCourse.sections.single(), INFO)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(localCourse.lessons.single(), CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
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

    StepikChangeRetriever(project, courseFromServer).setStepikChangeStatuses()
    checkStatus(changedTask, INFO_AND_CONTENT)
  }
}