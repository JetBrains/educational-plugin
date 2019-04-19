package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.Experiments
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCProjectComponent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.configFileName
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import junit.framework.TestCase

class YamlSynchronizationTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. To hack this, I enable `yaml` afterwards, so this method does nothing
    createCCCourseWithDescription()
    Experiments.setFeatureEnabled(EduExperimentalFeatures.YAML_FORMAT, true)

    // we need to specify the file type for yaml files as otherwise they are recognised as binaries and aren't allowed to be edited
    // we don't add dependency on yaml plugin because it's impossible to add for tests only and we don't want to have redundant dependency
    // in production code
    runWriteAction { FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE, "yaml") }
  }

  fun `test create config files`() {
    project.getComponent(CCProjectComponent::class.java).projectOpened()
    FileDocumentManager.getInstance().saveAllDocuments()
    checkConfigsExistAndNotEmpty(StudyTaskManager.getInstance(project).course!!)
  }

  fun `test invalid config file`() {

    YamlFormatSynchronizer.saveAll(project)
    FileDocumentManager.getInstance().saveAllDocuments()

    // make task config invalid
    val section = StudyTaskManager.getInstance(project).course!!.sections.first()
    val lesson = section.lessons.first()
    val task = lesson.taskList.first()
    val taskConfig = task.getDir(project)?.findChild(YamlFormatSettings.TASK_CONFIG)!!
    runWriteAction { VfsUtil.saveText(taskConfig, "invalid text") }

    // load course from config
    // TODO: remove try-catch after merge
    try {
      project.getComponent(CCProjectComponent::class.java).projectOpened()
    }
    catch (e: Exception) {
    }

    // check loaded task is null
    val loadedSection = StudyTaskManager.getInstance(project).course?.sections?.first()
    val loadedLesson = loadedSection?.lessons?.first()
    // TODO: uncomment after merge
    // val loadedTask = loadedLesson?.taskList?.firstOrNull()
    // assertNull(loadedTask)

    // check that config file wasn't overwritten
    val taskDir = loadedLesson!!.getDir(project)?.findChild("task1")
    val loadedTaskConfig = taskDir?.findChild(YamlFormatSettings.TASK_CONFIG)!!
    TestCase.assertEquals(VfsUtil.loadText(loadedTaskConfig), "invalid text")
  }

  private fun createCCCourseWithDescription(): Course {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask()
        }
      }
    }
    course.description = "test"
    return course
  }

  private fun checkConfigsExistAndNotEmpty(course: Course) {
    course.items.forEach { courseItem ->
      checkConfig(courseItem)

      // checking sections/top-level lessons
      (courseItem as ItemContainer).items.forEach {
        checkConfig(it)
        if (it is Lesson) {
          it.items.forEach { task -> checkConfig(task) }
        }
      }
    }

  }

  private fun checkConfig(item: StudyItem) {
    val itemDir = item.getDir(project)
    val configFileName = item.configFileName
    val configFile = itemDir.findChild(configFileName)
    assertNotNull("Config file shouldn't be null", configFile)
    assertTrue("Config file should not be empty: ${configFile!!.name}", VfsUtil.loadText(configFile).isNotEmpty())
  }
}