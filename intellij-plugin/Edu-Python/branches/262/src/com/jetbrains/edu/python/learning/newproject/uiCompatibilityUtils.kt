package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.util.whenItemSelectedFromUi
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.python.community.services.systemPython.SystemPython
import com.intellij.python.community.services.systemPython.SystemPythonService
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.cancelOnDispose
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.errors.ready
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.python.PathShortener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JList

// BACKCOMPAT: 2026.1: Inline it
typealias PySdkType = SystemPython

// BACKCOMPAT: 2026.1: Inline it
@Suppress("unused")
fun getLanguageSettingsComponents(
  course: Course,
  disposable: CheckedDisposable,
  context: UserDataHolder?,
  projectSettings: PyProjectSettings,
  notifyListeners: () -> Unit,
): List<LabeledComponent<JComponent>> {
  val combobox = ComboBox<SystemPythonItem>()
  combobox.renderer = SystemPythonListCellRenderer()
  combobox.whenItemSelectedFromUi {
    projectSettings.sdk = it.systemPython
    notifyListeners()
  }

  loadSystemPythonsAsync(disposable) { systemPythons ->
    withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
      systemPythons.forEach { combobox.addItem(it) }
      combobox.selectedItem = systemPythons.firstOrNull()
      notifyListeners()
    }
  }
  return listOf(
    LabeledComponent.create(combobox, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST)
  )
}

private fun loadSystemPythonsAsync(disposable: CheckedDisposable, consumer: suspend (List<SystemPythonItem>) -> Unit) {
  PyService.getInstance().scope.launch {
    val systemPythons = SystemPythonService().findSystemPythons()
    val items = systemPythons.map { it.toComboBoxItem() }
    consumer(items)
  }.cancelOnDispose(disposable)
}


@Service(Service.Level.APP)
private class PyService(val scope: CoroutineScope) {

  companion object {
    fun getInstance(): PyService = service()
  }
}

private data class SystemPythonItem(
  val systemPython: SystemPython,
  val title: String,
  val secondaryText: String,
)

private suspend fun SystemPython.toComboBoxItem(): SystemPythonItem {
  val version = pythonInfo.languageLevel.toString()
  val freeThreadedSuffix = if (pythonInfo.freeThreaded) " free-threaded" else ""

  return SystemPythonItem(
    systemPython = this,
    title = "Python $version$freeThreadedSuffix",
    secondaryText = PathShortener.shorten(pythonBinary),
  )
}

private class SystemPythonListCellRenderer : ColoredListCellRenderer<SystemPythonItem>() {
  override fun customizeCellRenderer(
    list: JList<out SystemPythonItem>,
    value: SystemPythonItem?,
    index: Int,
    selected: Boolean,
    hasFocus: Boolean,
  ) {
    if (value == null) {
      append(EduPythonBundle.getMessage("no.python.interpreter"))
      return
    }

    value.systemPython.ui?.icon?.let { icon = it }
    append(value.title)
    append("  ${value.secondaryText}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}

@Suppress("unused")
fun validate(isSettingsInitialized: Boolean, projectSettings: PyProjectSettings, course: Course): SettingsValidationResult {
  if (!isSettingsInitialized) return SettingsValidationResult.Pending


  if (projectSettings.sdk == null) {
    return ValidationMessage(
      EduPythonBundle.message("error.no.python.interpreter", ENVIRONMENT_CONFIGURATION_LINK_PYTHON),
      ENVIRONMENT_CONFIGURATION_LINK_PYTHON
    ).ready()
  }

  return SettingsValidationResult.OK
}