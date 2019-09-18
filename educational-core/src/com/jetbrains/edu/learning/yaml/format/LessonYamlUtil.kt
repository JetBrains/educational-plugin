@file:JvmName("LessonYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.coursecreator.yaml.formatError
import com.jetbrains.edu.coursecreator.yaml.unnamedItemAtMessage
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UNIT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UPDATE_DATE

/**
 * Mixin class is used to deserialize [Lesson] item.
 * Update [ItemContainerChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(CUSTOM_NAME, CONTENT)
@JsonDeserialize(builder = LessonBuilder::class)
abstract class LessonYamlMixin {
  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var myCustomPresentableName: String? = null

  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var items: List<StudyItem>
}

@JsonPOJOBuilder(withPrefix = "")
open class LessonBuilder(@JsonProperty(CONTENT) val content: List<String?> = emptyList(),
                         @JsonProperty(CUSTOM_NAME) val customName: String? = null) {
  @Suppress("unused") //used for deserialization
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
    return lesson
  }

  protected open fun createLesson(): Lesson = Lesson()
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

class RemoteLessonChangeApplier : RemoteInfoChangeApplierBase<Lesson>() {
  override fun applyChanges(existingItem: Lesson, deserializedItem: Lesson) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.unitId = deserializedItem.unitId
  }
}

