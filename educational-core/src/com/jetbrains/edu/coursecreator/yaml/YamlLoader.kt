package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.saveItem
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.loadItem
import com.jetbrains.edu.coursecreator.yaml.format.getChangeApplierForItem
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import java.io.IOException

/**
 *  Get fully-initialized [StudyItem] object from yaml config file.
 *  Uses [YamlDeserializer.deserializeItem] to deserialize object, than applies changes to existing object, see [loadItem].
 */
object YamlLoader {

  fun loadItem(project: Project, configFile: VirtualFile) {
    val editor = configFile.getEditor(project)
    if (editor != null) {
      if (editor.headerComponent is InvalidFormatPanel) {
        editor.headerComponent = null
      }
    }
    try {
      doLoad(project, configFile)
    }
    catch (e: Exception) {
      when (e) {
        is YamlIllegalStateException -> YamlDeserializer.showError(project, e, configFile, e.message.capitalize())
        else -> throw e
      }
    }
  }

  @VisibleForTesting
  fun doLoad(project: Project, configFile: VirtualFile) {
    val existingItem = getStudyItemForConfig(project, configFile)
    val deserializedItem = YamlDeserializer.deserializeItem(configFile) ?: return
    deserializedItem.ensureChildrenExist(configFile.parent)

    if (existingItem == null) {
      // tis code is called if item wasn't loaded because of broken config
      // and now if config fixed, we'll add item to a parent
      if (deserializedItem is Course) {
        StudyTaskManager.getInstance(project).course = YamlDeepLoader.loadCourse(project)
        return
      }
      val itemDir = configFile.parent
      deserializedItem.name = itemDir.name
      val parentItem = deserializedItem.getParentItem(project, itemDir.parent)
      val parentConfig = parentItem.getDir(project).findChild(parentItem.configFileName) ?: return
      val deserializedParent = YamlDeserializer.deserializeItem(parentConfig) as? ItemContainer ?: return
      if (deserializedParent.items.map { it.name }.contains(itemDir.name)) {
        parentItem.addItemAsNew(project, deserializedItem)
        // new item is added at the end, so we should save parent item to update items order in config file
        saveItem(parentItem)
      }
      return
    }
    if (existingItem.itemType != deserializedItem.itemType) {
      yamlIllegalStateError("Expected ${existingItem::class.simpleName} class, but was: ${deserializedItem.javaClass.simpleName}")
    }
    existingItem.applyChanges(project, deserializedItem)
  }

  private fun ItemContainer.addItemAsNew(project: Project, deserializedItem: StudyItem) {
    deserializedItem.deserializeChildrenIfNeeded(project, course)
    addItem(deserializedItem)
    init(course, this, false)
  }

  fun StudyItem.deserializeChildrenIfNeeded(project: Project, course: Course) {
    if (this !is ItemContainer) {
      return
    }
    init(course, this, false)
    items = deserializeContent(project, items)
    // set parent to deserialize content correctly
    items.forEach { it.init(course, this, false) }
    items.filterIsInstance(ItemContainer::class.java).forEach {
      it.items = it.deserializeContent(project, it.items)
    }
  }

  private fun StudyItem.getParentItem(project: Project, parentDir: VirtualFile): ItemContainer {
    val course = StudyTaskManager.getInstance(project).course
    return when (this) {
             is Section -> course
             is Lesson -> {
               val section = course?.let { EduUtils.getSection(parentDir, course) }
               section ?: course
             }
             is Task -> course?.let { EduUtils.getLesson(parentDir, course) }
             else -> yamlIllegalStateError(
               "Unexpected item type. Expected: 'Section', 'Lesson' or 'Task'. Was '${itemType}'")
           } ?: yamlIllegalStateError(notFoundMessage("parent", "for item '${name}'"))
  }

  private fun <T : StudyItem> T.applyChanges(project: Project, deserializedItem: T) {
    getChangeApplierForItem(project, deserializedItem).applyChanges(this, deserializedItem)
  }

  private fun getStudyItemForConfig(project: Project, configFile: VirtualFile): StudyItem? {
    val name = configFile.name
    val itemDir = configFile.parent ?: error(notFoundMessage("containing item dir", name))
    val course = StudyTaskManager.getInstance(project).course ?: return null
    return when (name) {
      YamlFormatSettings.COURSE_CONFIG -> course
      YamlFormatSettings.SECTION_CONFIG -> EduUtils.getSection(itemDir, course)
      YamlFormatSettings.LESSON_CONFIG -> EduUtils.getLesson(itemDir, course)
      YamlFormatSettings.TASK_CONFIG -> EduUtils.getTask(itemDir, course)
      else -> yamlIllegalStateError(unknownConfigMessage(name))
    }
  }

  fun VirtualFile.getEditor(project: Project): Editor? {
    val selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(this)
    return if (selectedEditor is TextEditor) selectedEditor.editor else null
  }
}

private fun StudyItem.ensureChildrenExist(itemDir: VirtualFile) {
  when (this) {
    is ItemContainer -> {
      items.forEach {
        val itemTypeName = if (it is Task) EduNames.TASK else EduNames.ITEM
        itemDir.findChild(it.name) ?: yamlIllegalStateError(noDirForItemMessage(itemTypeName, it.name))
      }
    }
    is Task -> {
      taskFiles.forEach { (name, _) ->
        itemDir.findFileByRelativePath(name) ?: yamlIllegalStateError("No physical file for file `$name`")
      }
    }
  }
}
