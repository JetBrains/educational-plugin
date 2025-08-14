package com.jetbrains.edu.learning

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.replaceService
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EducationalExtensionPoint
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import io.mockk.justRun
import io.mockk.spyk

// It's intentionally made as an extension property of `UsefulTestCase` to reduce the probability of incorrect usage
// since a plugin descriptor doesn't make sense in some tests
@Suppress("UnusedReceiverParameter")
val UsefulTestCase.testPluginDescriptor: IdeaPluginDescriptor
  get() = PluginManager.getPlugins().first { it.pluginId.idString.startsWith("com.jetbrains.edu") }

inline fun <reified T: EduConfigurator<*>> UsefulTestCase.registerConfigurator(
  language: Language,
  courseType: String = EduFormatNames.PYCHARM,
  environment: String = DEFAULT_ENVIRONMENT
) {
  val extension = EducationalExtensionPoint<EduConfigurator<*>>()
  extension.language = language.id
  extension.implementationClass = T::class.java.name
  extension.courseType = courseType
  extension.environment = environment
  extension.pluginDescriptor = testPluginDescriptor
  EducationalExtensionPoint.EP_NAME.point.registerExtension(extension, testRootDisposable)
}

/**
 * Temporarily replaces an existing service with a new spy created by [spyk].
 * Behavior of mocked service can be adjusted with [io.mockk.every] API
 *
 * @see [replaceService]
 */
inline fun <reified T : Any> UsefulTestCase.mockService(
  componentManager: ComponentManager,
  disposable: Disposable = testRootDisposable
): T {
  val service = componentManager.getService(T::class.java)
  return spyk(service) {
    if (this is Disposable) {
      // avoid executing actual `dispose` here since it will be passed to real service object
      // when `disposable` is disposed (implementation detail of `replaceService`).
      justRun { this@spyk.dispose() }
    }
    componentManager.replaceService(T::class.java, this, disposable)
  }
}
