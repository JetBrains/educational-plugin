package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.YamlLoader.addItemAsNew
import com.jetbrains.edu.learning.yaml.YamlLoader.deserializeChildrenIfNeeded
import com.jetbrains.edu.learning.yaml.YamlLoader.getConfigFileForChild
import com.jetbrains.edu.learning.yaml.deserializeItemProcessingErrors
import com.jetbrains.edu.learning.yaml.errorHandling.loadingError
import com.jetbrains.edu.learning.yaml.errorHandling.unexpectedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskChangeApplier

/**
 * Specific instance of this class applies changes from deserialized item, see [com.jetbrains.edu.learning.yaml.YamlDeserializer],
 * to already existing course item.
 */
abstract class StudyItemChangeApplier<T : StudyItem> {
  abstract fun applyChanges(existingItem: T, deserializedItem: T)
}

open class ItemContainerChangeApplier<T : ItemContainer>(val project: Project) : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    if (existingItem.itemType != deserializedItem.itemType) {
      changeType(project, existingItem, deserializedItem)
      return
    }
    @Suppress("DEPRECATION")
    existingItem.customPresentableName = deserializedItem.customPresentableName
    existingItem.contentTags = deserializedItem.contentTags
    if (existingItem is FrameworkLesson && deserializedItem is FrameworkLesson) {
      existingItem.isTemplateBased = deserializedItem.isTemplateBased
    }
    updateChildren(deserializedItem, existingItem)
  }

  private fun <T : ItemContainer> changeType(project: Project, existingItem: T, deserializedItem: T) {
    if (deserializedItem is EduCourse || deserializedItem is CourseraCourse || deserializedItem is HyperskillCourse) {
      deserializedItem.items = existingItem.items
      deserializedItem.init(deserializedItem, false)
      StudyTaskManager.getInstance(project).course = deserializedItem as Course
      return
    }

    if (deserializedItem is Lesson) {
      deserializedItem.name = existingItem.name
      deserializedItem.index = existingItem.index
      deserializedItem.items = existingItem.items

      val parentItem = existingItem.parent
      parentItem.removeItem(existingItem)
      parentItem.addItemAsNew(project, deserializedItem)
      return
    }

    loadingError(EduCoreBundle.message("yaml.editor.invalid.unexpected.class", existingItem::class.simpleName.toString(),
                                       deserializedItem.javaClass.simpleName))
  }

  private fun updateChildren(deserializedItem: T, existingItem: T) {
    val existingChildren = existingItem.items
    val preservedChildren = mutableListOf<StudyItem>()
    val mapper = existingItem.course.mapper()
    for (titledItem in deserializedItem.items) {
      val child = existingChildren.find { it.name == titledItem.name }
      if (child != null) {
        child.index = titledItem.index
        preservedChildren.add(child)
      }
      else {
        // this code adding new child item if it was added in config and there's a dir
        // it is called from `YamlLoader.loadItem`
        val configFile = existingItem.getConfigFileForChild(project, titledItem.name) ?: continue

        val deserializedChild = deserializeItemProcessingErrors(configFile, project, mapper=mapper) ?: continue
        deserializedChild.name = titledItem.name
        deserializedChild.index = titledItem.index
        deserializedChild.parent = existingItem
        deserializedChild.deserializeChildrenIfNeeded(project, existingItem.course)
        preservedChildren.add(deserializedChild)
      }
    }
    // update items so as removed items are no longer in the course
    existingItem.items = preservedChildren
    existingItem.init(existingItem.parent, false)
  }
}

fun <T : StudyItem> getChangeApplierForItem(project: Project, item: T): StudyItemChangeApplier<T> {
  @Suppress("UNCHECKED_CAST") //
  return when (item) {
    is Course -> CourseChangeApplier(project)
    is Section, is Lesson -> ItemContainerChangeApplier(project)
    is Task -> if (project.isStudentProject()) StudentTaskChangeApplier(project) else TaskChangeApplier(project)
    else -> loadingError(unexpectedItemTypeMessage(item.javaClass.simpleName))
  } as StudyItemChangeApplier<T>
}