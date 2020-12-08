package com.jetbrains.edu.learning.marketplace

import com.intellij.testFramework.UsefulTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils


class CreateMarketplaceCourseFromZipTest : EduTestCase() {

  fun `test create marketplace course from zip`() {
    val zipPath = "$testDataPath/marketplace course.zip"
    val marketplaceCourse = EduUtils.getLocalEncryptedCourse(zipPath) ?: error("Failed to load marketplace course from $zipPath")

    UsefulTestCase.assertInstanceOf(marketplaceCourse, EduCourse::class.java)
    GeneratorUtils.initializeCourse(project, marketplaceCourse)

    assertEquals("Introduction to Python", marketplaceCourse.name)
    assertTrue(marketplaceCourse.lessons.size == 10)

    val task = findTask(0, 0)
    assertEquals("print(\"Hello, world! My name is type your name\")\n", task.taskFiles["hello_world.py"]?.text)
    assertTrue(marketplaceCourse.additionalFiles.size == 1)
    assertEquals("additional file text", marketplaceCourse.additionalFiles.first().text)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/marketplace/unpack"
  }
}