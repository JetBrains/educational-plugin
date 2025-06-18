package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages additional information that should be displayed next to the Check button.
 * The additional information is an arbitrary HTML text.
 * Different users of the service could set texts independently of each other by using different
 * String keys for adding or removing information.
 * All the texts with different keys are joined into one HTML, separated by the `<br>` tag.
 */
@Service(Service.Level.PROJECT)
class CheckButtonAdditionalInformationManager {

  private val _additionalInformation: MutableStateFlow<CheckButtonAdditionalInformation> =
    MutableStateFlow(CheckButtonAdditionalInformation(emptyMap()))

  val additionalInformation: StateFlow<CheckButtonAdditionalInformation>
    get() = _additionalInformation.asStateFlow()

  fun setInformation(key: String, value: String) {
    _additionalInformation.update { it.put(key, value) }
  }

  fun removeInformation(key: String) {
    _additionalInformation.update { it.remove(key) }
  }

  companion object {
    fun getInstance(project: Project): CheckButtonAdditionalInformationManager =
      project.service<CheckButtonAdditionalInformationManager>()
  }
}

/**
 * The additional information displayed next to the Check button.
 * Represents a key-value set of HTML texts.
 */
class CheckButtonAdditionalInformation(private val information: Map<String, String>) {

  /**
   * Joins all the texts into one HTML with the `<br>` separator.
   * The order of texts is the order in which texts were added
   */
  val allTexts: String?
    get() {
      val values = information.values
      if (values.isEmpty()) return null
      return values.joinToString(separator = "<br>")
    }

  internal fun put(key: String, value: String): CheckButtonAdditionalInformation {
    return CheckButtonAdditionalInformation(information + (key to value))
  }

  internal fun remove(key: String): CheckButtonAdditionalInformation {
    return CheckButtonAdditionalInformation(information - key)
  }
}