package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.encrypt.getAesKey
import com.jetbrains.edu.learning.yaml.format.*
import com.jetbrains.edu.learning.yaml.format.student.*
import org.jetbrains.annotations.TestOnly
import java.util.*

object YamlMapperBase {
  val MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    mapper.addMixIns()
    mapper
  }

  val REMOTE_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    mapper.addRemoteInfoMixIns()
    mapper
  }

  val STUDENT_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    mapper.addMixIns()
    mapper.addMixIn(TaskFile::class.java, StudentTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()

    mapper
  }

  val STUDENT_MAPPER_WITH_ENCRYPTION: ObjectMapper by lazy {
    val mapper = createMapper()
    mapper.addMixIns()
    mapper.addEncryptionModule()
    mapper.addMixIn(TaskFile::class.java, StudentEncryptedTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentEncryptedAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()

    mapper
  }

  @get:TestOnly
  val TEST_STUDENT_MAPPER_WITH_ENCRYPTION: ObjectMapper by lazy {
    val mapper = createMapper()
    mapper.addMixIns()
    mapper.addMixIn(TaskFile::class.java, StudentEncryptedTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentEncryptedAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()

    mapper
  }

  private fun ObjectMapper.addEncryptionModule() {
    val aesKey = getAesKey()
    registerModule(EncryptionModule(aesKey))
  }

  private fun createMapper(): ObjectMapper {
    val yamlFactory = YAMLFactory.builder()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
      .build()

    return JsonMapper.builder(yamlFactory)
      .addModule(kotlinModule())
      .addModule(JavaTimeModule())
      .defaultLocale(Locale.ENGLISH)
      .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .serializationInclusion(JsonInclude.Include.NON_EMPTY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .disable(MapperFeature.AUTO_DETECT_FIELDS, MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_IS_GETTERS)
      .build()
  }

  private fun ObjectMapper.addMixIns() {
    addMixIn(Course::class.java, CourseYamlMixin::class.java)
    addMixIn(Section::class.java, SectionYamlMixin::class.java)
    addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    addMixIn(FrameworkLesson::class.java, FrameworkLessonYamlMixin::class.java)
    addMixIn(Task::class.java, TaskYamlMixin::class.java)
    addMixIn(ChoiceTask::class.java, ChoiceTaskYamlMixin::class.java)
    addMixIn(ChoiceOption::class.java, ChoiceOptionYamlMixin::class.java)
    addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
    addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyYamlMixin::class.java)
  }

  private fun ObjectMapper.addRemoteInfoMixIns() {
    addMixIn(EduCourse::class.java, EduCourseRemoteInfoYamlMixin::class.java)
    addMixIn(Lesson::class.java, RemoteStudyItemYamlMixin::class.java)
    addMixIn(Section::class.java, RemoteStudyItemYamlMixin::class.java)
    addMixIn(Task::class.java, RemoteStudyItemYamlMixin::class.java)
  }

  private fun ObjectMapper.addStudentMixIns() {
    addMixIn(Course::class.java, StudentCourseYamlMixin::class.java)
    addMixIn(FrameworkLesson::class.java, StudentFrameworkLessonYamlMixin::class.java)

    addMixIn(Task::class.java, StudentTaskYamlMixin::class.java)
    addMixIn(TheoryTask::class.java, TheoryTaskYamlUtil::class.java)
    addMixIn(ChoiceTask::class.java, StudentChoiceTaskYamlMixin::class.java)
    addMixIn(AnswerPlaceholder.MyInitialState::class.java, InitialStateMixin::class.java)
    addMixIn(CheckFeedback::class.java, FeedbackYamlMixin::class.java)
  }
}