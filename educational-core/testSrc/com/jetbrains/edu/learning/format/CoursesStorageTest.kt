package com.jetbrains.edu.learning.format

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import junit.framework.ComparisonFailure
import org.jdom.Element
import java.nio.file.Paths

class CoursesStorageTest : EduTestCase() {

  fun testCourseModeRespected() {
    val coursesStorage = CoursesStorage.getInstance()
    val educatorCourse = course(courseMode = CCUtils.COURSE_MODE) {}
    coursesStorage.addCourse(educatorCourse, "", 0, 0)
    val studentCourse = course {}
    assertFalse(coursesStorage.hasCourse(studentCourse))
  }

  fun testCourseIdRespected() {
    val coursesStorage = CoursesStorage.getInstance()
    val courseWithDefaultId = course {}
    coursesStorage.addCourse(courseWithDefaultId, "", 0, 0)
    val studentCourse = course {}.apply { id = 1234 }
    assertFalse(coursesStorage.hasCourse(studentCourse))
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
    assertEquals("Python 2.7", course.language)
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
    assertEquals("kotlin", course.language)
  }

  fun testDeserializeLanguageVersion() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME\$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals("Python 3.7", course.language)
    assertEquals("3.7", course.languageVersion)
  }

  fun testSerializeCourseWithDefaultParameters() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book") { }.apply {
      id = 20403
      language = EduNames.PYTHON
    }

    doSerializationTest(course)
  }

  fun testSerializeLanguageVersion() {
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book") { }.apply {
      id = 20403
      language = "${EduNames.PYTHON} 3.7"
    }

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

  private fun getCoursesStorage(): CoursesStorage {
    val coursesStorage = CoursesStorage.getInstance()
    coursesStorage.state.courses.clear()
    return coursesStorage
  }

  fun testCCGroup() {
    val coursesStorage = getCoursesStorage()
    val educatorCourse = course(courseMode = CCUtils.COURSE_MODE) {}
    coursesStorage.addCourse(educatorCourse, "", 0, 0)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.my.courses.course.creation"), coursesInGroups.first().name)
  }

  fun testAllCoursesGroups() {
    val coursesStorage = getCoursesStorage()

    val educatorCourse = course(name="CC course", courseMode = CCUtils.COURSE_MODE) {}
    coursesStorage.addCourse(educatorCourse, "/CC course", 0, 0)

    val inProgressCourse = course(name="In Progress") {}
    coursesStorage.addCourse(inProgressCourse, "/in_progress", 1, 10)

    val completedCourse = course(name="Completed") {}
    coursesStorage.addCourse(completedCourse, "/completed", 10, 10)

    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(3, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.my.courses.course.creation"), coursesInGroups.first().name)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups[1].name)
    assertEquals(EduCoreBundle.message("course.dialog.completed"), coursesInGroups[2].name)

  }

  private fun doSerializationTest(course: Course) {
    val coursesStorage = CoursesStorage.getInstance()
    val courses = coursesStorage.state.courses
    coursesStorage.state.courses.removeAll(courses)
    coursesStorage.addCourse(course, "\$USER_HOME\$/IdeaProjects/AtomicKotlin")

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

  override fun getTestDataPath() = "testData/coursesStorage"
}