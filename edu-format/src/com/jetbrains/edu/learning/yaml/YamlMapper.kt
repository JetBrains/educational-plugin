package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLParser
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTopic
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.encrypt.getAesKey
import com.jetbrains.edu.learning.yaml.format.*
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.COURSE_TYPE_YAML
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HYPERSKILL_TYPE_YAML
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STEPIK_TYPE_YAML
import com.jetbrains.edu.learning.yaml.format.coursera.CourseraCourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.hyperskill.*
import com.jetbrains.edu.learning.yaml.format.remote.*
import com.jetbrains.edu.learning.yaml.format.student.*
import com.jetbrains.edu.learning.yaml.format.tasks.*
import org.jetbrains.annotations.TestOnly
import java.util.*

object YamlMapper {
  fun basicMapper(): ObjectMapper {
    val mapper = createMapper()
    mapper.addMixIns()
    return mapper
  }

  val REMOTE_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addRemoteMixIns(mapper)
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
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()
    mapper
  }

  @get:TestOnly
  val TEST_STUDENT_MAPPER_WITH_ENCRYPTION: ObjectMapper by lazy {
    val mapper = createMapper()
    mapper.addMixIns()
    mapper.addMixIn(TaskFile::class.java, StudentEncryptedTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentAnswerPlaceholderYamlMixin::class.java)
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
      .enable(YAMLParser.Feature.EMPTY_STRING_AS_NULL)
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
    addMixIn(CourseraCourse::class.java, CourseraCourseYamlMixin::class.java)
    addMixIn(HyperskillCourse::class.java, RemoteCourseYamlMixin::class.java)
    addMixIn(StepikCourse::class.java, RemoteCourseYamlMixin::class.java)
    addMixIn(Course::class.java, CourseYamlMixin::class.java)
    addMixIn(Section::class.java, SectionYamlMixin::class.java)
    addMixIn(StepikLesson::class.java, StepikLessonYamlMixin::class.java)
    addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    addMixIn(FrameworkLesson::class.java, FrameworkLessonYamlMixin::class.java)
    addMixIn(Task::class.java, TaskYamlMixin::class.java)
    addMixIn(ChoiceTask::class.java, ChoiceTaskYamlMixin::class.java)
    addMixIn(CodeTask::class.java, CodeTaskYamlMixin::class.java)
    addMixIn(ChoiceOption::class.java, ChoiceOptionYamlMixin::class.java)
    addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
    addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyYamlMixin::class.java)

    registerSubtypes(NamedType(CourseraCourse::class.java, COURSE_TYPE_YAML))
    registerSubtypes(NamedType(HyperskillCourse::class.java, HYPERSKILL_TYPE_YAML))
    registerSubtypes(NamedType(StepikCourse::class.java, STEPIK_TYPE_YAML))
  }

  private fun addRemoteMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(EduCourse::class.java, EduCourseRemoteInfoYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(StepikLesson::class.java, StepikLessonRemoteYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(DataTask::class.java, RemoteDataTaskYamlMixin::class.java)
    mapper.addMixIn(DataTaskAttempt::class.java, DataTaskAttemptYamlMixin::class.java)
    mapper.addHyperskillMixins()
  }

  private fun ObjectMapper.addHyperskillMixins() {
    addMixIn(HyperskillCourse::class.java, HyperskillCourseMixin::class.java)
    addMixIn(HyperskillProject::class.java, HyperskillProjectMixin::class.java)
    addMixIn(HyperskillStage::class.java, HyperskillStageMixin::class.java)
    addMixIn(HyperskillTopic::class.java, HyperskillTopicMixin::class.java)
  }

  private fun ObjectMapper.addStudentMixIns() {
    addMixIn(Course::class.java, StudentCourseYamlMixin::class.java)

    addMixIn(FrameworkLesson::class.java, StudentFrameworkLessonYamlMixin::class.java)

    addMixIn(Task::class.java, StudentTaskYamlMixin::class.java)
    addMixIn(RemoteEduTask::class.java, RemoteEduTaskYamlMixin::class.java)
    addMixIn(TheoryTask::class.java, TheoryTaskYamlUtil::class.java)
    addMixIn(ChoiceTask::class.java, StudentChoiceTaskYamlMixin::class.java)
    addMixIn(SortingTask::class.java, SortingTaskYamlMixin::class.java)
    addMixIn(MatchingTask::class.java, MatchingTaskYamlMixin::class.java)
    addMixIn(TableTask::class.java, TableTaskYamlMixin::class.java)
    addMixIn(AnswerPlaceholder.MyInitialState::class.java, InitialStateMixin::class.java)
    addMixIn(CheckFeedback::class.java, FeedbackYamlMixin::class.java)
  }

}