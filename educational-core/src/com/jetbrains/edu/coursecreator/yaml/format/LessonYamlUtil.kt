@file:JvmName("LessonYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.yaml.InvalidYamlFormatException
import com.jetbrains.edu.coursecreator.yaml.YamlDeserializer
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.taskDirNotFoundError
import com.jetbrains.edu.coursecreator.yaml.setPlaceholdersPossibleAnswer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task

private const val CONTENT = "content"
private const val UNIT = "unit"

/**
 * Mixin class is used to deserialize [Lesson] item.
 * Update [LessonChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = LessonBuilder::class)
abstract class LessonYamlMixin {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var items: List<StudyItem>
}

@JsonPOJOBuilder(withPrefix = "")
open class LessonBuilder(@JsonProperty(CONTENT) val content: List<String?> = emptyList()) {
  @Suppress("unused") //used for deserialization
  private fun build(): Lesson {
    val lesson = createLesson()
    val taskList = content.mapIndexed { index: Int, title: String? ->
      if (title == null) {
        throw InvalidYamlFormatException("Unnamed item")
      }
      val task = TaskWithType(title)
      task.index = index + 1
      task
    }

    lesson.items = taskList
    return lesson
  }

  open fun createLesson() = Lesson()
}

/**
 * Mixin class is used to deserialize remote information of [Lesson] item stored on Stepik.
 */
@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonPropertyOrder(ID, UPDATE_DATE, UNIT)
abstract class RemoteLessonYamlMixin : RemoteStudyItemYamlMixin() {
  @JsonProperty(UNIT)
  private var unitId: Int = 0
}

class LessonChangeApplier<T : Lesson>(val project: Project) : StudyItemChangeApplier<T>() {

  override fun applyChanges(existingItem: T, deserializedItem: T) {
    updateLessonChildren(project, existingItem, deserializedItem)
  }

  //TODO: merge with updateItemContainer changes when Lesson inherits ItemContainer
  private fun updateLessonChildren(project: Project,
                                   existingLesson: Lesson,
                                   deserializedLesson: Lesson) {
    deserializedLesson.visitTasks {
      val existingTask = existingLesson.getTask(it.name)
      if (existingTask != null) {
        existingTask.index = it.index
      }
      else {
        existingLesson.addAsNewTask(project, it)
      }
    }
  }

  private fun Lesson.addAsNewTask(project: Project, titledTask: Task) {
    val lessonDir = getLessonDir(project)
    val taskDir = lessonDir?.findChild(titledTask.name) ?: taskDirNotFoundError(titledTask.name)
    val taskConfigFile = taskDir.findChild(YamlFormatSettings.TASK_CONFIG) ?: noConfigFileError(titledTask)

    val deserializedTask = YamlDeserializer.deserializeItem(VfsUtil.loadText(taskConfigFile), YamlFormatSettings.TASK_CONFIG) as Task
    deserializedTask.name = titledTask.name
    deserializedTask.index = titledTask.index
    deserializedTask.init(course, this, true)
    deserializedTask.taskFiles.values.forEach { it.setPlaceholdersPossibleAnswer(project) }

    addTask(deserializedTask.index - 1, deserializedTask)
    course.configurator?.courseBuilder?.refreshProject(project)
  }

  private fun noConfigFileError(it: StudyItem): Nothing = error("No config file for ${it.name}")
}

