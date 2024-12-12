package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.*
import com.jetbrains.edu.cognifire.highlighting.highlighers.ProdeHighlighter
import com.jetbrains.edu.cognifire.highlighting.highlighers.UncommitedChangesHighlighter

/**
 * Manages the highlighters in the editor. Allows adding and clearing highlighters for specific ranges and lines of code.
 */
@State(name = "HighlighterManager", storages = [Storage("storage.xml")])
class HighlighterManager : PersistentStateComponent<HighlighterManager> {
  val highlighters = mutableMapOf<String, MutableList<ProdeHighlighter>>()

  fun clearAll(id: String) {
    highlighters[id]?.forEach {
      it.markupHighlighter?.dispose()
    }
    highlighters.remove(id)
  }

  inline fun<reified T: ProdeHighlighter> clearProdeHighlighters(id: String) {
    highlighters[id]?.filterIsInstance<T>()?.forEach {
      it.markupHighlighter?.dispose()
    }
    highlighters[id]?.removeAll { it is T }
  }

  fun addProdeHighlighter(highlighter: ProdeHighlighter, id: String) {
    highlighter.addMarkupHighlighter()
    highlighters.getOrPut(id) { mutableListOf() }.add(highlighter)
  }

  fun highlightAllUncommitedChanges(id: String) {
    highlighters[id]?.filterIsInstance<UncommitedChangesHighlighter>()?.forEach { highlighter ->
      highlighter.addHighlighter()
    }
  }

  override fun getState() = this

  override fun loadState(state: HighlighterManager) {
    highlighters.putAll(state.highlighters)
  }

  companion object {
    fun getInstance(): HighlighterManager = service()
  }
}