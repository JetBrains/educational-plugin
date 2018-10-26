@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.*

private const val VERSION = "version"
private const val TITLE = "title"
private const val LANGUAGE = "language"
private const val SUMMARY = "summary"
private const val PROGRAMMING_LANGUAGE = "programming_language"
private const val ITEMS = "items"
private const val NAME = "name"
private const val TASK_LIST = "task_list"
private const val TASK_FILES = "task_files"
private const val TEST_FILES = "test_files"
private const val ADDITIONAL_FILES = "additional_files"
private const val TASK_TYPE = "task_type"
private const val DESCRIPTION_TEXT = "description_text"
private const val DESCRIPTION_FORMAT = "description_format"
private const val TEXT = "text"
private const val PLACEHOLDERS = "placeholders"
private const val TYPE = "type"

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(VERSION, TITLE, SUMMARY, PROGRAMMING_LANGUAGE, LANGUAGE, ITEMS)
abstract class LocalCourseMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @JsonSerialize(converter = ProgrammingLanguageConverter::class)
  @JsonProperty(PROGRAMMING_LANGUAGE)
  private lateinit var myProgrammingLanguage: String

  @JsonSerialize(converter = LanguageConverter::class)
  @JsonProperty(LANGUAGE)
  private lateinit var myLanguageCode: String

  @JsonProperty(ITEMS)
  private lateinit var items: List<StudyItem>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalSectionMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(ITEMS)
  private lateinit var items: List<StudyItem>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalLessonMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(TASK_LIST)
  private lateinit var taskList: List<Task>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalTaskMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(TASK_FILES)
  private lateinit var myTaskFiles: MutableMap<String, TaskFile>

  @JsonProperty(TEST_FILES)
  private lateinit var testsText: MutableMap<String, String>

  @JsonProperty(DESCRIPTION_TEXT)
  private lateinit var descriptionText: String

  @JsonProperty(DESCRIPTION_FORMAT)
  private lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty(ADDITIONAL_FILES)
  protected lateinit var additionalFiles: MutableMap<String, AdditionalFile>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalTaskFileMixin {
  @JsonProperty(NAME)
  private lateinit var myName: String

  @JsonProperty(TEXT)
  private lateinit var _text: String

  @JsonProperty(PLACEHOLDERS)
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String = Language.findLanguageByID(languageId)!!.displayName
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = Locale(languageCode).displayName
}

class TaskSerializer : JsonSerializer<Task>() {
  override fun serialize(value: Task, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeStartObject()
    val javaType = provider.constructType(Task::class.java)
    val beanDesc: BeanDescription = provider.config.introspect(javaType)
    val serializer = BeanSerializerFactory.instance.findBeanSerializer(provider, javaType, beanDesc)
    serializer.unwrappingSerializer(null).serialize(value, jgen, provider)
    jgen.writeObjectField(TASK_TYPE, "edu")
    jgen.writeEndObject()
  }
}