@file:JvmName("LessonYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.coursecreator.yaml.formatError
import com.jetbrains.edu.coursecreator.yaml.unnamedItemAtMessage
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem

private const val CONTENT = "content"
private const val UNIT = "unit"

/**
 * Mixin class is used to deserialize [Lesson] item.
 * Update [ItemContainerChangeApplier] if new fields added to mixin
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
        formatError(unnamedItemAtMessage(index + 1))
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

class RemoteLessonChangeApplier : RemoteInfoChangeApplierBase<Lesson>() {
  override fun applyChanges(existingItem: Lesson, deserializedItem: Lesson) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.unitId = deserializedItem.unitId
  }
}

