package com.jetbrains.edu.coursecreator.yaml.format

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.getConfigFileForChild
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.deserializeChildrenIfNeeded
import com.jetbrains.edu.coursecreator.yaml.loadingError
import com.jetbrains.edu.coursecreator.yaml.unexpectedItemTypeMessage
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraCourse

/**
 * Specific instance of this class applies changes from deserialized item, see [com.jetbrains.edu.coursecreator.yaml.YamlDeserializer],
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
    updateChildren(deserializedItem, existingItem)
  }

  private fun <T : ItemContainer> changeType(project: Project, existingItem: T, deserializedItem: T) {
    if (deserializedItem !is EduCourse && deserializedItem !is CourseraCourse) {
      loadingError("Expected ${existingItem::class.simpleName} class, but was: ${deserializedItem.javaClass.simpleName}")
    }

    deserializedItem.items = existingItem.items
    deserializedItem.init(null, null, false)
    StudyTaskManager.getInstance(project).course = deserializedItem as Course
  }

  private fun updateChildren(deserializedItem: T, existingItem: T) {
    val existingChildren = existingItem.items
    val preservedChildren = mutableListOf<StudyItem>()
    for (titledItem in deserializedItem.items)  {
      val child = existingChildren.find { it.name == titledItem.name }
      if (child != null) {
        child.index = titledItem.index
        preservedChildren.add(child)
      }
      else {
        // this code adding new child item if it was added in config and there's a dir
        // it is called from `YamlLoader.loadItem`
        val configFile = existingItem.getConfigFileForChild(project, titledItem.name) ?: continue

        val deserializedChild = YamlDeserializer.deserializeItem(project, configFile) ?: continue
        deserializedChild.name = titledItem.name
        deserializedChild.index = titledItem.index
        deserializedChild.deserializeChildrenIfNeeded(project, existingItem.course)
        preservedChildren.add(deserializedChild)
      }
    }
    // update items so as removed items are no longer in the course
    existingItem.items = preservedChildren
    existingItem.init(existingItem.course, existingItem.parent, false)
  }
}

fun <T : StudyItem> getChangeApplierForItem(project: Project, item: T): StudyItemChangeApplier<T> {
  @Suppress("UNCHECKED_CAST") //
  return when (item) {
    is Course -> CourseChangeApplier(project)
    is Section, is Lesson -> ItemContainerChangeApplier(project)
    is Task -> TaskChangeApplier(project)
    else -> loadingError(unexpectedItemTypeMessage(item.javaClass.simpleName))
  } as StudyItemChangeApplier<T>
}