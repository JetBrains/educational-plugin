package com.jetbrains.edu.learning.messages

import com.intellij.DynamicBundle

/**
 * Inherit this class to provide user-visible messages.
 * Be aware that these messages might be appended with localization information (see `-Didea.l10n` usages).
 * For properties like OAuth keys inherit [EduPropertiesBundle] instead.
 */
abstract class EduBundle(pathToBundle: String) : DynamicBundle(pathToBundle)