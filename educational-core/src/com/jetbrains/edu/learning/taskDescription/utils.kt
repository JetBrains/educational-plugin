@file:JvmName("TaskDescriptionUtil")

package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.keymap.KeymapUtil

private const val SHORTCUT = "shortcut"
private const val SHORTCUT_ENTITY = "&$SHORTCUT:"
private const val SHORTCUT_ENTITY_ENCODED = "&amp;$SHORTCUT:"
const val IMG_TAG = "img"
const val SCRIPT_TAG = "script"
const val SRC_ATTRIBUTE = "src"
private val HYPERSKILL_TAGS = tagsToRegex({ "\\[$it](.*)\\[/$it]" }, "HINT", "PRE", "META") +
                              tagsToRegex({ "\\[$it-\\w+](.*)\\[/$it]" }, "ALERT")


private fun tagsToRegex(pattern: (String) -> String, vararg tags: String): List<Regex> = tags.map { pattern(it).toRegex() }

// see EDU-2444
fun removeHyperskillTags(text: StringBuffer) {
  var result: String = text.toString()
  for (regex in HYPERSKILL_TAGS) {
    result = result.replace(regex) { it.groupValues[1] }
  }

  text.delete(0, text.length)
  text.append(result)
}

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
