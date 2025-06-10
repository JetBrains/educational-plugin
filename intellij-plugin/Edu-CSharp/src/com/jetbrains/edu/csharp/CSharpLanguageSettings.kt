package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.csharp.messages.EduCSharpBundle
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.rider.environmentSetup.EnvironmentAnalyzer
import com.jetbrains.rider.environmentSetup.analyzers.DotNetCoreEnvironmentComponent
import java.awt.BorderLayout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

class CSharpLanguageSettings : LanguageSettings<CSharpProjectSettings>() {
  private var targetFrameworkVersion: String = DEFAULT_DOT_NET
  override fun getSettings(): CSharpProjectSettings = CSharpProjectSettings(targetFrameworkVersion)
  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {

    if (course is HyperskillCourse) return emptyList()
    val versions = course.configurator?.courseBuilder?.getSupportedLanguageVersions()?.toTypedArray()
                   ?: error("No builder associated with course found")
    val langStandardComboBox = ComboBox(versions)
    val courseTargetFrameworkVersion = course.languageVersion
    if (courseTargetFrameworkVersion != null && versions.contains(courseTargetFrameworkVersion)) {
      targetFrameworkVersion = courseTargetFrameworkVersion
    }
    langStandardComboBox.selectedItem = targetFrameworkVersion

    langStandardComboBox.addItemListener {
      targetFrameworkVersion = it.item.toString()
      notifyListeners()
    }
    return listOf(
      LabeledComponent.create(
        langStandardComboBox,
        EduCSharpBundle.getMessage("target.framework"),
        BorderLayout.WEST
      )
    )
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    val project = course?.project ?: return super.validate(course, courseLocation)

    val isDotNetInstalled = checkDotNetInstallation(project)

    return if (!isDotNetInstalled) {
      showInstallMSBuildNotification(project)

      SettingsValidationResult.OK
    }
    else {
      SettingsValidationResult.OK
    }
  }

  private fun checkDotNetInstallation(project: Project): Boolean {
    val future = CompletableFuture<Boolean>()

    val analyzer = EnvironmentAnalyzer(project, false)

    analyzer.analyze(
      onDone = { components, _, _ ->
        val dotNetComponent = components.find { it is DotNetCoreEnvironmentComponent } as? DotNetCoreEnvironmentComponent
        val isInstalled = dotNetComponent?.let { component ->
          val version = component.version
          version != "Unknown" && version.isNotBlank() && version != "Not installed"
        } ?: false

        future.complete(isInstalled)
      },
      onError = { _ ->
        future.complete(false)
      },
      filter = { component -> component is DotNetCoreEnvironmentComponent }
    )

    return try {
      future.get(10, TimeUnit.SECONDS)
    }
    catch (_: Exception) {
      false
    }
  }

}