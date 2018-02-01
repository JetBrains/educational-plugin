package com.jetbrains.edu.learning.taskDescription

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.impl.KeymapManagerImpl
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException

class TaskDescriptionTest : EduTestCase() {
  fun testSimpleImg() {
    doTestImage()
  }

  fun testImgInsideOtherTags() {
    doTestImage()
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

  private fun doTestImage() {
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

  fun testShortcutRendering() {
    val taskText = "You can use &shortcut:OverrideMethods; to override methods"
    val taskTextWithShortcuts = "You can use Ctrl+O to override methods"
    doTestShortcut(taskText, taskTextWithShortcuts)
  }

  fun testSeveralShortcutsRendering() {
    val taskText = "You can use &shortcut:OverrideMethods; to override methods. One more useful shortcut: &shortcut:GotoAction;"
    val taskTextWithShortcuts = "You can use Ctrl+O to override methods. One more useful shortcut: Ctrl+Shift+A"
    doTestShortcut(taskText, taskTextWithShortcuts)
  }

  fun testShortcutInsideTag() {
    val taskText = "You can use <code>&shortcut:OverrideMethods;</code> to override methods. One more useful shortcut: &shortcut:GotoAction;"
    val taskTextWithShortcuts = "You can use Ctrl+O to override methods. One more useful shortcut: Ctrl+Shift+A"
    doTestShortcut(taskText, taskTextWithShortcuts)
  }

  private fun doTestShortcut(taskText: String, taskTextWithShortcuts: String, keymapName: String = "Default for XWin") {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val task = EduUtils.getCurrentTask(project)!!
    task.taskTexts.clear()
    task.addTaskText(EduNames.TASK, taskText)
    val oldActiveKeymap = KeymapManager.getInstance().activeKeymap
    val keymapManager = KeymapManager.getInstance() as KeymapManagerImpl
    keymapManager.activeKeymap = keymapManager.getKeymap(keymapName)
    val taskDescription = task.getTaskDescription(task.getTaskDir(project))
    assertEquals(taskTextWithShortcuts, taskDescription!!.getBody())
    keymapManager.activeKeymap = oldActiveKeymap
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

  private fun String.getBody() = Jsoup.parse(this).getElementsByTag("body").text()

}