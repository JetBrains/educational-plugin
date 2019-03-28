@file:JvmName("LessonYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.coursecreator.yaml.InvalidYamlFormatException
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task

private const val CONTENT = "content"

/**
 * Mixin class is used to deserialize [Lesson] item.
 * Update [LessonChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = LessonBuilder::class)
abstract class LessonYamlMixin {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var taskList: List<Task>
}

@JsonPOJOBuilder(withPrefix = "")
open class LessonBuilder(@JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") //used for deserialization
  private fun build(): Lesson {
    val lesson = createLesson()
    val taskList = content.map {
      if (it == null) {
        throw InvalidYamlFormatException("Unnamed item")
      }
      TaskWithType(it)
    }
    lesson.updateTaskList(taskList)
    return lesson
  }

  open fun createLesson() = Lesson()
}

class LessonChangeApplier<T : Lesson>(val project: Project) : StudyItemChangeApplier<T>() {

  override fun applyChanges(existingItem: T, deserializedItem: T) {
    updateLessonChildren(project, existingItem, deserializedItem)
  }

  //TODO: merge with updateItemContainer changes when Lesson inherits ItemContainer
  private fun updateLessonChildren(project: Project,
                                   existingLesson: Lesson,
                                   deserializedLesson: Lesson) {
    deserializedLesson.visitTasks { titledTask, _ ->
      val existingTask = existingLesson.getTask(titledTask.name)
      if (existingTask != null) {
        existingTask.index = titledTask.index
      }
      else {
        existingLesson.addAsNewTask(project, titledTask)
      }
      true
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

    taskList.add(deserializedTask.index - 1, deserializedTask)
    course.configurator?.courseBuilder?.refreshProject(project)
  }

  private fun noConfigFileError(it: StudyItem): Nothing = error("No config file for ${it.name}")
}

