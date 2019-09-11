package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.refactoring.rename.RenameHandlerRegistry

class EduRenameHandlerRegistry : RenameHandlerRegistry() {

  override fun hasAvailableHandler(dataContext: DataContext): Boolean {
    val hasAvailableEduHandler = RenameHandler.EP_NAME.extensionList.any { it is EduRenameHandler && it.isAvailableOnDataContext(dataContext) }
    return hasAvailableEduHandler || super.hasAvailableHandler(dataContext)
  }

  override fun getRenameHandler(dataContext: DataContext): RenameHandler? {
    val eduHandler = RenameHandler.EP_NAME.extensionList.find { it is EduRenameHandler && it.isAvailableOnDataContext(dataContext) }
    return eduHandler ?: super.getRenameHandler(dataContext)
  }
}
