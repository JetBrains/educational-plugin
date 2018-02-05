package com.jetbrains.edu.learning

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.testFramework.VfsTestUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow
import org.jsoup.Jsoup
import java.io.IOException

class TaskDescriptionToolWindowTest : EduTestCase() {
  fun testSimpleImg() {
    doTest()
  }

  fun testImgInsideOtherTags() {
    doTest()
  }

  private fun doTest() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val name = getTestName(true)
    val vFile = VfsTestUtil.findFileByCaseSensitivePath("$testDataPath/$name.html")

    val fileText = StringUtil.convertLineSeparators(VfsUtilCore.loadText(vFile))
    val task = EduUtils.getCurrentTask(project)
    val taskDir = task?.getTaskDir(project)
    val processedText = BrowserWindow(project, false, false).processContent(fileText, taskDir!!)
    val document = Jsoup.parse(processedText)
    val imageElements = document.getElementsByTag("img")
    imageElements
      .map { assert(BrowserUtil.isAbsoluteURL(it.attr("src"))) }
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    val course = Course()
    course.name = "Edu test course"
    course.language = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(myFixture.project).course = course

    val lesson1 = createLesson(1, 1)
    course.addLesson(lesson1)
    course.initCourse(false)

  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/toolWindow"
  }
}