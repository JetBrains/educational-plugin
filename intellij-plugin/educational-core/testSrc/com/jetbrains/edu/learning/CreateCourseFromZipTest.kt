package com.jetbrains.edu.learning

import com.intellij.testFramework.UsefulTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.nio.file.Files
import java.nio.file.Paths

class CreateCourseFromZipTest : EduTestCase() {

  fun `test create marketplace course from old format zip archive`() {
    doTestCreateMarketplaceCourseFromZip("old format marketplace course.zip", 11)
  }

  fun `test create marketplace course from new format zip archive`() {
    doTestCreateMarketplaceCourseFromZip("new format marketplace course.zip", 12)
  }

  fun `test create edu course from not encrypted old format zip archive`() {
    doTestCreateEduCourseFromZip("old format edu course.zip", 11)
  }

  fun `test create pycharm typed course from encrypted new format zip archive`() {
    doTestCreateEduCourseFromZip("new format edu course.zip", 12)
  }

  private fun doTestCreateMarketplaceCourseFromZip(fileName: String, jsonVersion: Int) {
    val marketplaceCourse = doTestCreateEduCourseFromZip(fileName, jsonVersion)

    assertTrue(marketplaceCourse.isMarketplace)
    assertEquals(1, marketplaceCourse.marketplaceCourseVersion)
  }

  private fun doTestCreateEduCourseFromZip(fileName: String,
                                           jsonVersion: Int,
                                           courseName: String = "Introduction to Python",
                                           lessonsSize: Int = 10): Course {
    val zipPath = "$testDataPath/$fileName"
    val course = EduUtilsKt.getLocalCourse(zipPath) ?: error("Failed to load course from $zipPath")

    UsefulTestCase.assertInstanceOf(course, EduCourse::class.java)
    initializeCourse(project, course)

    UsefulTestCase.assertInstanceOf(course, EduCourse::class.java)
    assertEquals(courseName, course.name)
    assertTrue(course.lessons.size == lessonsSize)

    val task = findTask(0, 0)
    assertEquals("print(\"Hello, world! My name is type your name\")\n", task.taskFiles["hello_world.py"]?.text)
    assertTrue(course.additionalFiles.size == 1)
    assertEquals("additional file text", course.additionalFiles.first().text)

    assertEquals(jsonVersion, (course as EduCourse).formatVersion)

    return course
  }

  fun `test loading course from byte array`() {
    val bytes = Files.readAllBytes(Paths.get(testDataPath, "new format marketplace course.zip"))
    val course = EduUtilsKt.getLocalCourse(bytes) ?: error("Failed to load course from byte array")

    assertEquals("Introduction to Python", course.name)
    assertTrue(course.lessons.size == 10)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/unpack"
  }
}