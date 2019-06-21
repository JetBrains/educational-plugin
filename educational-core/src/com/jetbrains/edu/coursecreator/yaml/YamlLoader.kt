package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.deserializeContent
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.saveItem
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.loadItem
import com.jetbrains.edu.coursecreator.yaml.format.getChangeApplierForItem
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
        showError(project, e, configFile, editor)
      }
      else {
        showError(project, e, configFile, editor,
                  "${NameUtil.nameToWordsLowerCase(parameterName).joinToString("_")} is empty")
      }
    }
    catch (e: MismatchedInputException) {
      showError(project, e, configFile, editor)
    }
    catch (e: InvalidYamlFormatException) {
      showError(project, e, configFile, editor, e.message.capitalize())
    }
    catch (e: IllegalStateException) {
      showError(project, e, configFile, editor)
    }
    catch (e: IOException) {
      val causeException = e.cause
      if (causeException?.message == null || causeException !is InvalidYamlFormatException) {
        showError(project, e, configFile, editor)
      }
      else {
        showError(project, e, configFile, editor, causeException.message.capitalize())
      }
    }
  }

  @VisibleForTesting
  fun doLoad(project: Project, configFile: VirtualFile) {
    val existingItem = getStudyItemForConfig(project, configFile)
    val deserializedItem = YamlDeserializer.deserializeItem(configFile)

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
      val deserializedParent = YamlDeserializer.deserializeItem(parentConfig) as ItemContainer
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
    return when (this) {
             is Section -> findCourse(project)
             is Lesson -> {
               val section = findSection(project, parentDir)
               section ?: findCourse(project)
             }
             is Task -> findLesson(project, parentDir)
             else -> yamlIllegalStateError(
               "Unexpected item type. Expected: 'Section', 'Lesson' or 'Task'. Was '${itemType}'")
           } ?: yamlIllegalStateError(notFoundMessage("parent", "for item '${name}'"))
  }

  private fun <T : StudyItem> T.applyChanges(project: Project, deserializedItem: T) {
    getChangeApplierForItem(project, deserializedItem).applyChanges(this, deserializedItem)
  }

  private fun getStudyItemForConfig(project: Project, configFile: VirtualFile): StudyItem? {
    val name = configFile.name
    val itemDir = configFile.parent ?: yamlIllegalStateError(notFoundMessage("containing item dir", name))

    return when (name) {
      YamlFormatSettings.COURSE_CONFIG -> findCourse(project)
      YamlFormatSettings.SECTION_CONFIG -> findSection(project, itemDir)
      YamlFormatSettings.LESSON_CONFIG -> findLesson(project, itemDir)
      YamlFormatSettings.TASK_CONFIG -> findTask(project, itemDir)
      else -> yamlIllegalStateError(unknownConfigMessage(name))
    }
  }

  private fun findCourse(project: Project): Course? {
    return StudyTaskManager.getInstance(project).course
  }

  private fun findSection(project: Project, sectionDir: VirtualFile): Section? {
    val course = findCourse(project) ?: return null
    return EduUtils.getSection(sectionDir, course)
  }

  private fun findTask(project: Project, taskDir: VirtualFile): Task? {
    val course = findCourse(project) ?: return null
    return EduUtils.getTask(taskDir, course)
  }

  private fun findLesson(project: Project, lessonDir: VirtualFile): Lesson? {
    val course = findCourse(project) ?: return null
    return EduUtils.getLesson(lessonDir, course)
  }

  private fun showError(project: Project,
                        originalException: Throwable,
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
    // to make test failures more comprehensible
    if (isUnitTestMode) {
      throw originalException
    }
  }

  private fun VirtualFile.getEditor(project: Project): Editor? {
    val selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(this)
    return if (selectedEditor is TextEditor) selectedEditor.editor else null
  }
}
