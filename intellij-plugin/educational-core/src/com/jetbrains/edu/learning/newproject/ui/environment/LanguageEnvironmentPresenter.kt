package com.jetbrains.edu.learning.newproject.ui.environment

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import javax.swing.Icon

/**
 * Display data for [LanguageEnvironment].
 * Currently used in [EnvironmentCatalogComboBox] to provide information for the cell renderer.
 */
interface LanguageEnvironmentPresenter<in E: LanguageEnvironment> {
  /**
   * Explain users what type of environment they should select.
   * For example, "Python Interpreter" or "JVM".
   */
  fun label(): @NlsContexts.Label String

  /**
   * The main visible text in the ComboBox
   */
  fun name(environment: E): String

  /**
   * The grayed-out text in the ComboBox
   */
  fun secondaryText(environment: E): String?

  /**
   * The icon on the left side of the ComboBox item
   */
  fun icon(environment: E): Icon?

  /**
   * The non-operational presenter to use when no ComboBox is used
   */
  object NoOp : LanguageEnvironmentPresenter<LanguageEnvironment> {
    private fun error(): Nothing {
      error("Empty presentation should not be used")
    }

    override fun label(): String = error()
    override fun name(environment: LanguageEnvironment): String = error()
    override fun secondaryText(environment: LanguageEnvironment): String = error()
    override fun icon(environment: LanguageEnvironment): Icon = error()
  }
}
