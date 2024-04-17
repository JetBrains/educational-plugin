package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.test.assertIs

class FileContentsDeserializationTest : EduTestCase() {

  @Test
  fun `test reading file contents from course_dot_json version 16`() {
    // all file contents should be read as undefined

    val course = createCourseFromJson("testData/fileContents/course archive 16.json", CourseMode.STUDENT)

    val allEduFiles = course.allTasks[0].taskFiles.values + course.additionalFiles
    for (eduFile in allEduFiles) {
      assertIs<UndeterminedContents>(eduFile.contents, "all read file contents must be undetermined because is_binary field is absent")
    }
  }

  @Test
  fun `test reading file contents from course_dot_json version 17`() {
    // all file contents should be read as either binary or textual

    val course = createCourseFromJson("testData/fileContents/course archive 17.json", CourseMode.STUDENT)

    val allEduFiles = course.allTasks[0].taskFiles.values + course.additionalFiles

    val textualExtensions = setOf("txt", "asdf", "json")

    for (eduFile in allEduFiles) {
      val extension = Path.of(eduFile.name).extension
      if (extension in textualExtensions) {
        assertIs<TextualContents>(eduFile.contents, "${eduFile.name} must have textual contents")
      }
      else {
        assertIs<BinaryContents>(eduFile.contents, "${eduFile.name} must have binary contents")
      }
    }
  }
}