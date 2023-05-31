package com.jetbrains.edu.learning.checkio

import com.intellij.openapi.Disposable
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.newProjectUI.CheckiOCoursesPanel
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon
import kotlin.math.max

class CheckiOPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CheckiOPlatformProvider())
}

class CheckiOPlatformProvider : CoursesPlatformProvider() {
  override val name: String = CheckiONames.CHECKIO

  override val icon: Icon get() = EducationalCoreIcons.CheckiO.to24()

  override fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel = CheckiOCoursesPanel(this, scope, disposable)

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    return if (EduUtilsKt.isAndroidStudio()) {
      emptyList()
    }
    else {
      val courses = listOf(
        CheckiOCourse(CheckiONames.PY_CHECKIO, "${EduNames.PYTHON} ${EduNames.PYTHON_3_VERSION}"),
//        CheckiOCourse(CheckiONames.JS_CHECKIO, EduNames.JAVASCRIPT)
      )

      CoursesGroup(courses).asList()
    }
  }

  private fun Icon.to24() = IconUtil.scale(this, null, JBUIScale.scale(24f / max(iconHeight, iconWidth)))
}