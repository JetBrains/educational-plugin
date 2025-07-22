package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils
import com.jetbrains.edu.learning.EduNames.COURSE_IGNORE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test
import kotlin.test.DefaultAsserter.assertNotEquals

class CourseIgnoreAssociationsTest : CourseGenerationTestBase<EduProjectSettings>() {

  override val defaultSettings = EmptyProjectSettings

  @Test
  fun `test _courseignore file works even without file type association`() = doTestCourseignoreWorksEvenWithWrongFileTypeAssociation {
    FileTypeManager.getInstance().removeAssociation(CourseIgnoreFileType, ExactFileNameMatcher(COURSE_IGNORE))
  }

  @Test
  fun `test _courseignore file works even if associated with a wrong file type`() = doTestCourseignoreWorksEvenWithWrongFileTypeAssociation {
    FileTypeManager.getInstance().associate(HtmlFileType.INSTANCE, ExactFileNameMatcher(COURSE_IGNORE))
  }

  @Test
  fun `test _courseignore association after project reopening with no association`() = doTestCorrectAssociationAfterProjectOpening {
    FileTypeManager.getInstance().removeAssociation(CourseIgnoreFileType, ExactFileNameMatcher(COURSE_IGNORE))
  }

  @Test
  fun `test _courseignore association after project reopening with wrong association`() = doTestCorrectAssociationAfterProjectOpening {
    FileTypeManager.getInstance().associate(HtmlFileType.INSTANCE, ExactFileNameMatcher(COURSE_IGNORE))
  }

  private fun doTestCourseignoreWorksEvenWithWrongFileTypeAssociation(doWrongAssociation: () -> Unit) {
    val ignoredFile = "ignored.txt"
    val course = course {
      additionalFiles {
        eduFile(COURSE_IGNORE, ignoredFile)
        eduFile(ignoredFile)
      }
    }
    createCourseStructure(course)
    val courseIgnoreFile = project.courseDir.findChild(COURSE_IGNORE)!!

    // change file type associations after the project is created
    runInEdtAndWait {
      runWriteAction {
        doWrongAssociation()
      }
    }

    val actualCourseIgnoreFileTypeBefore = FileTypeManager.getInstance().getFileTypeByFile(courseIgnoreFile)
    assertNotEquals(".courseignore must be associated wrongly", actualCourseIgnoreFileTypeBefore, CourseIgnoreFileType)

    val additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(course.configurator, project)
    assertSameElements(additionalFiles.map { it.name }, listOf())
  }

  private fun doTestCorrectAssociationAfterProjectOpening(doWrongAssociation: () -> Unit) {
    // change file type associations before the project is created
    runInEdtAndWait {
      runWriteAction {
        doWrongAssociation()
      }
    }

    val course = course {
      additionalFiles {
        eduFile(COURSE_IGNORE, "")
      }
    }
    createCourseStructure(course) // this runs EduStartupActivity
    val courseIgnoreFile = project.courseDir.findChild(COURSE_IGNORE)!!

    val actualCourseIgnoreFileTypeAfter = FileTypeManager.getInstance().getFileTypeByFile(courseIgnoreFile)
    assertEquals(".courseignore must be associated with the CourseIgnoreFileType", actualCourseIgnoreFileTypeAfter, CourseIgnoreFileType)
  }

  override fun tearDown() {
    try {
      runWriteAction {
        FileTypeManager.getInstance().associate(CourseIgnoreFileType, ExactFileNameMatcher(COURSE_IGNORE))
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
