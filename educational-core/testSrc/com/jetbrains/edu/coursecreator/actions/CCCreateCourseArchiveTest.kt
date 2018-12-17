package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.GENERATED_FILES_FOLDER
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
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
            testFile("test.py", "some test")
            additionalFile("additional.py", "my test", visible = false)
            additionalFile("visibleAdditional.py", "my test")
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
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a")
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

  private fun loadExpectedJson(): String {
    val fileName = getTestFile()
    return FileUtil.loadFile(File(testDataPath, fileName))
  }

  private fun generateJson(): String {
    val baseDir = myFixture.project.baseDir
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val created = CCCreateCourseArchive.createCourseArchive(myFixture.module, "course",
                                                            myFixture.project.basePath + "/" + GENERATED_FILES_FOLDER,
                                                            false)
    TestCase.assertTrue(created)
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val generated = baseDir.findChild(CCUtils.GENERATED_FILES_FOLDER)
    TestCase.assertNotNull(generated)
    val archive = generated!!.findChild("course.zip")
    TestCase.assertNotNull(archive)
    val courseFolder = generated.findChild("course")
    TestCase.assertNotNull(courseFolder)
    val jsonFile = courseFolder!!.findChild(EduNames.COURSE_META_FILE)
    TestCase.assertNotNull(jsonFile)
    return FileUtil.loadFile(File(jsonFile!!.path))
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/createCourseArchive"
  }

  private fun getTestFile(): String {
    return getTestName(true).trim() + ".json"
  }

}
