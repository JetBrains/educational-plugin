package com.jetbrains.edu.smartSearch.connector

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.smartSearch.ui.SmartSearchDialog
import com.jetbrains.edu.smartSearch.ui.SmartSearchResultDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class SmartSearchManager {
  suspend fun searchAndShow(smartSearchDialogResult: SmartSearchDialog.SmartSearchDialogResult) {
    withContext(Dispatchers.IO) {
      val smartSearchResult = SmartSearchConnector.getInstance().search(
        smartSearchDialogResult.searchQuery,
        smartSearchDialogResult.collectionName,
        smartSearchDialogResult.numberOfDocuments
      )
      withContext(Dispatchers.EDT) {
        SmartSearchResultDialog(smartSearchResult).show()
      }
    }
  }

  companion object {
    fun getInstance(): SmartSearchManager = service()
  }
}