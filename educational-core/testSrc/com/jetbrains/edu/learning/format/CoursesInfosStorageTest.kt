package com.jetbrains.edu.learning.format

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.JBCoursesStorage
import com.jetbrains.edu.learning.newproject.coursesStorage.UserCoursesState
import com.jetbrains.edu.learning.stepik.hyperskill.PlainTextHyperskillConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourse
import junit.framework.ComparisonFailure
import org.jdom.Element
import java.nio.file.Paths
import java.util.*

class CoursesInfosStorageTest : EduTestCase() {

  fun `test correct configurator found for courses in storage`() {
    val coursesStorage = JBCoursesStorage.getInstance()

    val hyperskillCourse = hyperskillCourse(language = PlainTextLanguage.INSTANCE) {}
    val eduCourse = course {}

    for ((course, configuratorClass) in listOf(hyperskillCourse to PlainTextHyperskillConfigurator::class.java,
                                               eduCourse to PlainTextConfigurator::class.java)) {
      coursesStorage.addCourse(course, "location", 0, 0)
      assertInstanceOf(coursesStorage.getCourseMetaInfo(course)!!.configurator, configuratorClass)
    }
  }

  fun testCourseModeRespected() {
    val coursesStorage = JBCoursesStorage.getInstance()
    val educatorCourse = course(courseMode = CourseMode.EDUCATOR) {}
    coursesStorage.addCourse(educatorCourse, "", 0, 0)
    val studentCourse = course {}
    assertFalse(coursesStorage.hasCourse(studentCourse))
  }

  fun testCourseIdRespected() {
    val coursesStorage = JBCoursesStorage.getInstance()
    val courseWithDefaultId = course {}
    coursesStorage.addCourse(courseWithDefaultId, "", 0, 0)
    val studentCourse = course {}.apply { id = 1234 }
    assertFalse(coursesStorage.hasCourse(studentCourse))
  }

  fun testLanguageRespected() {
    val coursesStorage = JBCoursesStorage.getInstance()
    val courseWithDefaultId = course {}
    coursesStorage.addCourse(courseWithDefaultId, "", 0, 0)
    val courseWithLanguage = course {}.apply { languageId = EduNames.PYTHON }
    assertFalse(coursesStorage.hasCourse(courseWithLanguage))
  }

  fun testDeserializeFirstVersionCoursesStorage() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("Introduction to Python", course.name)
    assertEquals(238, course.id)
    assertEquals("\$USER_HOME\$/IdeaProjects/Introduction to Python", course.location)
    assertEquals("Introduction course to Python.", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduNames.PYTHON, course.languageId)
    assertEquals("2.7", course.languageVersion)
  }

  fun testDeserializeCourseWithDefaultParameters() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME\$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals("kotlin", course.languageId)
  }

  fun testDeserializeOldLanguageVersion() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME\$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }

  // Such case shouldn't happen, but this test is useful for migration testing
  fun testDeserializeNewLanguageVersion() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME\$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }

  fun testDeserializeNewLanguageVersionAndLanguageId() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME\$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }

  // Such case shouldn't happen, but this test is useful for migration testing
  fun testDeserializeNewAndOldLanguageVersion() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME\$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }

  fun testDeserializeHumanLanguageInRussianLocale() {
    val default = Locale.getDefault()
    Locale.setDefault(Locale("ru", "RU"))
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    course.humanLanguage
    Locale.setDefault(default)
    assertEquals("en", course.languageCode)
  }

  fun testDeserializeHumanLanguageInEnglishLocale() {
    val default = Locale.getDefault()
    Locale.setDefault(Locale("en", ""))
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    course.humanLanguage
    Locale.setDefault(default)
    assertEquals("en", course.languageCode)
  }

  fun testSerializeCourseWithDefaultParameters() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book") { }.apply {
      id = 20403
      languageId = EduNames.PYTHON
    }

    doSerializationTest(course)
  }

  fun testSerializeLanguageVersion() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book") { }.apply {
      id = 20403
      languageId = EduNames.PYTHON
      languageVersion = "3.7"
    }

    doSerializationTest(course)
  }

  fun testSerializeHumanLanguage() {
    val default = Locale.getDefault()
    Locale.setDefault(Locale("ru", "RU"))
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book") { }.apply {
      id = 20403
      languageId = EduNames.PYTHON
      languageVersion = "3.7"
      languageCode = "ru"
    }

    Locale.setDefault(default)
    doSerializationTest(course)
  }

  fun testEmptyCoursesGroup() {
    val coursesStorage = getCoursesStorage()
    assertEmpty(coursesStorage.coursesInGroups())
  }

  fun testInProgressCoursesGroup() {
    val coursesStorage = getCoursesStorage()
    val course = course {}
    coursesStorage.addCourse(course, "", 1, 10)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups.first().name)
  }

  fun testCompletedCoursesGroup() {
    val coursesStorage = getCoursesStorage()
    val course = course {}
    coursesStorage.addCourse(course, "", 10, 10)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.completed"), coursesInGroups.first().name)
  }

  fun testUntouchedCourse() {
    val coursesStorage = getCoursesStorage()
    val course = course {}
    coursesStorage.addCourse(course, "", 0, 0)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups.first().name)
  }

  private fun getCoursesStorage(): JBCoursesStorage {
    val coursesStorage = JBCoursesStorage.getInstance()
    coursesStorage.state.courses.clear()
    return coursesStorage
  }

  fun testCCGroup() {
    val coursesStorage = getCoursesStorage()
    val educatorCourse = course(courseMode = CourseMode.EDUCATOR) {}
    coursesStorage.addCourse(educatorCourse, "", 0, 0)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.my.courses.course.creation"), coursesInGroups.first().name)
  }

  fun testAllCoursesGroups() {
    val coursesStorage = getCoursesStorage()

    val educatorCourse = course(name = "CC course", courseMode = CourseMode.EDUCATOR) {}
    coursesStorage.addCourse(educatorCourse, "/CC course", 0, 0)

    val inProgressCourse = course(name="In Progress") {}
    coursesStorage.addCourse(inProgressCourse, "/in_progress", 1, 10)

    val completedCourse = course(name = "Completed") {}
    coursesStorage.addCourse(completedCourse, "/completed", 10, 10)

    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(3, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.my.courses.course.creation"), coursesInGroups.first().name)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups[1].name)
    assertEquals(EduCoreBundle.message("course.dialog.completed"), coursesInGroups[2].name)
  }

  private fun doSerializationTest(course: Course) {
    val coursesStorage = JBCoursesStorage.getInstance()
    val courses = coursesStorage.state.courses
    coursesStorage.state.courses.removeAll(courses)
    coursesStorage.addCourse(course, "\$USER_HOME\$/IdeaProjects/AtomicKotlin")

    @Suppress("UnstableApiUsage")
    val actual = XmlSerializer.serialize(coursesStorage.state, SkipDefaultsSerializationFilter())
    val expected = loadFromFile()

    checkEquals(expected, actual)
  }

  private fun deserializeState(): UserCoursesState {
    val element = loadFromFile()
    return XmlSerializer.deserialize(element.children.first(), UserCoursesState::class.java)
  }

  private fun loadFromFile(): Element {
    val name = getTestName(true)
    val loaded = Paths.get(testDataPath).resolve("$name.xml")
    return JDOMUtil.load(loaded)
  }

  private fun checkEquals(expected: Element, actual: Element) {
    if (!JDOMUtil.areElementsEqual(expected, actual)) {
      throw ComparisonFailure("Elements are not equal", JDOMUtil.writeElement(expected), JDOMUtil.writeElement(actual))
    }
  }

  override fun tearDown() {
    super.tearDown()
    JBCoursesStorage.getInstance().state.courses.clear()
  }

  override fun getTestDataPath() = "testData/coursesStorage"
}