package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.*

@State(name = "ProdeStateManager", storages = [Storage("storage.xml")])
class ProdeStateManager : PersistentStateComponent<ProdeStateManager> {
  val listenerMetadata = mutableMapOf<String, MutableList<ListenerType>>()
  val prodeData = mutableMapOf<String, ListenerData>()

  fun addProde(
    prodeID: String,
    promptToCodeLines: Map<Int, List<Int>>,
    codeToPromptLines: Map<Int, List<Int>>,
    initialPrompt: String,
    initialCode: String
  ) {
    prodeData[prodeID] = ListenerData(
      promptToCodeLines, codeToPromptLines, initialPrompt, initialCode
    )
  }

  fun addListener(prodeId: String, type: ListenerType) {
    listenerMetadata.getOrPut(prodeId) { mutableListOf() }.add(type)
  }

  fun clearAllListeners(prodeId: String) {
    listenerMetadata[prodeId]?.clear()
  }

  fun clearAllListenersOfType(prodeId: String, type: ListenerType) {
    listenerMetadata[prodeId]?.removeAll { it == type }
  }

  override fun getState(): ProdeStateManager {
    return this
  }

  override fun loadState(state: ProdeStateManager) {
    listenerMetadata.putAll(state.listenerMetadata)
    prodeData.putAll(state.prodeData)
  }

  companion object {
    fun getInstance(): ProdeStateManager {
      return service()
    }
  }

  enum class ListenerType {
    DOCUMENT, MOUSE_MOTION
  }

  data class ListenerData(
    val promptToCodeLines: Map<Int, List<Int>>,
    val codeToPromptLines: Map<Int, List<Int>>,
    val initialPrompt: String,
    val initialCode: String
  )
}