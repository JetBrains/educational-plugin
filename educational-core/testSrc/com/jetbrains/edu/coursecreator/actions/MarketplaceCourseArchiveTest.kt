package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.GENERATED_FILES_FOLDER
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Vendor
import java.io.File

class MarketplaceCourseArchiveTest : EduActionTestCase() {

  fun `test vendor with email`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com"}
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.pdf")
    }

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test vendor with url`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; url = "jetbrains.com"}
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.pdf")
    }

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test plugin version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com"}
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.pdf")
    }

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  private fun loadExpectedJson(): String {
    val fileName = getTestFile()
    return FileUtil.loadFile(File(testDataPath, fileName))
  }

  private fun generateJson(): String {
    @Suppress("DEPRECATION")
    val baseDir = myFixture.project.baseDir
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val errorMessage = runWriteAction {
      MarketplaceArchiveCreator(myFixture.project,
                                File("${myFixture.project.basePath}/$GENERATED_FILES_FOLDER", "course.zip")).compute() }
    assertNull(errorMessage)
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val generated = baseDir.findChild(GENERATED_FILES_FOLDER)
    assertNotNull(generated)
    val archive = generated!!.findChild("course.zip")
    assertNotNull(archive)
    val courseFolder = generated.findChild("course")
    assertNotNull(courseFolder)
    val jsonFile = courseFolder!!.findChild(EduNames.COURSE_META_FILE)
    assertNotNull(jsonFile)
    return FileUtil.loadFile(File(jsonFile!!.path), true).replace(Regex("\\n\\n"), "\n")
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/marketplaceCourseArchive"
  }

  private fun getTestFile(): String {
    return getTestName(true).trim() + ".json"
  }

}
