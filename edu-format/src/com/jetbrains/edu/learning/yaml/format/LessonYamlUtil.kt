@file:JvmName("LessonYamlUtil")
@file:Suppress("unused")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unnamedItemAtMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.tasks.TaskWithType

/**
 * Mixin class is used to deserialize [Lesson] item.
 * Update [ItemContainerChangeApplier] if new fields added to mixin
 */
@JsonPropertyOrder(CUSTOM_NAME, CONTENT, TAGS)
@JsonDeserialize(builder = LessonBuilder::class)
abstract class LessonYamlMixin {
  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var _items: List<StudyItem>

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  protected open lateinit var contentTags: List<String>
}

@JsonPOJOBuilder(withPrefix = "")
open class LessonBuilder(
  @param:JsonProperty(CONTENT) val content: List<String?> = emptyList(),
  @param:JsonProperty(CUSTOM_NAME) val customName: String? = null,
  @param:JsonProperty(TAGS) val contentTags: List<String> = emptyList()
) {
  private fun build(): Lesson {
    val lesson = createLesson()
    val taskList = content.mapIndexed { index: Int, title: String? ->
      if (title == null) {
        formatError(unnamedItemAtMessage(index + 1))
      }
      val task = TaskWithType(title)
      task.index = index + 1
      task
    }

    lesson.items = taskList
    lesson.customPresentableName = customName
    lesson.contentTags = contentTags
    return lesson
  }

  protected open fun createLesson(): Lesson = Lesson()
}
