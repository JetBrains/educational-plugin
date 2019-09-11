package com.jetbrains.edu.learning.handlers

import com.intellij.refactoring.rename.RenameHandler

/**
 * Marker interface to distinguish the plugin rename handlers from others
 *
 * @see EduRenameHandlerRegistry
 */
interface EduRenameHandler : RenameHandler
