package com.jetbrains.edu.commandLine

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.test.assertIs

class LocalCourseSourceTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    // For some reason, access to course files in test data can be forbidden.
    // Most likely, it's because of race condition.
    // Let's explicitly allow access to any file in test data to avoid such errors
    VfsRootAccess.allowRootAccess(testRootDisposable, Paths.get(testDataPath).absolutePathString())
  }

  // Important not to hold EDT during test execution.
  // Otherwise, it will lead to deadlock during project opening, which happens under the hood
  override fun runInDispatchThread(): Boolean = false

  @Test
  fun `test load educator local course`() = runTest {
    // when
    val result = CourseSource.LOCAL.loadCourse("$testDataPath/educatorPlainTextCourse")

    // then
    assertIs<Ok<EduCourse>>(result)

    val course = result.value
    assertEquals("Plain Text Course", course.name)
    assertEquals(1, course.lessons.size)

    val lesson = course.lessons.single()
    assertEquals("lesson1", lesson.name)
    assertEquals(2, lesson.taskList.size)

    val theoryTask = lesson.taskList[0]
    val eduTask = lesson.taskList[1]
    assertIs<TheoryTask>(theoryTask)
    assertEquals("theory-task", theoryTask.name)
    assertIs<EduTask>(eduTask)
    assertEquals("edu-task", eduTask.name)
  }

  @Test
  fun `test load student local course`() = runTest {
    // when
    val result = CourseSource.LOCAL.loadCourse("$testDataPath/studentPlainTextCourse")

    // then
    assertIs<Err<String>>(result)
  }
}
