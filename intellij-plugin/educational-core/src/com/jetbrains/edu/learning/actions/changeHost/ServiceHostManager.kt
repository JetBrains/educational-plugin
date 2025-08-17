package com.jetbrains.edu.learning.actions.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.TestOnly

abstract class ServiceHostManager<E>(val name: @NlsSafe String, val hostEnumClass: Class<E>)
  where E: Enum<E>,
        E: ServiceHostEnum {

  abstract val default: E
  abstract val other: E

  var selectedHost: SelectedServiceHost<E>
    get() = currentHost()
    set(value) {
      if (value == currentHost()) return

      PropertiesComponent.getInstance().setValue(valueKey, value.value.name, default.name)
      val url = if (value.value == other) value.url else null
      PropertiesComponent.getInstance().setValue(urlKey, url)

      onHostChanged()
    }

  private val valueKey: String get() = "edu.selectedHost.${hostEnumClass.simpleName}.value"
  private val urlKey: String get() = "edu.selectedHost.${hostEnumClass.simpleName}.url"

  private fun currentHost(): SelectedServiceHost<E> {
    val stringValue = PropertiesComponent.getInstance().getValue(valueKey)
    val hostValue = hostEnumClass.enumConstants.find { it.name == stringValue } ?: default

    val url = if (hostValue == other) {
      PropertiesComponent.getInstance().getValue(urlKey, hostValue.url)
    }
    else {
      hostValue.url
    }

    return SelectedServiceHost(hostValue, url)
  }

  protected open fun onHostChanged() {}

  @TestOnly
  fun reset() {
    PropertiesComponent.getInstance().unsetValue(urlKey)
    PropertiesComponent.getInstance().unsetValue(valueKey)
  }

  data class SelectedServiceHost<E>(val value: E, val url: String = value.url) where E: Enum<E>, E: ServiceHostEnum
}
