package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduNames.COURSE_META_FILE
import junit.framework.TestCase
import java.io.File

class CCLocalCourseJsonTest : CCActionTestCase() {

  fun `test course without sections`() {
    val course = courseWithFiles {
      lesson {
       eduTask {
         taskFile("tmp.py")
       }
      }
    }
    runWriteAction { CCCreateCourseArchive.generateJson(LightPlatformTestCase.getSourceRoot(), course) }
    val courseJson = VfsUtil.loadText(LightPlatformTestCase.getSourceRoot().findChild(COURSE_META_FILE)!!)
    val expectedJson = FileUtil.loadFile(File(testDataPath, "courseWithoutSection.json"))
    TestCase.assertEquals(expectedJson, courseJson)
  }

  fun `test course with sections`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("tmp.py")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("tmp2.py")
        }
      }
    }
    runWriteAction { CCCreateCourseArchive.generateJson(LightPlatformTestCase.getSourceRoot(), course) }
    val courseJson = VfsUtil.loadText(LightPlatformTestCase.getSourceRoot().findChild(COURSE_META_FILE)!!)
    val expectedJson = FileUtil.loadFile(File(testDataPath, "courseWithSection.json"))
    TestCase.assertEquals(expectedJson, courseJson)
  }

  fun `test framework lesson`() {
    val course = courseWithFiles {
      lesson(isFramework = true) {
        eduTask {
          taskFile("tmp.py")
        }
      }
    }
    runWriteAction { CCCreateCourseArchive.generateJson(LightPlatformTestCase.getSourceRoot(), course) }
    val courseJson = VfsUtil.loadText(LightPlatformTestCase.getSourceRoot().findChild(COURSE_META_FILE)!!)
    val expectedJson = FileUtil.loadFile(File(testDataPath, "frameworkLesson.json"))
    TestCase.assertEquals(expectedJson, courseJson)
  }

  override fun getTestDataPath(): String {
    return "testData/coursecreator/actions/localCourseJson"
  }
}
