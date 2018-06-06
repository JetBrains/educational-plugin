package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.ActionCallback
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.ui.CoursePanel
import org.jetbrains.annotations.TestOnly
import javax.swing.JComponent

@Volatile
private var MOCK: NewCoursesNotifierUi? = null

fun showNewCoursesNotification(courses: List<RemoteCourse>) {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("You should set mock ui via `withMockNewCoursesNotifierUi`")
  } else {
    NotificationNewCourseNotifierUi
  }
  ui.showNotification(courses)
}

@TestOnly
fun withMockNewCoursesNotifierUi(mockUi: NewCoursesNotifierUi, action: () -> ActionCallback): ActionCallback {
  MOCK = mockUi
  try {
    return action().doWhenProcessed { MOCK = null }
  } catch (e: Throwable) {
    MOCK = null
    throw e
  }
}

interface NewCoursesNotifierUi {
  fun showNotification(courses: List<RemoteCourse>)
}

object NotificationNewCourseNotifierUi : NewCoursesNotifierUi {

  override fun showNotification(courses: List<RemoteCourse>) {
    check(courses.isNotEmpty()) { "course list should be not empty" }
    val title = if (courses.size == 1) "New course available" else "New courses available"
    val message = courses.joinToString(separator = " <br> ") { "<a href=\"${it.id}\">${it.name}</a>" }
    val notification = Notification("New.course", title, message, NotificationType.INFORMATION, NotificationListener { notification, e ->
      notification.expire()
      val course = courses.find { it.id.toString() == e.description } ?: return@NotificationListener
      val configurator = course.configurator ?: return@NotificationListener
      SingleCourseDialog(course, configurator).show()
    })
    notification.notify(null)
  }

  private class SingleCourseDialog(
    private val course: Course,
    private val configurator: EduConfigurator<*>
  ) : DialogWrapper(true) {

    private val panel: CoursePanel = CoursePanel(/*isIndependent = */ true, /*isLocationFieldNeeded = */ true).apply {
      preferredSize = JBUI.size(WIDTH, HEIGHT)
      minimumSize = JBUI.size(WIDTH, HEIGHT)
    }

    init {
      title = "Create Course"
      setOKButtonText("Create")
      panel.bindCourse(course)
      init()
    }

    override fun createCenterPanel(): JComponent = panel

    override fun doOKAction() {
      val settings = panel.projectSettings
      val location = panel.locationString ?: error("Location should be not null")
      configurator.courseBuilder
        .getCourseProjectGenerator(course)
        ?.doCreateCourseProject(location, settings)
      close(OK_EXIT_CODE)
    }

    companion object {
      private const val WIDTH: Int = 370
      private const val HEIGHT: Int = 330
    }
  }
}
