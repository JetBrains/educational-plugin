package com.jetbrains.edu.learning

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException

class TaskDescriptionToolWindowTest : EduTestCase() {
  fun testSimpleImg() {
    doTest()
  }

  fun testImgInsideOtherTags() {
    doTest()
  }

  fun testWebImage() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val name = getTestName(true) + ".html"
    val fileText = FileUtil.loadFile(File(testDataPath, name))
    val task = EduUtils.getCurrentTask(project)
    val taskDir = task?.getTaskDir(project)
    val initialDocument = Jsoup.parse(fileText)

    val processedText = BrowserWindow.processContent(fileText, taskDir!!, project)
    val processedDocument = Jsoup.parse(processedText)

    val initialImgElements = initialDocument.getElementsByTag("img")
    val processedImageElements = processedDocument.getElementsByTag("img")
    processedImageElements.zip(initialImgElements)
    initialImgElements.zip(processedImageElements).map { assert(it.first.attr("src") == it.second.attr("src") ) }
  }

  private fun doTest() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val name = getTestName(true) + ".html"
    val fileText = FileUtil.loadFile(File(testDataPath, name))
    val task = EduUtils.getCurrentTask(project)
    val taskDir = task?.getTaskDir(project)
    val processedText = BrowserWindow.processContent(fileText, taskDir!!, project)
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