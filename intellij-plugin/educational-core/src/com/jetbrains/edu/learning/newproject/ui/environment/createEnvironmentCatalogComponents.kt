package com.jetbrains.edu.learning.newproject.ui.environment

import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.ModalityStateProvider
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.EnvironmentUiKind
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalog
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentService
import com.jetbrains.edu.learning.newproject.ui.EnvironmentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JComponent

/**
 * Load the environment catalog and return the UI components to represent it
 */
internal fun <E : LanguageEnvironment> createEnvironmentCatalogComponents(
  environmentCatalogProvider: LanguageEnvironmentCatalogProvider<E>,
  environmentPresenter: LanguageEnvironmentPresenter<E>,
  course: Course,
  context: UserDataHolder?,
  modalityStateProvider: ModalityStateProvider,
  disposable: CheckedDisposable,
  @RequiresEdt setEnvironmentState: (EnvironmentState<E>) -> Unit
): List<LabeledComponent<JComponent>> {

  @RequiresEdt
  fun EnvironmentCatalogComboBox<E>.updateEnvironmentState() {
    val selectedEnvironment = selectedEnvironment

    val newState = if (selectedEnvironment == null) {
      EnvironmentState.NotSelected
    }
    else {
      EnvironmentState.Selected(selectedEnvironment)
    }

    setEnvironmentState(newState)
  }

  @RequiresEdt
  fun EnvironmentCatalogComboBox<E>.catalogLoaded(catalog: LanguageEnvironmentCatalog<E>) {
    setElements(catalog)
    isEnabled = true
    addActionListener {
      updateEnvironmentState()
    }
    updateEnvironmentState()
  }

  @RequiresEdt
  fun LanguageEnvironmentCatalogProvider<E>.loadCatalog(
    catalogFailedToLoad: suspend () -> Unit = {},
    catalogLoaded: suspend (LanguageEnvironmentCatalog<E>) -> Unit
  ) {
    LanguageEnvironmentService.getInstance().loadEnvironmentCatalog(
      this, course, context, modalityStateProvider, disposable
    ) { environmentCatalog ->
      when (environmentCatalog) {
        is Ok -> {
          catalogLoaded(environmentCatalog.value)
        }

        is Err -> {
          catalogFailedToLoad()
          withContext(Dispatchers.EDT) {
            setEnvironmentState(EnvironmentState.LoadingError(environmentCatalog.error))
          }
        }
      }
    }
  }

  return when (environmentCatalogProvider.uiKind) {
    EnvironmentUiKind.ComboBox -> {
      val environmentComboBox = EnvironmentCatalogComboBox(environmentPresenter).apply {
        isEnabled = false
      }
      environmentCatalogProvider.loadCatalog(catalogFailedToLoad = {
        withContext(Dispatchers.EDT) {
          environmentComboBox.setEmptyElements()
        }
      }) { environmentCatalog ->
        withContext(Dispatchers.EDT) {
          environmentComboBox.catalogLoaded(environmentCatalog)
        }
      }

      listOf(LabeledComponent.create(environmentComboBox, environmentPresenter.label(), BorderLayout.WEST))
    }

    EnvironmentUiKind.Empty -> {
      environmentCatalogProvider.loadCatalog { environmentCatalog ->
        withContext(Dispatchers.EDT) {
          setEnvironmentState(EnvironmentState.Selected(environmentCatalog.recommended))
        }
      }

      emptyList()
    }
  }
}