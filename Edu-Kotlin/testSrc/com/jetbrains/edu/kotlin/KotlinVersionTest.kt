package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.*


class KotlinVersionTest : EduTestCase() {

  fun `test Kotlin version extraction from Kotlin plugin change notes`() {
    val version = kotlinVersionFromPlugin(KOTLIN_PLUGIN_ID)
    assertNotNull("Failed to get Kotlin version from the Kotlin Plugin.", version)
    val kotlinVersionFromPlugin = KotlinVersion(version!!)
    assertTrue("Kotlin version from the Kotlin Plugin is less than default Kotlin version.", kotlinVersionFromPlugin >= DEFAULT_KOTLIN_VERSION)
  }

}