package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.ModalityStateProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings
import com.jetbrains.edu.learning.newproject.ui.environment.LanguageEnvironmentPresenter
import com.jetbrains.edu.learning.newproject.ui.environment.createEnvironmentCatalogComponents
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.newCourseSettings.NewCourseSettingsUI
import com.jetbrains.edu.learning.newproject.ui.newCourseSettings.createNewCourseSettingsComponents
import javax.swing.JComponent

internal sealed interface EnvironmentState<out E : LanguageEnvironment> {
  data object NotSelected : EnvironmentState<Nothing>
  data class Selected<E : LanguageEnvironment>(val environment: E) : EnvironmentState<E>
  data class LoadingError(@NlsContexts.StatusText val message: String) : EnvironmentState<Nothing>
}

/**
 * Represents a UI component for choosing both the environment and the settings for a new course.
 *
 * TODO EDU-8931 After all languages use this class as their [com.jetbrains.edu.learning.LanguageSettings], the [com.jetbrains.edu.learning.LanguageSettings] class should be merged into this one.
 *
 * This class is parameterized by two types:
 * - [E]: extends [com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment], represents the language environment required for the course project.
 * - [S]: extends [com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings], represents the settings for creating a new course.
 *
 * The [environmentCatalogProvider] is used to get a set of environments suitable for the course.
 * The [com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider.collectEnvironmentsForCourse] method is called to load the environment catalog.
 * Until the catalog is loaded, the UI is in the pending state.
 *
 * The [newCourseSettingsUI] is used to get a set of settings that can be applied to the new course.
 * In contrast with [environmentCatalogProvider], the set of settings should not be loaded, and it is available immediately.
 */
class EnvironmentAndNewCourseSettings<E : LanguageEnvironment, S : NewCourseSettings>(
  private val environmentCatalogProvider: LanguageEnvironmentCatalogProvider<E>,
  private val environmentPresenter: LanguageEnvironmentPresenter<E> = LanguageEnvironmentPresenter.NoOp,

  private val newCourseSettingsUI: NewCourseSettingsUI<S>
) : LanguageSettings<E>() {

  @Volatile
  private var environmentState: EnvironmentState<E> = EnvironmentState.NotSelected

  @Volatile
  private var newCourseSettings: S = newCourseSettingsUI.catalog.preferred

  override fun getLanguageSettingsComponents(
    course: Course,
    modalityStateProvider: ModalityStateProvider,
    disposable: CheckedDisposable,
    context: UserDataHolder?,
    uiComponents: UiComponents
  ): List<LabeledComponent<JComponent>> {

    val environmentComponents = createEnvironmentCatalogComponents(
      environmentCatalogProvider, environmentPresenter, course, context, modalityStateProvider, disposable
    ) { state ->
      environmentState = state
      notifyListeners()
    }

    val settingsComponents = if (uiComponents == UiComponents.LANGUAGE_ENVIRONMENT_AND_NEW_COURSE_SETTINGS) {
      createNewCourseSettingsComponents(newCourseSettingsUI) { settings ->
        newCourseSettings = settings
        notifyListeners()
      }
    }
    else {
      emptyList()
    }

    return environmentComponents + settingsComponents
  }

  override fun getSettings(): E {
    when (val state = environmentState) {
      is EnvironmentState.Selected -> return state.environment
      else -> error("No environment selected")
    }
  }

  fun getNewCourseSettings(): S = newCourseSettings

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    return when (val es = environmentState) {
      EnvironmentState.NotSelected -> SettingsValidationResult.Pending
      is EnvironmentState.Selected -> SettingsValidationResult.OK
      is EnvironmentState.LoadingError -> SettingsValidationResult.Ready(ValidationMessage(es.message))
    }
  }
}