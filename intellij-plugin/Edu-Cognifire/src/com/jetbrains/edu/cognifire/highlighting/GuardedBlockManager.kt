package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.cognifire.ui.ProdeReadonlyFragmentHandler
import com.intellij.openapi.fileEditor.FileDocumentManager

@State(name = "GuardedBlockManager", storages = [Storage("storage.xml")])
class GuardedBlockManager : PersistentStateComponent<GuardedBlockManager> {
  val blocks = mutableMapOf<String, RangeMarker>()

  private fun VirtualFile.getDocument() = FileDocumentManager.getInstance().getDocument(this)

  fun removeGuardedBlock(id: String, file: VirtualFile) {
    val document = file.getDocument() ?: return
    blocks[id]?.let { range -> document.removeGuardedBlock(range) }
    blocks.remove(id)
  }

  fun addAllGuardBlocks(id: String, file: VirtualFile) {
    val range = blocks[id] ?: return
    val document = file.getDocument() ?: return
    document.createGuardedBlock(range.startOffset, range.endOffset)
//    setReadonlyFragmentModificationHandler(document, false)
  }

  fun addGuardedBlock(document: Document, startOffset: Int, endOffset: Int, id: String, isPromptChange: Boolean) {
     blocks[id]?.let { range -> document.removeGuardedBlock(range) }
    val block = document.createGuardedBlock(startOffset, endOffset)
    blocks[id] = block
//    setReadonlyFragmentModificationHandler(document, isPromptChange)
  }

  // TODO: fix
  private fun setReadonlyFragmentModificationHandler(document: Document, isPromptChange: Boolean) {
    val handler = EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document)
    if (handler !is ProdeReadonlyFragmentHandler) {
      val editor = EditorFactory.getInstance().getEditors(document).firstOrNull() ?: return
      EditorActionManager.getInstance()
        .setReadonlyFragmentModificationHandler(document, ProdeReadonlyFragmentHandler(editor, isPromptChange))
    }
  }

  override fun getState() = this

  override fun loadState(state: GuardedBlockManager) {
    blocks.putAll(state.blocks)
  }

  companion object {
    fun getInstance(): GuardedBlockManager = service()
  }
}