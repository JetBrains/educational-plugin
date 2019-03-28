@file:JvmName("SectionYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem

private const val CONTENT = "content"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = SectionBuilder::class)
abstract class SectionYamlMixin {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var items: List<StudyItem>
}

@JsonPOJOBuilder(withPrefix = "")
private class SectionBuilder(@JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") //used for deserialization
  private fun build(): Section {
    val section = Section()
    val items = content.map {
      if (it == null) {
        throw InvalidYamlFormatException("Unnamed item")
      }
      TitledStudyItem(it)
    }
    section.items = items
    return section
  }
}
