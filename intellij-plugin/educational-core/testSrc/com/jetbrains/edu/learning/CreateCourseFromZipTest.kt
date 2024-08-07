package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.courseFormat.zip.ZipContents
import com.jetbrains.edu.learning.storage.ContentsFromLearningObjectsStorage
import com.jetbrains.edu.learning.storage.LearningObjectsStorageManager
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertIs

class CreateCourseFromZipTest : EduTestCase() {

  @Test
  fun `test create marketplace course from old format zip archive`() {
    doTestCreateMarketplaceCourseFromZip("old format marketplace course.zip", 11)
  }

  @Test
  fun `test create marketplace course from new format zip archive`() {
    doTestCreateMarketplaceCourseFromZip("new format marketplace course.zip", 12)
  }

  @Test
  fun `test create edu course from not encrypted old format zip archive`() {
    doTestCreateEduCourseFromZip("old format edu course.zip", 11)
  }

  @Test
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

    assertInstanceOf(course, EduCourse::class.java)
    initializeCourse(project, course)

    assertInstanceOf(course, EduCourse::class.java)
    assertEquals(courseName, course.name)
    assertTrue(course.lessons.size == lessonsSize)

    val task = findTask(0, 0)
    assertEquals("print(\"Hello, world! My name is type your name\")\n", task.taskFiles["hello_world.py"]?.text)
    assertTrue(course.additionalFiles.size == 1)
    assertEquals("additional file text", course.additionalFiles.first().text)

    assertEquals(jsonVersion, (course as EduCourse).formatVersion)

    return course
  }

  @Test
  fun `test reading archive in the format with files outside json`() {
    val zipPath = "$testDataPath/Kotlin_Course_to_test_archive_reading.zip"
    val course = EduUtilsKt.getLocalCourse(zipPath) ?: error("Failed to load course from $zipPath")
    course.init(false)

    assertEquals("Kotlin Course to test archive reading", course.name)

    course.visitTasks { task ->
      for (taskFile in task.taskFiles.values) {
        // all task files are textual; have file name inside; have lesson1.task1 name inside
        val contents = taskFile.contents as? TextualContents ?: error("all task files are textual in the example")
        assertContains(contents.text, taskFile.name.substringAfterLast("/"))
        assertContains(contents.text, "${task.lesson.name}.${task.name}")
      }
    }

    // must have additional file additional_file.txt with the same contents
    // build.gradle
    // nonempty image img.png
    assertEquals(3, course.additionalFiles.size)

    for (additionalFile in course.additionalFiles) {
      val contents = additionalFile.contents
      when (additionalFile.name) {
        "additional_file.txt" -> assertEquals("additional_file.txt", (contents as TextualContents).text)
        "img.png" -> {
          assertIs<BinaryContents>(contents)
          assertTrue(contents.bytes.isNotEmpty())
        }
      }
    }
  }

  @Test
  fun `no ZipContents after course archive is fully opened`() {
    val zipPath = "$testDataPath/Kotlin_Course_to_test_archive_reading.zip"
    val course = EduUtilsKt.getLocalCourse(zipPath) ?: error("Failed to load course from $zipPath")

    course.visitEduFiles { eduFile ->
      assertIs<ZipContents>(eduFile.contents)
    }

    // This sets the course to the StudyTaskManager and thus fires LearningObjectsPersister to persist all the contests to the learning
    // object storage
    initializeCourse(project, course)
    LearningObjectsStorageManager.getInstance(project).waitForPersisting()

    course.visitEduFiles { eduFile ->
      assertIs<ContentsFromLearningObjectsStorage>(
        eduFile.contents,
        "Contents must come from the learning objects storage: ${eduFile.contents.javaClass}}"
      )
    }
  }

  @Test
  fun `reading archive with broken paths`() {
    val zipPath = "$testDataPath/Kotlin_Course_with_broken_paths.zip"
    val course = EduUtilsKt.getLocalCourse(zipPath) ?: error("Failed to load course from $zipPath")

    // The just loaded course references files inside the Zip archive, but their paths are wrong.
    // After course initialization we persist empty contents in Learning Objects Storage, and log errors about that
    initializeCourse(project, course)

    course.visitEduFiles { eduFile ->
      when (val contents = eduFile.contents) {
        is BinaryContents -> assertTrue("bytes contents must be empty", contents.bytes.isEmpty())
        is TextualContents -> assertEmpty(contents.text)
        is UndeterminedContents -> fail("Impossible to have undetermined contents")
      }
    }
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/unpack"
  }
}