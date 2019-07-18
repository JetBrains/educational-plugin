package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.GENERATED_FILES_FOLDER
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.coursera.CourseraCourse
import junit.framework.TestCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CCCreateCourseArchiveTest : EduActionTestCase() {

  fun `test local course archive`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test coursera course archive`() {
    val course = courseWithFiles(courseProducer = ::CourseraCourse, courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    } as CourseraCourse
    course.submitManually = false
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test coursera course archive submit manually`() {
    val course = courseWithFiles(courseProducer = ::CourseraCourse, courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    } as CourseraCourse
    course.submitManually = true
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test local course with author`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    course.setAuthorsAsString(arrayOf("EduTools Dev", "EduTools QA", "EduTools"))
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test framework lesson archive`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson("my lesson") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test sections`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test custom files`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
            taskFile("test.py", "some test")
            taskFile("additional.py", "my test", visible = false)
            taskFile("visibleAdditional.py", "my test")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test remote course archive`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }.asRemote()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = dateFormat.parse("Jan 01, 1970 03:00:00 AM")
    course.updateDate = date
    for (lesson in course.lessons) {
      lesson.updateDate = date
      for (task in lesson.taskList) {
        task.updateDate = date
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test placeholder dependencies`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """) {
            placeholder(0, dependency = "lesson1#task1#fizz.kt#1")
            placeholder(1, dependency = "lesson1#task1#fizz.kt#2")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test course additional files`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
      }
      additionalFiles {
        taskFile("additional.txt", "file text")
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test course with choice tasks`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("task.txt")
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test task with custom name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    val task = course.lessons.first().taskList.first()
    task.customPresentableName = "custom name"

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  private fun loadExpectedJson(): String {
    val fileName = getTestFile()
    return FileUtil.loadFile(File(testDataPath, fileName))
  }

  private fun generateJson(): String {
    val baseDir = myFixture.project.baseDir
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val created = CCCreateCourseArchive.createCourseArchive(myFixture.project, "course",
                                                            myFixture.project.basePath + "/" + GENERATED_FILES_FOLDER,
                                                            false)
    TestCase.assertTrue(created)
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val generated = baseDir.findChild(GENERATED_FILES_FOLDER)
    TestCase.assertNotNull(generated)
    val archive = generated!!.findChild("course.zip")
    TestCase.assertNotNull(archive)
    val courseFolder = generated.findChild("course")
    TestCase.assertNotNull(courseFolder)
    val jsonFile = courseFolder!!.findChild(EduNames.COURSE_META_FILE)
    TestCase.assertNotNull(jsonFile)
    return FileUtil.loadFile(File(jsonFile!!.path), true).replace(Regex("\\n\\n"), "\n")
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/createCourseArchive"
  }

  private fun getTestFile(): String {
    return getTestName(true).trim() + ".json"
  }

}
