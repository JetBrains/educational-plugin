package com.jetbrains.edu.learning.update

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.ActionCallback
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class NewCoursesNotifierTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    EduSettings.getInstance().init()
  }

  fun `test notification is shown`() {
    val course = createCourse(0)

    doTest(1, listOf(course)) { if (it == 0) listOf(course) else emptyList() }
  }

  fun `test notifications is scheduled`() {
    val firstCourse = createCourse(0)
    val secondCourse = createCourse(1)
    val oldCourse = createCourse(2, false)

    doTest(2, listOf(firstCourse, secondCourse)) {
      when (it) {
        0 -> listOf(firstCourse)
        1 -> listOf(secondCourse, oldCourse)
        else -> emptyList()
      }
    }
  }

  fun `test do not show notification about same course twice`() {
    val firstCourse = createCourse(0)
    val secondCourse = createCourse(1)
    val thirdCourse = createCourse(0)

    doTest(2, listOf(firstCourse, secondCourse)) {
      when (it) {
        0 -> listOf(firstCourse)
        1 -> listOf(secondCourse, thirdCourse)
        else -> emptyList()
      }
    }
  }

  fun `test do not show notification for not featured courses`() {
    val firstCourse = createCourse(0)
    val secondCourse = createCourse(5)

    doTest(1, listOf(firstCourse)) { if (it == 0) listOf(firstCourse, secondCourse) else emptyList() }
  }

  fun `test do not show notification for course with increased updateDate`() {
    val course = createCourse(0)
    course.createDate = Date(EduSettings.getInstance().lastTimeChecked - DateFormatUtil.DAY)
    course.updateDate = Date(System.currentTimeMillis())

    doTest(1, emptyList()) { if (it == 0) listOf(course) else emptyList() }
  }

  private fun doTest(expectedCheckNumber: Int, expectedCourses: List<EduCourse>, courseProducer: (Int) -> List<EduCourse>) {
    val newCoursesNotifier = NewCoursesNotifier(testRootDisposable) { listOf(0, 1, 2) }
    PlatformTestUtil.registerExtension(CoursesProvider.EP_NAME, TestCoursesProvider(courseProducer), testRootDisposable)

    val actionCallback = ActionCallback()
    try {
      val newCourses = Collections.synchronizedList(mutableListOf<EduCourse>())
      withMockNewCoursesNotifierUi(object : NewCoursesNotifierUi {
        override fun showNotification(courses: List<EduCourse>) {
          newCourses.addAll(courses)
        }
      }) {
        newCoursesNotifier.setNewCheckInterval(DateFormatUtil.SECOND)
        newCoursesNotifier.scheduleNotificationInternal()
        actionCallback
      }
      Thread.sleep(500)
      assertEquals(newCoursesNotifier.invocationNumber(), 0)

      Thread.sleep((1 + expectedCheckNumber) * DateFormatUtil.SECOND)

      assertTrue("Invocation number should be at least `$expectedCheckNumber` but `${newCoursesNotifier.invocationNumber()}`",
                 newCoursesNotifier.invocationNumber() >= expectedCheckNumber)
      assertEquals(expectedCourses, newCourses)
    } finally {
      actionCallback.setDone()
    }
  }

  private fun createCourse(courseId: Int, isNew: Boolean = true): EduCourse = EduCourse().apply {
    name = "Test Course $courseId"
    id = courseId
    language = PlainTextLanguage.INSTANCE.id
    createDate = Date(System.currentTimeMillis() + if (isNew) DateFormatUtil.DAY else -DateFormatUtil.DAY)
  }

  private class TestCoursesProvider(private val producer: (Int) -> List<EduCourse>) : CoursesProvider {

    private val counter = AtomicInteger()

    override fun loadCourses(): List<Course> {
      checkIsBackgroundThread()
      return producer(counter.getAndIncrement())
    }
  }
}
