package com.jetbrains.edu.yaml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.configFileName

class YamlRenameTest : YamlCodeInsightTest() {

  override fun setUp() {
    super.setUp()
    createConfigFiles(project)
  }

  override fun createCourse() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
  }

  fun `test rename lesson`() {
    val lesson = getCourse().getLesson("lesson1")!!
    val dir = lesson.getDir(project)!!
    val psiDir = PsiManager.getInstance(project).findDirectory(dir)!!

    doTest(psiDir, "lesson2", lesson.parent, """
      |title: Test Course
      |language: English
      |programming_language: Plain text
      |content:
      |- lesson2
      |
    """.trimMargin("|"))
  }

  fun `test rename task`() {
    val task = getCourse().findTask("lesson1", "task1")
    val dir = task.getDir(project)!!
    val psiDir = PsiManager.getInstance(project).findDirectory(dir)!!

    doTest(psiDir, "task2", task.parent, """
      |content:
      |- task2
      |
    """.trimMargin("|"))
  }

  fun `test rename task file`() {
    val task = getCourse().findTask("lesson1", "task1")
    val taskFile = task.getTaskFile("src/taskfile1.txt")!!
    val file = taskFile.getVirtualFile(project)!!
    val psiFile = PsiManager.getInstance(project).findFile(file)!!

    doTest(psiFile, "taskfile2.txt", task, """
      |type: edu
      |files:
      |- name: src/taskfile2.txt
      |  visible: true
      |
    """.trimMargin("|"))
  }

  private fun doTest(element: PsiElement, newName: String, parentItem: StudyItem, text: String) {
    myFixture.renameElement(element, newName)
    openConfigFile(parentItem)
    myFixture.checkResult(text)
  }

  private fun openConfigFile(item: StudyItem) {
    val configFile = item.getDir(project).findChild(item.configFileName)!!
    myFixture.openFileInEditor(configFile)
  }
}
