package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.configFileName
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem

class AddNewItemYamlTest : YamlTestCase() {

  override fun setUp() {
    super.setUp()
    // as we don't create config file in CCProjectComponent, we have to create them manually
    createConfigFiles()
  }

  private fun createConfigFiles() {
    YamlFormatSynchronizer.saveAll(project)
    FileDocumentManager.getInstance().saveAllDocuments()
  }

  override fun createCourse() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("test1.txt")
          taskFile("test2.txt")
        }
        eduTask("task2")
      }
      lesson("lesson2") {
        eduTask("task1")
      }
      section("section1") {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
      section("section2") {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
    }
    course.description = "test"
  }

  fun `test new task file added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val task = course.lessons.first().taskList.first()
    task.taskFiles.remove("test2.txt")

    val configFile = task.getDir(project)!!.findChild(task.configFileName)!!
    YamlLoader.loadItem(project, configFile, null)

    assertEquals(2, task.taskFiles.size)
  }

  fun `test unexpected task file isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val task = course.lessons.first().taskList.first()
    task.taskFiles.remove("test2.txt")

    YamlFormatSynchronizer.saveItem(task)
    FileDocumentManager.getInstance().saveAllDocuments()

    val configFile = task.getDir(project)!!.findChild(task.configFileName)!!
    YamlLoader.loadItem(project, configFile, null)

    assertEquals(1, task.taskFiles.size)
  }

  fun `test new task added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doAddedTest(course.lessons.first())
  }

  fun `test new unexpected task isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doNotAddedTest(course.lessons.first())
  }

  fun `test new lesson added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doAddedTest(course)
  }

  fun `test new lesson content added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val section = course.sections.first()
    doAddedTest(section)
    val lesson = section.items.last() as Lesson
    assertEquals(1, lesson.taskList.size)
  }

  fun `test new unexpected lesson isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doNotAddedTest(course)
  }

  fun `test new section added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doAddedTest(course)
  }

  fun `test missing section added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doToParentAddedTest(course)
  }

  fun `test new section content added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doAddedTest(course)
  }

  fun `test new unexpected section isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!

    doNotAddedTest(course)
  }

  private fun ItemContainer.removeLastItem() {
    val itemsCopy = items.toMutableList()
    itemsCopy.removeAt(items.lastIndex)
    items = itemsCopy
  }

  private fun doAddedTest(itemContainer: ItemContainer) {
    val expectedSize = itemContainer.items.size
    itemContainer.removeLastItem()

    val configFile = itemContainer.getDir(project)!!.findChild(itemContainer.configFileName)!!
    YamlLoader.loadItem(project, configFile, null)

    assertEquals(expectedSize, itemContainer.items.size)
  }

  private fun doNotAddedTest(itemContainer: ItemContainer) {
    val expectedSize = itemContainer.items.size - 1
    itemContainer.removeLastItem()

    YamlFormatSynchronizer.saveItem(itemContainer)
    FileDocumentManager.getInstance().saveAllDocuments()

    loadParent(itemContainer)

    assertEquals(expectedSize, itemContainer.items.size)
  }

  private fun loadParent(parentItem: StudyItem) {
    val lessonConfigFile = parentItem.getDir(project)!!.findChild(parentItem.configFileName)!!
    YamlLoader.loadItem(project, lessonConfigFile, null)
  }

}