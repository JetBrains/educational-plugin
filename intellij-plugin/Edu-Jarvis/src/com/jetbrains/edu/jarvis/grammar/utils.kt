package com.jetbrains.edu.jarvis.grammar

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker

fun parse(input: String) {
  val sentences = input.replace(System.lineSeparator(), "").lowercase().split(".")
  for (sentence in sentences) {
    val lexer = ProblemSolvingLanguageLexer(CharStreams.fromString(sentence))
    val tokens: TokenStream = CommonTokenStream(lexer)
    val parser = ProblemSolvingLanguageParser(tokens)
    val result = parser.sentence()
    val walker = ParseTreeWalker()
    val listener = ProblemSolvingLanguageBaseListener()
    walker.walk(listener, result)
  }
}
