package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import kotlin.test.assertIs
import kotlin.test.assertNotNull as kAssertNotNull

class CoursesInfosStorageTest : EduTestCase() {

  @Test
  fun `test correct configurator found for courses in storage`() {
    val coursesStorage = CoursesStorage.getInstance()

    val eduCourse = course {}
    coursesStorage.addCourse(eduCourse, "location", 0, 0)
    assertIs<PlainTextConfigurator>(coursesStorage.getCourseMetaInfo(eduCourse)?.toCourse()?.configurator)
  }

  @Test
  fun `test course mode respected`() {
    val coursesStorage = CoursesStorage.getInstance()
    val educatorCourse = course(courseMode = CourseMode.EDUCATOR) {}
    coursesStorage.addCourse(educatorCourse, "", 0, 0)
    val studentCourse = course {}
    assertFalse(coursesStorage.hasCourse(studentCourse))
  }

  @Test
  fun `test course id respected`() {
    val coursesStorage = CoursesStorage.getInstance()
    val courseWithDefaultId = course {}
    coursesStorage.addCourse(courseWithDefaultId, "", 0, 0)
    val studentCourse = course {}.apply { id = 1234 }
    assertFalse(coursesStorage.hasCourse(studentCourse))
  }

  @Test
  fun `test language respected`() {
    val coursesStorage = CoursesStorage.getInstance()
    val courseWithDefaultId = course {}
    coursesStorage.addCourse(courseWithDefaultId, "", 0, 0)
    val courseWithLanguage = course {}.apply { languageId = EduFormatNames.PYTHON }
    assertFalse(coursesStorage.hasCourse(courseWithLanguage))
  }

  @Test
  fun `test empty courses group`() {
    val coursesStorage = CoursesStorage.getInstance()
    assertEmpty(coursesStorage.coursesInGroups())
  }

  @Test
  fun `test in progress courses group`() {
    val coursesStorage = CoursesStorage.getInstance()
    val course = course {}
    coursesStorage.addCourse(course, "", 1, 10)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups.first().name)
  }

  @Test
  fun `test completed courses group`() {
    val coursesStorage = CoursesStorage.getInstance()
    val course = course {}
    coursesStorage.addCourse(course, "", 10, 10)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.completed"), coursesInGroups.first().name)
  }

  @Test
  fun `test untouched course`() {
    val coursesStorage = CoursesStorage.getInstance()
    val course = course {}
    coursesStorage.addCourse(course, "", 0, 0)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups.first().name)
  }

  @Test
  fun `test cc group`() {
    val coursesStorage = CoursesStorage.getInstance()
    val educatorCourse = course(courseMode = CourseMode.EDUCATOR) {}
    coursesStorage.addCourse(educatorCourse, "", 0, 0)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.my.courses.course.creation"), coursesInGroups.first().name)
  }

  @Test
  fun `test record metadata is filled on course adding`() = runTest {
    val coursesStorage = CoursesStorage(backgroundScope, newRecordId = { RECORD_ID }, now = { STARTED_AT })
    val course = course {}
    coursesStorage.addCourse(course, "/location", 0, 10)

    val courseMetaInfo = kAssertNotNull(coursesStorage.getCourseMetaInfo(course))
    assertEquals(RECORD_ID, courseMetaInfo.recordId)
    assertEquals(STARTED_AT, courseMetaInfo.startedAt)
    assertEquals(STARTED_AT, courseMetaInfo.lastUpdatedAt)
  }

  @Test
  fun `test only last update date is changed on progress update`() = runTest {
    var now = STARTED_AT
    val coursesStorage = CoursesStorage(backgroundScope, newRecordId = { RECORD_ID }, now = { now })
    val course = course {}
    coursesStorage.addCourse(course, "/location", 0, 10)

    now = UPDATED_AT
    coursesStorage.updateCourseProgress(course, "/location", 1, 10)

    val courseMetaInfo = kAssertNotNull(coursesStorage.getCourseMetaInfo(course))
    assertEquals(1, courseMetaInfo.tasksSolved)
    assertEquals(RECORD_ID, courseMetaInfo.recordId)
    assertEquals(STARTED_AT, courseMetaInfo.startedAt)
    assertEquals(UPDATED_AT, courseMetaInfo.lastUpdatedAt)
  }

  @Test
  fun `test new record is created when course is added again`() = runTest {
    var now = STARTED_AT
    var recordIdCounter = 0
    val coursesStorage = CoursesStorage(backgroundScope, newRecordId = { "record-id-${++recordIdCounter}" }, now = { now })
    val course = course {}
    coursesStorage.addCourse(course, "/location", 0, 10)

    now = UPDATED_AT
    coursesStorage.addCourse(course, "/location", 0, 10)

    assertSize(1, coursesStorage.getAllCourses())
    val courseMetaInfo = kAssertNotNull(coursesStorage.getCourseMetaInfo(course))
    assertEquals("record-id-2", courseMetaInfo.recordId)
    assertEquals(UPDATED_AT, courseMetaInfo.startedAt)
    assertEquals(UPDATED_AT, courseMetaInfo.lastUpdatedAt)
  }

  @Test
  fun `test all courses groups`() {
    val coursesStorage = CoursesStorage.getInstance()

    val educatorCourse = course(name = "CC course", courseMode = CourseMode.EDUCATOR) {}
    coursesStorage.addCourse(educatorCourse, "/CC course", 0, 0)

    val inProgressCourse = course(name = "In Progress") {}
    coursesStorage.addCourse(inProgressCourse, "/in_progress", 1, 10)

    val completedCourse = course(name = "Completed") {}
    coursesStorage.addCourse(completedCourse, "/completed", 10, 10)

    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(3, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.my.courses.course.creation"), coursesInGroups.first().name)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups[1].name)
    assertEquals(EduCoreBundle.message("course.dialog.completed"), coursesInGroups[2].name)
  }

  companion object {
    private const val RECORD_ID = "c0ffee00-0000-0000-0000-000000000000"
    private val STARTED_AT: Instant = Instant.parse("2026-05-01T10:15:30Z")
    private val UPDATED_AT: Instant = Instant.parse("2026-06-02T11:16:31Z")
  }
}
