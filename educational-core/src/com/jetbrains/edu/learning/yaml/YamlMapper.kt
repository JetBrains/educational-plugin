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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTopic
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.encrypt.TEST_AES_KEY
import com.jetbrains.edu.learning.json.encrypt.getAesKey
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE_YAML
import com.jetbrains.edu.learning.yaml.format.*
import com.jetbrains.edu.learning.yaml.format.student.*
import java.util.*

object YamlMapper {
  val MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)

    mapper
  }

  @VisibleForTesting
  val REMOTE_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addRemoteMixIns(mapper)

    mapper
  }

  @VisibleForTesting
  val STUDENT_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)
    mapper.addMixIn(TaskFile::class.java, StudentTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()

    mapper
  }

  @VisibleForTesting
  val STUDENT_MAPPER_WITH_ENCRYPTION: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)
    val aesKey = if (!isUnitTestMode) getAesKey() else TEST_AES_KEY
    mapper.registerModule(EncryptionModule(aesKey))
    mapper.addMixIn(TaskFile::class.java, StudentEncryptedTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentEncryptedAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()

    mapper
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

  private fun addMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(CodeforcesCourse::class.java, CodeforcesCourseYamlMixin::class.java)
    mapper.addMixIn(CourseraCourse::class.java, CourseraCourseYamlMixin::class.java)
    mapper.addMixIn(CheckiOCourse::class.java, RemoteCourseYamlMixin::class.java)
    mapper.addMixIn(HyperskillCourse::class.java, RemoteCourseYamlMixin::class.java)
    mapper.addMixIn(StepikCourse::class.java, RemoteCourseYamlMixin::class.java)
    mapper.addMixIn(Course::class.java, CourseYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, SectionYamlMixin::class.java)
    mapper.addMixIn(StepikLesson::class.java, StepikLessonYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    mapper.addMixIn(FrameworkLesson::class.java, FrameworkLessonYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, TaskYamlMixin::class.java)
    mapper.addMixIn(ChoiceTask::class.java, ChoiceTaskYamlMixin::class.java)
    mapper.addMixIn(CodeTask::class.java, CodeTaskYamlMixin::class.java)
    mapper.addMixIn(ChoiceOption::class.java, ChoiceOptionYamlMixin::class.java)
    mapper.addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyYamlMixin::class.java)

    mapper.registerSubtypes(NamedType(CodeforcesCourse::class.java, EduFormatNames.CODEFORCES_TYPE_YAML))
    mapper.registerSubtypes(NamedType(CourseraCourse::class.java, CourseraNames.COURSE_TYPE_YAML))
    mapper.registerSubtypes(NamedType(CheckiOCourse::class.java, CheckiONames.CHECKIO_TYPE_YAML))
    mapper.registerSubtypes(NamedType(HyperskillCourse::class.java, HYPERSKILL_TYPE_YAML))
    mapper.registerSubtypes(NamedType(StepikCourse::class.java, StepikNames.STEPIK_TYPE_YAML))
  }

  private fun addRemoteMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(EduCourse::class.java, EduCourseRemoteInfoYamlMixin::class.java)
    mapper.addMixIn(CodeforcesCourse::class.java, CodeforcesCourseRemoteInfoYamlMixin::class.java)
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

    addMixIn(CheckiOStation::class.java, CheckiOStationYamlMixin::class.java)
    addMixIn(FrameworkLesson::class.java, StudentFrameworkLessonYamlMixin::class.java)

    addMixIn(Task::class.java, StudentTaskYamlMixin::class.java)
    addMixIn(RemoteEduTask::class.java, RemoteEduTaskYamlMixin::class.java)
    addMixIn(TheoryTask::class.java, TheoryTaskYamlUtil::class.java)
    addMixIn(ChoiceTask::class.java, StudentChoiceTaskYamlMixin::class.java)
    addMixIn(CheckiOMission::class.java, CheckiOMissionYamlMixin::class.java)
    addMixIn(CodeforcesTask::class.java, CodeforcesTaskYamlMixin::class.java)
    addMixIn(CodeforcesTaskWithFileIO::class.java, CodeforcesTaskWithFileIOYamlMixin::class.java)
    addMixIn(SortingTask::class.java, SortingTaskYamlMixin::class.java)
    addMixIn(MatchingTask::class.java, MatchingTaskYamlMixin::class.java)
    addMixIn(AnswerPlaceholder.MyInitialState::class.java, InitialStateMixin::class.java)
    addMixIn(CheckFeedback::class.java, FeedbackYamlMixin::class.java)
  }

}