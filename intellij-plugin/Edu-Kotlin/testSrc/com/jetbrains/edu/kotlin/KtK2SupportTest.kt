package com.jetbrains.edu.kotlin

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduTestCase
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.junit.Test

class KtK2SupportTest : EduTestCase() {
  @Test
  fun checkKotlinCacheService() {
    thisLogger().info("Kotlin plugin mode: ${KotlinPluginModeProvider.currentPluginMode}")
    when (KotlinPluginModeProvider.currentPluginMode) {
      KotlinPluginMode.K1 -> assertNoException(
        IllegalStateException::class.java,
        ThrowableRunnable { project.service<KotlinCacheService>() })

      KotlinPluginMode.K2 -> {
        assertThrows(IllegalStateException::class.java) { project.service<KotlinCacheService>() }
      }
    }
  }
}