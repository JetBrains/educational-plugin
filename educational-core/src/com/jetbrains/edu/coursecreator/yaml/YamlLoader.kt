package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.loadItem
import com.jetbrains.edu.coursecreator.yaml.format.getChangeApplierForItem
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.IOException
import kotlin.reflect.KClass

/**
 *  Get fully-initialized [StudyItem] object from yaml config file.
 *  Uses [YamlDeserializer.deserializeItem] to deserialize object, than applies changes to existing object, see [loadItem].
 */
object YamlLoader {

  fun loadItem(project: Project, configFile: VirtualFile, editor: Editor? = configFile.getEditor(project)) {
    if (editor != null) {
      if (editor.headerComponent is InvalidFormatPanel) {
        editor.headerComponent = null
      }
    }

    try {
      doLoad(project, configFile)
    }
    catch (e: MissingKotlinParameterException) {
      val parameterName = e.parameter.name
      if (parameterName == null) {
        showError(project, configFile, editor)
      }
      else {
        showError(project, configFile, editor,
                  "${NameUtil.nameToWordsLowerCase(parameterName).joinToString("_")} is empty")
      }
    }
    catch (e: MismatchedInputException) {
      showError(project, configFile, editor)
    }
    catch (e: InvalidYamlFormatException) {
      showError(project, configFile, editor, e.message.capitalize())
    }
    catch (e: IllegalStateException) {
      showError(project, configFile, editor)
    }
    catch (e: IOException) {
      val causeException = e.cause
      if (causeException?.message == null || causeException !is InvalidYamlFormatException) {
        showError(project, configFile, editor)
      }
      else {
        showError(project, configFile, editor, causeException.message.capitalize())
      }
    }
  }

  private fun doLoad(project: Project, configFile: VirtualFile) {
    val existingItem = getStudyItemForConfig(project, configFile)
    val deserializedItem = YamlDeserializer.deserializeItem(VfsUtil.loadText(configFile), configFile.name)

    if (existingItem == null) {
      // tis code is called if item wasn't loaded because of broken config
      // and now if config fixed, we'll add item to a parent
      val itemDir = configFile.parent
      deserializedItem.name = itemDir.name
      val parentItem = deserializedItem.getParentItem(project, itemDir.parent)
      if (parentItem.items.map { it.name }.contains(itemDir.name)) {
        parentItem.addItemAsNew(project, deserializedItem)
      }
      return
    }
    if (existingItem.itemType != deserializedItem.itemType) {
      unexpectedItemError(existingItem::class, deserializedItem)
    }
    existingItem.applyChanges(project, deserializedItem)
  }

  fun ItemContainer.addItemAsNew(project: Project, deserializedItem: StudyItem) {
    if (deserializedItem is ItemContainer) {
      deserializedItem.init(course, this, false)
      deserializedItem.items = deserializedItem.deserializeContent(project, deserializedItem.items)
      // set parent to deserialize content correctly
      deserializedItem.items.forEach { it.init(course, deserializedItem, false) }
      deserializedItem.items.filterIsInstance(ItemContainer::class.java).forEach {
        it.items = it.deserializeContent(project, it.items)
      }
    }
    val mutableItemsList = items.toMutableList()
    mutableItemsList.add(deserializedItem)
    items = mutableItemsList
    init(course, this, false)
  }

  private fun StudyItem.getParentItem(project: Project, parentDir: VirtualFile): ItemContainer {
    return when (this) {
             is Section -> findCourse(project)
             is Lesson -> {
               val section = findSection(project, parentDir)
               return section ?: findCourse(project)
             }
             is Task -> findLesson(project, parentDir)
             else -> unexpectedItemError(StudyItem::class, this)
           } ?: itemNotFound(parentDir.name)
  }

  private fun <T : StudyItem> T.applyChanges(project: Project, deserializedItem: T) {
    getChangeApplierForItem(project, deserializedItem).applyChanges(this, deserializedItem)
  }

  private fun getStudyItemForConfig(project: Project, configFile: VirtualFile): StudyItem? {
    val name = configFile.name
    val itemDir = configFile.parent ?: error("Cannot find containing item dir for config: $name. Config file is a root directory.")

    return when (name) {
      YamlFormatSettings.COURSE_CONFIG -> findCourse(project)
      YamlFormatSettings.SECTION_CONFIG -> findSection(project, itemDir)
      YamlFormatSettings.LESSON_CONFIG -> findLesson(project, itemDir)
      YamlFormatSettings.TASK_CONFIG -> findTask(project, itemDir)
      else -> unknownConfigError(name)
    }
  }

  private fun findCourse(project: Project): Course {
    return StudyTaskManager.getInstance(project).course ?: error("Unable to find ${EduNames.COURSE}")
  }

  private fun findSection(project: Project, sectionDir: VirtualFile): Section? {
    return EduUtils.getSection(sectionDir, findCourse(project))
  }

  private fun findTask(project: Project, taskDir: VirtualFile): Task? {
    return EduUtils.getTask(taskDir, findCourse(project))
  }

  private fun findLesson(project: Project, lessonDir: VirtualFile): Lesson? {
    val course = findCourse(project)
    return EduUtils.getLesson(lessonDir, course)
  }

  private fun showError(project: Project,
                        configFile: VirtualFile,
                        editor: Editor?,
                        cause: String = "invalid config") {
    if (editor != null) {
      editor.headerComponent = InvalidFormatPanel(cause)
    }
    else {
      val notification = InvalidConfigNotification(project, configFile, cause)
      notification.notify(project)
    }
  }

  private fun VirtualFile.getEditor(project: Project): Editor? {
    val selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(this)
    return if (selectedEditor is TextEditor) selectedEditor.editor else null
  }

  private fun unknownConfigError(name: String): Nothing = error("Unknown config file: $name")

  fun taskDirNotFoundError(itemName: String): Nothing = error("Cannot find directory for a task: $itemName")

  private fun itemNotFound(itemName: String?): Nothing = error("Item not found: '$itemName'")

  private fun <T : StudyItem> unexpectedItemError(expected: KClass<T>, actual: Any): Nothing = error(
    "Expected ${expected.simpleName} class, but was: ${actual.javaClass.simpleName}")
}
