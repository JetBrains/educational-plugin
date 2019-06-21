package com.jetbrains.edu.coursecreator.yaml.format

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.childrenConfigFileNames
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer.findConfigFile
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.addItemAsNew
import com.jetbrains.edu.coursecreator.yaml.unexpectedItemTypeMessage
import com.jetbrains.edu.coursecreator.yaml.yamlIllegalStateError
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.deserializeChildrenIfNeeded
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Specific instance of this class applies changes from deserialized item, see [com.jetbrains.edu.coursecreator.yaml.YamlDeserializer],
 * to already existing course item.
 */
abstract class StudyItemChangeApplier<T : StudyItem> {
  abstract fun applyChanges(existingItem: T, deserializedItem: T)
}

open class ItemContainerChangeApplier<T : ItemContainer>(val project: Project) : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    updateChildren(deserializedItem, existingItem)
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
        val parentDir = existingItem.getDir(project)
        val configFile = titledItem.findConfigFile(project, parentDir, *deserializedItem.childrenConfigFileNames) ?: continue

        val deserializedChild = YamlDeserializer.deserializeItem(configFile)
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
    else -> yamlIllegalStateError(unexpectedItemTypeMessage(item.javaClass.simpleName))
  } as StudyItemChangeApplier<T>
}