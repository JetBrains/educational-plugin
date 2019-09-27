@file:JvmName("TaskDescriptionUtil")

package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.keymap.KeymapUtil

private const val SHORTCUT = "shortcut"
private const val SHORTCUT_ENTITY = "&$SHORTCUT:"
private const val SHORTCUT_ENTITY_ENCODED = "&amp;$SHORTCUT:"

fun replaceActionIDsWithShortcuts(text: StringBuffer) {
  var lastIndex = 0
  while (lastIndex < text.length) {
    lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex)
    var shortcutEntityLength = SHORTCUT_ENTITY.length
    if (lastIndex < 0) {
      //`&` symbol might be replaced with `&amp;`
      lastIndex = text.indexOf(SHORTCUT_ENTITY_ENCODED)
      if (lastIndex < 0) {
        return
      }
      shortcutEntityLength = SHORTCUT_ENTITY_ENCODED.length
    }
    val actionIdStart = lastIndex + shortcutEntityLength
    val actionIdEnd = text.indexOf(";", actionIdStart)
    if (actionIdEnd < 0) {
      return
    }
    val actionId = text.substring(actionIdStart, actionIdEnd)
    var shortcutText = KeymapUtil.getFirstKeyboardShortcutText(actionId)
    if (shortcutText.isEmpty()) {
      shortcutText = "<no shortcut for action $actionId>"
    }
    text.replace(lastIndex, actionIdEnd + 1, shortcutText)
    lastIndex += shortcutText.length
  }
}

fun String.replaceEncodedShortcuts() = this.replace(SHORTCUT_ENTITY_ENCODED, SHORTCUT_ENTITY)

fun String.toShortcut(): String = "${SHORTCUT_ENTITY}$this;"

fun String.containsShortcut(): Boolean = startsWith(SHORTCUT_ENTITY) || startsWith(SHORTCUT_ENTITY_ENCODED)
