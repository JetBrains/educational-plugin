// Generated from ProblemSolvingLanguage.g4 by ANTLR 4.13.1
package com.jetbrains.edu.jarvis.grammar;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ProblemSolvingLanguageParser}.
 */
public interface ProblemSolvingLanguageListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ProblemSolvingLanguageParser#sentence}.
	 * @param ctx the parse tree
	 */
	void enterSentence(ProblemSolvingLanguageParser.SentenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProblemSolvingLanguageParser#sentence}.
	 * @param ctx the parse tree
	 */
	void exitSentence(ProblemSolvingLanguageParser.SentenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProblemSolvingLanguageParser#arbitraryText}.
	 * @param ctx the parse tree
	 */
	void enterArbitraryText(ProblemSolvingLanguageParser.ArbitraryTextContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProblemSolvingLanguageParser#arbitraryText}.
	 * @param ctx the parse tree
	 */
	void exitArbitraryText(ProblemSolvingLanguageParser.ArbitraryTextContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProblemSolvingLanguageParser#word}.
	 * @param ctx the parse tree
	 */
	void enterWord(ProblemSolvingLanguageParser.WordContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProblemSolvingLanguageParser#word}.
	 * @param ctx the parse tree
	 */
	void exitWord(ProblemSolvingLanguageParser.WordContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProblemSolvingLanguageParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(ProblemSolvingLanguageParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProblemSolvingLanguageParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(ProblemSolvingLanguageParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProblemSolvingLanguageParser#contains}.
	 * @param ctx the parse tree
	 */
	void enterContains(ProblemSolvingLanguageParser.ContainsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProblemSolvingLanguageParser#contains}.
	 * @param ctx the parse tree
	 */
	void exitContains(ProblemSolvingLanguageParser.ContainsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProblemSolvingLanguageParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ProblemSolvingLanguageParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProblemSolvingLanguageParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ProblemSolvingLanguageParser.ExprContext ctx);
}