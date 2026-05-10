package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import org.junit.Test
import kotlin.test.assertIs

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
}
