package com.jetbrains.edu.jarvis.highlighting

import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.callFunction
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.createVariable
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.isolatedCode
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.setVariable
import com.jetbrains.edu.jarvis.highlighting.GrammarRegex.storeVariable

enum class AnnotatorRule(val regex: Regex) {
  STORE_VARIABLE(storeVariable),
  CREATE_VARIABLE(createVariable),
  SET_VARIABLE(setVariable),
  CALL_FUNCTION(callFunction),
  ISOLATED_CODE(isolatedCode),
}