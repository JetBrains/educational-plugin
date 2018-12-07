package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import junit.framework.TestCase
import java.io.File

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
    val created = CCCreateCourseArchive.createCourseArchive(myFixture.module, "course", myFixture.project.basePath,
                                                            false)
    TestCase.assertTrue(created)
    val baseDir = myFixture.project.baseDir
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val archive = baseDir.findChild("course.zip")
    TestCase.assertNotNull(archive)
    val generated = baseDir.findChild(CCUtils.GENERATED_FILES_FOLDER)
    TestCase.assertNotNull(generated)
    val courseFolder = generated!!.findChild("course")
    TestCase.assertNotNull(courseFolder)
    val jsonFile = courseFolder!!.findChild(EduNames.COURSE_META_FILE)
    TestCase.assertNotNull(jsonFile)
    val fileName = getTestFile()
    val expectedCourseJson = FileUtil.loadFile(File(testDataPath, fileName))
    TestCase.assertEquals(expectedCourseJson, FileUtil.loadFile(File(jsonFile!!.path)))
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/createCourseArchive"
  }

  private fun getTestFile(): String {
    return getTestName(true).trim() + ".json"
  }

}
