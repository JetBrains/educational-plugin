// Generated from ProblemSolvingLanguage.g4 by ANTLR 4.13.1
package com.jetbrains.edu.jarvis.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class ProblemSolvingLanguageParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		AND=1, VALUE=2, IF=3, ELSE=4, THEN=5, LOOP=6, EACH=7, REPEAT=8, UNTIL=9, 
		WHILE=10, DO=11, EMPTY=12, RANDOM=13, STRING_WORD=14, VARIABLE=15, MUTABLE=16, 
		VAR=17, VAL=18, SET=19, CALLED=20, FUNCTION=21, PRINT=22, STORE=23, CALL=24, 
		IN=25, CREATE=26, BOOL=27, ADD=28, EQUAL=29, RETURN=30, READ=31, WITH=32, 
		GET=33, NUMBER=34, STRING=35, CODE=36, IDENTIFIER=37, WS=38;
	public static final int
		RULE_sentence = 0, RULE_arbitraryText = 1, RULE_word = 2, RULE_value = 3, 
		RULE_contains = 4, RULE_expr = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"sentence", "arbitraryText", "word", "value", "contains", "expr"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'and'", null, "'if'", null, "'then'", null, null, null, "'until'", 
			"'while'", "'do'", "'empty'", "'random'", "'string'", null, "'mutable'", 
			null, "'val'", null, null, null, null, null, null, "'in'", null, null, 
			null, null, null, null, "'with'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "AND", "VALUE", "IF", "ELSE", "THEN", "LOOP", "EACH", "REPEAT", 
			"UNTIL", "WHILE", "DO", "EMPTY", "RANDOM", "STRING_WORD", "VARIABLE", 
			"MUTABLE", "VAR", "VAL", "SET", "CALLED", "FUNCTION", "PRINT", "STORE", 
			"CALL", "IN", "CREATE", "BOOL", "ADD", "EQUAL", "RETURN", "READ", "WITH", 
			"GET", "NUMBER", "STRING", "CODE", "IDENTIFIER", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "ProblemSolvingLanguage.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public ProblemSolvingLanguageParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SentenceContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ProblemSolvingLanguageParser.EOF, 0); }
		public List<ArbitraryTextContext> arbitraryText() {
			return getRuleContexts(ArbitraryTextContext.class);
		}
		public ArbitraryTextContext arbitraryText(int i) {
			return getRuleContext(ArbitraryTextContext.class,i);
		}
		public SentenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sentence; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).enterSentence(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).exitSentence(this);
		}
	}

	public final SentenceContext sentence() throws RecognitionException {
		SentenceContext _localctx = new SentenceContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_sentence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(13);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				setState(12);
				arbitraryText();
				}
				break;
			}
			setState(15);
			expr(0);
			setState(17);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
				{
				setState(16);
				arbitraryText();
				}
			}

			setState(19);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArbitraryTextContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(ProblemSolvingLanguageParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(ProblemSolvingLanguageParser.IDENTIFIER, i);
		}
		public List<TerminalNode> AND() { return getTokens(ProblemSolvingLanguageParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(ProblemSolvingLanguageParser.AND, i);
		}
		public List<TerminalNode> VALUE() { return getTokens(ProblemSolvingLanguageParser.VALUE); }
		public TerminalNode VALUE(int i) {
			return getToken(ProblemSolvingLanguageParser.VALUE, i);
		}
		public List<TerminalNode> ELSE() { return getTokens(ProblemSolvingLanguageParser.ELSE); }
		public TerminalNode ELSE(int i) {
			return getToken(ProblemSolvingLanguageParser.ELSE, i);
		}
		public List<TerminalNode> THEN() { return getTokens(ProblemSolvingLanguageParser.THEN); }
		public TerminalNode THEN(int i) {
			return getToken(ProblemSolvingLanguageParser.THEN, i);
		}
		public List<TerminalNode> LOOP() { return getTokens(ProblemSolvingLanguageParser.LOOP); }
		public TerminalNode LOOP(int i) {
			return getToken(ProblemSolvingLanguageParser.LOOP, i);
		}
		public List<TerminalNode> EACH() { return getTokens(ProblemSolvingLanguageParser.EACH); }
		public TerminalNode EACH(int i) {
			return getToken(ProblemSolvingLanguageParser.EACH, i);
		}
		public List<TerminalNode> WHILE() { return getTokens(ProblemSolvingLanguageParser.WHILE); }
		public TerminalNode WHILE(int i) {
			return getToken(ProblemSolvingLanguageParser.WHILE, i);
		}
		public List<TerminalNode> DO() { return getTokens(ProblemSolvingLanguageParser.DO); }
		public TerminalNode DO(int i) {
			return getToken(ProblemSolvingLanguageParser.DO, i);
		}
		public List<TerminalNode> EMPTY() { return getTokens(ProblemSolvingLanguageParser.EMPTY); }
		public TerminalNode EMPTY(int i) {
			return getToken(ProblemSolvingLanguageParser.EMPTY, i);
		}
		public List<TerminalNode> RANDOM() { return getTokens(ProblemSolvingLanguageParser.RANDOM); }
		public TerminalNode RANDOM(int i) {
			return getToken(ProblemSolvingLanguageParser.RANDOM, i);
		}
		public List<TerminalNode> STRING_WORD() { return getTokens(ProblemSolvingLanguageParser.STRING_WORD); }
		public TerminalNode STRING_WORD(int i) {
			return getToken(ProblemSolvingLanguageParser.STRING_WORD, i);
		}
		public List<TerminalNode> VARIABLE() { return getTokens(ProblemSolvingLanguageParser.VARIABLE); }
		public TerminalNode VARIABLE(int i) {
			return getToken(ProblemSolvingLanguageParser.VARIABLE, i);
		}
		public List<TerminalNode> SET() { return getTokens(ProblemSolvingLanguageParser.SET); }
		public TerminalNode SET(int i) {
			return getToken(ProblemSolvingLanguageParser.SET, i);
		}
		public List<TerminalNode> CALLED() { return getTokens(ProblemSolvingLanguageParser.CALLED); }
		public TerminalNode CALLED(int i) {
			return getToken(ProblemSolvingLanguageParser.CALLED, i);
		}
		public List<TerminalNode> FUNCTION() { return getTokens(ProblemSolvingLanguageParser.FUNCTION); }
		public TerminalNode FUNCTION(int i) {
			return getToken(ProblemSolvingLanguageParser.FUNCTION, i);
		}
		public List<TerminalNode> PRINT() { return getTokens(ProblemSolvingLanguageParser.PRINT); }
		public TerminalNode PRINT(int i) {
			return getToken(ProblemSolvingLanguageParser.PRINT, i);
		}
		public List<TerminalNode> STORE() { return getTokens(ProblemSolvingLanguageParser.STORE); }
		public TerminalNode STORE(int i) {
			return getToken(ProblemSolvingLanguageParser.STORE, i);
		}
		public List<TerminalNode> CALL() { return getTokens(ProblemSolvingLanguageParser.CALL); }
		public TerminalNode CALL(int i) {
			return getToken(ProblemSolvingLanguageParser.CALL, i);
		}
		public List<TerminalNode> IN() { return getTokens(ProblemSolvingLanguageParser.IN); }
		public TerminalNode IN(int i) {
			return getToken(ProblemSolvingLanguageParser.IN, i);
		}
		public List<TerminalNode> CREATE() { return getTokens(ProblemSolvingLanguageParser.CREATE); }
		public TerminalNode CREATE(int i) {
			return getToken(ProblemSolvingLanguageParser.CREATE, i);
		}
		public List<TerminalNode> BOOL() { return getTokens(ProblemSolvingLanguageParser.BOOL); }
		public TerminalNode BOOL(int i) {
			return getToken(ProblemSolvingLanguageParser.BOOL, i);
		}
		public List<TerminalNode> REPEAT() { return getTokens(ProblemSolvingLanguageParser.REPEAT); }
		public TerminalNode REPEAT(int i) {
			return getToken(ProblemSolvingLanguageParser.REPEAT, i);
		}
		public List<TerminalNode> UNTIL() { return getTokens(ProblemSolvingLanguageParser.UNTIL); }
		public TerminalNode UNTIL(int i) {
			return getToken(ProblemSolvingLanguageParser.UNTIL, i);
		}
		public List<TerminalNode> ADD() { return getTokens(ProblemSolvingLanguageParser.ADD); }
		public TerminalNode ADD(int i) {
			return getToken(ProblemSolvingLanguageParser.ADD, i);
		}
		public List<TerminalNode> EQUAL() { return getTokens(ProblemSolvingLanguageParser.EQUAL); }
		public TerminalNode EQUAL(int i) {
			return getToken(ProblemSolvingLanguageParser.EQUAL, i);
		}
		public List<TerminalNode> RETURN() { return getTokens(ProblemSolvingLanguageParser.RETURN); }
		public TerminalNode RETURN(int i) {
			return getToken(ProblemSolvingLanguageParser.RETURN, i);
		}
		public List<TerminalNode> READ() { return getTokens(ProblemSolvingLanguageParser.READ); }
		public TerminalNode READ(int i) {
			return getToken(ProblemSolvingLanguageParser.READ, i);
		}
		public List<TerminalNode> WITH() { return getTokens(ProblemSolvingLanguageParser.WITH); }
		public TerminalNode WITH(int i) {
			return getToken(ProblemSolvingLanguageParser.WITH, i);
		}
		public List<TerminalNode> GET() { return getTokens(ProblemSolvingLanguageParser.GET); }
		public TerminalNode GET(int i) {
			return getToken(ProblemSolvingLanguageParser.GET, i);
		}
		public ArbitraryTextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arbitraryText; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).enterArbitraryText(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).exitArbitraryText(this);
		}
	}

	public final ArbitraryTextContext arbitraryText() throws RecognitionException {
		ArbitraryTextContext _localctx = new ArbitraryTextContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_arbitraryText);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(22); 
			_errHandler.sync(this);
			_alt = 1+1;
			do {
				switch (_alt) {
				case 1+1:
					{
					{
					setState(21);
					_la = _input.LA(1);
					if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(24); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			} while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WordContext extends ParserRuleContext {
		public TerminalNode STRING_WORD() { return getToken(ProblemSolvingLanguageParser.STRING_WORD, 0); }
		public TerminalNode RANDOM() { return getToken(ProblemSolvingLanguageParser.RANDOM, 0); }
		public TerminalNode EMPTY() { return getToken(ProblemSolvingLanguageParser.EMPTY, 0); }
		public TerminalNode NUMBER() { return getToken(ProblemSolvingLanguageParser.NUMBER, 0); }
		public TerminalNode CODE() { return getToken(ProblemSolvingLanguageParser.CODE, 0); }
		public WordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_word; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).enterWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).exitWord(this);
		}
	}

	public final WordContext word() throws RecognitionException {
		WordContext _localctx = new WordContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_word);
		try {
			setState(33);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_WORD:
				enterOuterAlt(_localctx, 1);
				{
				setState(26);
				match(STRING_WORD);
				}
				break;
			case RANDOM:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(27);
				match(RANDOM);
				setState(28);
				match(STRING_WORD);
				}
				}
				break;
			case EMPTY:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(29);
				match(EMPTY);
				setState(30);
				match(STRING_WORD);
				}
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 4);
				{
				setState(31);
				match(NUMBER);
				}
				break;
			case CODE:
				enterOuterAlt(_localctx, 5);
				{
				setState(32);
				match(CODE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ValueContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(ProblemSolvingLanguageParser.NUMBER, 0); }
		public TerminalNode STRING() { return getToken(ProblemSolvingLanguageParser.STRING, 0); }
		public TerminalNode CODE() { return getToken(ProblemSolvingLanguageParser.CODE, 0); }
		public TerminalNode IDENTIFIER() { return getToken(ProblemSolvingLanguageParser.IDENTIFIER, 0); }
		public TerminalNode BOOL() { return getToken(ProblemSolvingLanguageParser.BOOL, 0); }
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).exitValue(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(35);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 257832255488L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ContainsContext extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public TerminalNode IN() { return getToken(ProblemSolvingLanguageParser.IN, 0); }
		public TerminalNode CODE() { return getToken(ProblemSolvingLanguageParser.CODE, 0); }
		public List<ArbitraryTextContext> arbitraryText() {
			return getRuleContexts(ArbitraryTextContext.class);
		}
		public ArbitraryTextContext arbitraryText(int i) {
			return getRuleContext(ArbitraryTextContext.class,i);
		}
		public ContainsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_contains; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).enterContains(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).exitContains(this);
		}
	}

	public final ContainsContext contains() throws RecognitionException {
		ContainsContext _localctx = new ContainsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_contains);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(37);
			value();
			setState(39);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				{
				setState(38);
				arbitraryText();
				}
				break;
			}
			setState(41);
			match(IN);
			setState(43);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
				{
				setState(42);
				arbitraryText();
				}
			}

			setState(45);
			match(CODE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public TerminalNode PRINT() { return getToken(ProblemSolvingLanguageParser.PRINT, 0); }
		public List<ArbitraryTextContext> arbitraryText() {
			return getRuleContexts(ArbitraryTextContext.class);
		}
		public ArbitraryTextContext arbitraryText(int i) {
			return getRuleContext(ArbitraryTextContext.class,i);
		}
		public TerminalNode STRING() { return getToken(ProblemSolvingLanguageParser.STRING, 0); }
		public List<TerminalNode> CODE() { return getTokens(ProblemSolvingLanguageParser.CODE); }
		public TerminalNode CODE(int i) {
			return getToken(ProblemSolvingLanguageParser.CODE, i);
		}
		public TerminalNode CALL() { return getToken(ProblemSolvingLanguageParser.CALL, 0); }
		public TerminalNode FUNCTION() { return getToken(ProblemSolvingLanguageParser.FUNCTION, 0); }
		public TerminalNode WITH() { return getToken(ProblemSolvingLanguageParser.WITH, 0); }
		public TerminalNode GET() { return getToken(ProblemSolvingLanguageParser.GET, 0); }
		public TerminalNode STORE() { return getToken(ProblemSolvingLanguageParser.STORE, 0); }
		public TerminalNode IN() { return getToken(ProblemSolvingLanguageParser.IN, 0); }
		public TerminalNode VARIABLE() { return getToken(ProblemSolvingLanguageParser.VARIABLE, 0); }
		public TerminalNode CREATE() { return getToken(ProblemSolvingLanguageParser.CREATE, 0); }
		public List<WordContext> word() {
			return getRuleContexts(WordContext.class);
		}
		public WordContext word(int i) {
			return getRuleContext(WordContext.class,i);
		}
		public TerminalNode CALLED() { return getToken(ProblemSolvingLanguageParser.CALLED, 0); }
		public TerminalNode EQUAL() { return getToken(ProblemSolvingLanguageParser.EQUAL, 0); }
		public TerminalNode SET() { return getToken(ProblemSolvingLanguageParser.SET, 0); }
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public TerminalNode VALUE() { return getToken(ProblemSolvingLanguageParser.VALUE, 0); }
		public TerminalNode IDENTIFIER() { return getToken(ProblemSolvingLanguageParser.IDENTIFIER, 0); }
		public TerminalNode IF() { return getToken(ProblemSolvingLanguageParser.IF, 0); }
		public TerminalNode BOOL() { return getToken(ProblemSolvingLanguageParser.BOOL, 0); }
		public ContainsContext contains() {
			return getRuleContext(ContainsContext.class,0);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(ProblemSolvingLanguageParser.ELSE, 0); }
		public TerminalNode NUMBER() { return getToken(ProblemSolvingLanguageParser.NUMBER, 0); }
		public TerminalNode THEN() { return getToken(ProblemSolvingLanguageParser.THEN, 0); }
		public TerminalNode REPEAT() { return getToken(ProblemSolvingLanguageParser.REPEAT, 0); }
		public TerminalNode UNTIL() { return getToken(ProblemSolvingLanguageParser.UNTIL, 0); }
		public TerminalNode WHILE() { return getToken(ProblemSolvingLanguageParser.WHILE, 0); }
		public TerminalNode DO() { return getToken(ProblemSolvingLanguageParser.DO, 0); }
		public TerminalNode LOOP() { return getToken(ProblemSolvingLanguageParser.LOOP, 0); }
		public TerminalNode EACH() { return getToken(ProblemSolvingLanguageParser.EACH, 0); }
		public TerminalNode ADD() { return getToken(ProblemSolvingLanguageParser.ADD, 0); }
		public TerminalNode RETURN() { return getToken(ProblemSolvingLanguageParser.RETURN, 0); }
		public TerminalNode READ() { return getToken(ProblemSolvingLanguageParser.READ, 0); }
		public TerminalNode AND() { return getToken(ProblemSolvingLanguageParser.AND, 0); }
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProblemSolvingLanguageListener ) ((ProblemSolvingLanguageListener)listener).exitExpr(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 10;
		enterRecursionRule(_localctx, 10, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(253);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,59,_ctx) ) {
			case 1:
				{
				setState(48);
				match(PRINT);
				setState(50);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
				case 1:
					{
					setState(49);
					arbitraryText();
					}
					break;
				}
				setState(53);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
				case 1:
					{
					setState(52);
					_la = _input.LA(1);
					if ( !(_la==STRING || _la==CODE) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				}
				}
				break;
			case 2:
				{
				setState(55);
				match(CALL);
				setState(57);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
					{
					setState(56);
					arbitraryText();
					}
				}

				setState(59);
				match(CODE);
				setState(61);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
				case 1:
					{
					setState(60);
					match(FUNCTION);
					}
					break;
				}
				setState(72);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
				case 1:
					{
					setState(63);
					match(WITH);
					setState(65);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
						{
						setState(64);
						arbitraryText();
						}
					}

					setState(68); 
					_errHandler.sync(this);
					_alt = 1+1;
					do {
						switch (_alt) {
						case 1+1:
							{
							{
							setState(67);
							match(CODE);
							}
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						setState(70); 
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
					} while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
					}
					break;
				}
				setState(75);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(74);
					arbitraryText();
					}
					break;
				}
				setState(82);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
				case 1:
					{
					setState(77);
					match(GET);
					setState(79);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
						{
						setState(78);
						arbitraryText();
						}
					}

					setState(81);
					match(CODE);
					}
					break;
				}
				}
				break;
			case 3:
				{
				setState(84);
				match(STORE);
				setState(86);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(85);
					arbitraryText();
					}
					break;
				}
				setState(88);
				match(IN);
				setState(90);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
				case 1:
					{
					setState(89);
					arbitraryText();
					}
					break;
				}
				setState(93);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==VARIABLE) {
					{
					setState(92);
					match(VARIABLE);
					}
				}

				setState(95);
				match(CODE);
				}
				break;
			case 4:
				{
				setState(96);
				match(CREATE);
				setState(98);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(97);
					arbitraryText();
					}
					break;
				}
				setState(101);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
				case 1:
					{
					setState(100);
					word();
					}
					break;
				}
				setState(104);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==VARIABLE) {
					{
					setState(103);
					match(VARIABLE);
					}
				}

				setState(107);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CALLED) {
					{
					setState(106);
					match(CALLED);
					}
				}

				setState(109);
				match(CODE);
				setState(115);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
				case 1:
					{
					setState(110);
					match(EQUAL);
					setState(112);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
					case 1:
						{
						setState(111);
						arbitraryText();
						}
						break;
					}
					setState(114);
					word();
					}
					break;
				}
				}
				break;
			case 5:
				{
				setState(117);
				match(SET);
				setState(119);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
				case 1:
					{
					setState(118);
					arbitraryText();
					}
					break;
				}
				setState(122);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
				case 1:
					{
					setState(121);
					match(VALUE);
					}
					break;
				}
				setState(125);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
				case 1:
					{
					setState(124);
					arbitraryText();
					}
					break;
				}
				setState(127);
				value();
				}
				break;
			case 6:
				{
				setState(128);
				match(SET);
				setState(130);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==VARIABLE) {
					{
					setState(129);
					match(VARIABLE);
					}
				}

				setState(133);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CALLED) {
					{
					setState(132);
					match(CALLED);
					}
				}

				setState(135);
				_la = _input.LA(1);
				if ( !(_la==CODE || _la==IDENTIFIER) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(137);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
				case 1:
					{
					setState(136);
					arbitraryText();
					}
					break;
				}
				setState(140);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==VALUE) {
					{
					setState(139);
					match(VALUE);
					}
				}

				setState(142);
				value();
				}
				break;
			case 7:
				{
				setState(143);
				match(IF);
				setState(145);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
				case 1:
					{
					setState(144);
					arbitraryText();
					}
					break;
				}
				setState(150);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
				case 1:
					{
					setState(147);
					match(BOOL);
					}
					break;
				case 2:
					{
					setState(148);
					match(CODE);
					}
					break;
				case 3:
					{
					setState(149);
					contains();
					}
					break;
				}
				setState(153);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
				case 1:
					{
					setState(152);
					arbitraryText();
					}
					break;
				}
				setState(157);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
				case 1:
					{
					setState(155);
					match(EQUAL);
					setState(156);
					_la = _input.LA(1);
					if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 257698037760L) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				}
				setState(169);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
				case 1:
					{
					setState(160);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
					case 1:
						{
						setState(159);
						match(THEN);
						}
						break;
					}
					setState(163);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
					case 1:
						{
						setState(162);
						arbitraryText();
						}
						break;
					}
					setState(165);
					expr(0);
					setState(167);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
					case 1:
						{
						setState(166);
						arbitraryText();
						}
						break;
					}
					}
					break;
				}
				setState(176);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
				case 1:
					{
					setState(171);
					match(ELSE);
					setState(173);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
					case 1:
						{
						setState(172);
						arbitraryText();
						}
						break;
					}
					setState(175);
					expr(0);
					}
					break;
				}
				}
				break;
			case 8:
				{
				setState(178);
				match(REPEAT);
				setState(180);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
				case 1:
					{
					setState(179);
					arbitraryText();
					}
					break;
				}
				setState(182);
				_la = _input.LA(1);
				if ( !(_la==UNTIL || _la==WHILE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(184);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,43,_ctx) ) {
				case 1:
					{
					setState(183);
					arbitraryText();
					}
					break;
				}
				}
				break;
			case 9:
				{
				setState(186);
				match(WHILE);
				setState(188);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
				case 1:
					{
					setState(187);
					arbitraryText();
					}
					break;
				}
				setState(190);
				match(DO);
				setState(192);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,45,_ctx) ) {
				case 1:
					{
					setState(191);
					arbitraryText();
					}
					break;
				}
				}
				break;
			case 10:
				{
				setState(194);
				match(LOOP);
				setState(196);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,46,_ctx) ) {
				case 1:
					{
					setState(195);
					arbitraryText();
					}
					break;
				}
				setState(198);
				match(EACH);
				setState(200);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
					{
					setState(199);
					arbitraryText();
					}
				}

				setState(202);
				match(CODE);
				setState(204);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
				case 1:
					{
					setState(203);
					arbitraryText();
					}
					break;
				}
				setState(206);
				match(IN);
				setState(208);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
					{
					setState(207);
					arbitraryText();
					}
				}

				setState(210);
				match(CODE);
				setState(215);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
				case 1:
					{
					setState(211);
					match(DO);
					setState(213);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,50,_ctx) ) {
					case 1:
						{
						setState(212);
						arbitraryText();
						}
						break;
					}
					}
					break;
				}
				}
				break;
			case 11:
				{
				setState(217);
				match(ADD);
				setState(219);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,52,_ctx) ) {
				case 1:
					{
					setState(218);
					arbitraryText();
					}
					break;
				}
				setState(221);
				value();
				setState(223);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 154618363894L) != 0)) {
					{
					setState(222);
					arbitraryText();
					}
				}

				setState(225);
				match(CODE);
				}
				break;
			case 12:
				{
				setState(227);
				match(LOOP);
				setState(228);
				match(EACH);
				setState(229);
				match(CODE);
				setState(230);
				match(IN);
				setState(231);
				match(CODE);
				}
				break;
			case 13:
				{
				setState(232);
				match(RETURN);
				setState(234);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,54,_ctx) ) {
				case 1:
					{
					setState(233);
					arbitraryText();
					}
					break;
				}
				setState(236);
				value();
				}
				break;
			case 14:
				{
				setState(237);
				match(READ);
				setState(239);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
				case 1:
					{
					setState(238);
					arbitraryText();
					}
					break;
				}
				}
				break;
			case 15:
				{
				setState(241);
				match(CODE);
				setState(243);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
				case 1:
					{
					setState(242);
					match(FUNCTION);
					}
					break;
				}
				setState(246);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
				case 1:
					{
					setState(245);
					arbitraryText();
					}
					break;
				}
				setState(248);
				match(RETURN);
				setState(250);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,58,_ctx) ) {
				case 1:
					{
					setState(249);
					arbitraryText();
					}
					break;
				}
				setState(252);
				value();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(268);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,63,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(266);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(255);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(257);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
						case 1:
							{
							setState(256);
							arbitraryText();
							}
							break;
						}
						setState(259);
						expr(2);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(260);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(261);
						match(AND);
						setState(264);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
						case 1:
							{
							setState(262);
							expr(0);
							}
							break;
						case 2:
							{
							setState(263);
							arbitraryText();
							}
							break;
						}
						}
						break;
					}
					} 
				}
				setState(270);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,63,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 5:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001&\u0110\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0001\u0000\u0003\u0000\u000e\b\u0000\u0001\u0000\u0001"+
		"\u0000\u0003\u0000\u0012\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0004"+
		"\u0001\u0017\b\u0001\u000b\u0001\f\u0001\u0018\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002"+
		"\"\b\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0003\u0004"+
		"(\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004,\b\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u00053\b\u0005\u0001"+
		"\u0005\u0003\u00056\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005:\b\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005>\b\u0005\u0001\u0005\u0001\u0005"+
		"\u0003\u0005B\b\u0005\u0001\u0005\u0004\u0005E\b\u0005\u000b\u0005\f\u0005"+
		"F\u0003\u0005I\b\u0005\u0001\u0005\u0003\u0005L\b\u0005\u0001\u0005\u0001"+
		"\u0005\u0003\u0005P\b\u0005\u0001\u0005\u0003\u0005S\b\u0005\u0001\u0005"+
		"\u0001\u0005\u0003\u0005W\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005"+
		"[\b\u0005\u0001\u0005\u0003\u0005^\b\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0003\u0005c\b\u0005\u0001\u0005\u0003\u0005f\b\u0005\u0001\u0005"+
		"\u0003\u0005i\b\u0005\u0001\u0005\u0003\u0005l\b\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0003\u0005q\b\u0005\u0001\u0005\u0003\u0005t\b\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005x\b\u0005\u0001\u0005\u0003\u0005"+
		"{\b\u0005\u0001\u0005\u0003\u0005~\b\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0003\u0005\u0083\b\u0005\u0001\u0005\u0003\u0005\u0086\b\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005\u008a\b\u0005\u0001\u0005\u0003\u0005"+
		"\u008d\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u0092\b"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u0097\b\u0005\u0001"+
		"\u0005\u0003\u0005\u009a\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u009e"+
		"\b\u0005\u0001\u0005\u0003\u0005\u00a1\b\u0005\u0001\u0005\u0003\u0005"+
		"\u00a4\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00a8\b\u0005\u0003"+
		"\u0005\u00aa\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00ae\b\u0005"+
		"\u0001\u0005\u0003\u0005\u00b1\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005"+
		"\u00b5\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00b9\b\u0005\u0001"+
		"\u0005\u0001\u0005\u0003\u0005\u00bd\b\u0005\u0001\u0005\u0001\u0005\u0003"+
		"\u0005\u00c1\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00c5\b\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005\u00c9\b\u0005\u0001\u0005\u0001\u0005"+
		"\u0003\u0005\u00cd\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00d1\b"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00d6\b\u0005\u0003"+
		"\u0005\u00d8\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00dc\b\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005\u00e0\b\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0003\u0005\u00eb\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0003\u0005\u00f0\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00f4\b"+
		"\u0005\u0001\u0005\u0003\u0005\u00f7\b\u0005\u0001\u0005\u0001\u0005\u0003"+
		"\u0005\u00fb\b\u0005\u0001\u0005\u0003\u0005\u00fe\b\u0005\u0001\u0005"+
		"\u0001\u0005\u0003\u0005\u0102\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005\u0109\b\u0005\u0005\u0005\u010b\b"+
		"\u0005\n\u0005\f\u0005\u010e\t\u0005\u0001\u0005\u0002\u0018F\u0001\n"+
		"\u0006\u0000\u0002\u0004\u0006\b\n\u0000\u0006\u0004\u0000\u0001\u0002"+
		"\u0004\u000f\u0013!%%\u0002\u0000\u001b\u001b\"%\u0001\u0000#$\u0001\u0000"+
		"$%\u0001\u0000\"%\u0001\u0000\t\n\u015a\u0000\r\u0001\u0000\u0000\u0000"+
		"\u0002\u0016\u0001\u0000\u0000\u0000\u0004!\u0001\u0000\u0000\u0000\u0006"+
		"#\u0001\u0000\u0000\u0000\b%\u0001\u0000\u0000\u0000\n\u00fd\u0001\u0000"+
		"\u0000\u0000\f\u000e\u0003\u0002\u0001\u0000\r\f\u0001\u0000\u0000\u0000"+
		"\r\u000e\u0001\u0000\u0000\u0000\u000e\u000f\u0001\u0000\u0000\u0000\u000f"+
		"\u0011\u0003\n\u0005\u0000\u0010\u0012\u0003\u0002\u0001\u0000\u0011\u0010"+
		"\u0001\u0000\u0000\u0000\u0011\u0012\u0001\u0000\u0000\u0000\u0012\u0013"+
		"\u0001\u0000\u0000\u0000\u0013\u0014\u0005\u0000\u0000\u0001\u0014\u0001"+
		"\u0001\u0000\u0000\u0000\u0015\u0017\u0007\u0000\u0000\u0000\u0016\u0015"+
		"\u0001\u0000\u0000\u0000\u0017\u0018\u0001\u0000\u0000\u0000\u0018\u0019"+
		"\u0001\u0000\u0000\u0000\u0018\u0016\u0001\u0000\u0000\u0000\u0019\u0003"+
		"\u0001\u0000\u0000\u0000\u001a\"\u0005\u000e\u0000\u0000\u001b\u001c\u0005"+
		"\r\u0000\u0000\u001c\"\u0005\u000e\u0000\u0000\u001d\u001e\u0005\f\u0000"+
		"\u0000\u001e\"\u0005\u000e\u0000\u0000\u001f\"\u0005\"\u0000\u0000 \""+
		"\u0005$\u0000\u0000!\u001a\u0001\u0000\u0000\u0000!\u001b\u0001\u0000"+
		"\u0000\u0000!\u001d\u0001\u0000\u0000\u0000!\u001f\u0001\u0000\u0000\u0000"+
		"! \u0001\u0000\u0000\u0000\"\u0005\u0001\u0000\u0000\u0000#$\u0007\u0001"+
		"\u0000\u0000$\u0007\u0001\u0000\u0000\u0000%\'\u0003\u0006\u0003\u0000"+
		"&(\u0003\u0002\u0001\u0000\'&\u0001\u0000\u0000\u0000\'(\u0001\u0000\u0000"+
		"\u0000()\u0001\u0000\u0000\u0000)+\u0005\u0019\u0000\u0000*,\u0003\u0002"+
		"\u0001\u0000+*\u0001\u0000\u0000\u0000+,\u0001\u0000\u0000\u0000,-\u0001"+
		"\u0000\u0000\u0000-.\u0005$\u0000\u0000.\t\u0001\u0000\u0000\u0000/0\u0006"+
		"\u0005\uffff\uffff\u000002\u0005\u0016\u0000\u000013\u0003\u0002\u0001"+
		"\u000021\u0001\u0000\u0000\u000023\u0001\u0000\u0000\u000035\u0001\u0000"+
		"\u0000\u000046\u0007\u0002\u0000\u000054\u0001\u0000\u0000\u000056\u0001"+
		"\u0000\u0000\u00006\u00fe\u0001\u0000\u0000\u000079\u0005\u0018\u0000"+
		"\u00008:\u0003\u0002\u0001\u000098\u0001\u0000\u0000\u00009:\u0001\u0000"+
		"\u0000\u0000:;\u0001\u0000\u0000\u0000;=\u0005$\u0000\u0000<>\u0005\u0015"+
		"\u0000\u0000=<\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000\u0000>H\u0001"+
		"\u0000\u0000\u0000?A\u0005 \u0000\u0000@B\u0003\u0002\u0001\u0000A@\u0001"+
		"\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000BD\u0001\u0000\u0000\u0000"+
		"CE\u0005$\u0000\u0000DC\u0001\u0000\u0000\u0000EF\u0001\u0000\u0000\u0000"+
		"FG\u0001\u0000\u0000\u0000FD\u0001\u0000\u0000\u0000GI\u0001\u0000\u0000"+
		"\u0000H?\u0001\u0000\u0000\u0000HI\u0001\u0000\u0000\u0000IK\u0001\u0000"+
		"\u0000\u0000JL\u0003\u0002\u0001\u0000KJ\u0001\u0000\u0000\u0000KL\u0001"+
		"\u0000\u0000\u0000LR\u0001\u0000\u0000\u0000MO\u0005!\u0000\u0000NP\u0003"+
		"\u0002\u0001\u0000ON\u0001\u0000\u0000\u0000OP\u0001\u0000\u0000\u0000"+
		"PQ\u0001\u0000\u0000\u0000QS\u0005$\u0000\u0000RM\u0001\u0000\u0000\u0000"+
		"RS\u0001\u0000\u0000\u0000S\u00fe\u0001\u0000\u0000\u0000TV\u0005\u0017"+
		"\u0000\u0000UW\u0003\u0002\u0001\u0000VU\u0001\u0000\u0000\u0000VW\u0001"+
		"\u0000\u0000\u0000WX\u0001\u0000\u0000\u0000XZ\u0005\u0019\u0000\u0000"+
		"Y[\u0003\u0002\u0001\u0000ZY\u0001\u0000\u0000\u0000Z[\u0001\u0000\u0000"+
		"\u0000[]\u0001\u0000\u0000\u0000\\^\u0005\u000f\u0000\u0000]\\\u0001\u0000"+
		"\u0000\u0000]^\u0001\u0000\u0000\u0000^_\u0001\u0000\u0000\u0000_\u00fe"+
		"\u0005$\u0000\u0000`b\u0005\u001a\u0000\u0000ac\u0003\u0002\u0001\u0000"+
		"ba\u0001\u0000\u0000\u0000bc\u0001\u0000\u0000\u0000ce\u0001\u0000\u0000"+
		"\u0000df\u0003\u0004\u0002\u0000ed\u0001\u0000\u0000\u0000ef\u0001\u0000"+
		"\u0000\u0000fh\u0001\u0000\u0000\u0000gi\u0005\u000f\u0000\u0000hg\u0001"+
		"\u0000\u0000\u0000hi\u0001\u0000\u0000\u0000ik\u0001\u0000\u0000\u0000"+
		"jl\u0005\u0014\u0000\u0000kj\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000"+
		"\u0000lm\u0001\u0000\u0000\u0000ms\u0005$\u0000\u0000np\u0005\u001d\u0000"+
		"\u0000oq\u0003\u0002\u0001\u0000po\u0001\u0000\u0000\u0000pq\u0001\u0000"+
		"\u0000\u0000qr\u0001\u0000\u0000\u0000rt\u0003\u0004\u0002\u0000sn\u0001"+
		"\u0000\u0000\u0000st\u0001\u0000\u0000\u0000t\u00fe\u0001\u0000\u0000"+
		"\u0000uw\u0005\u0013\u0000\u0000vx\u0003\u0002\u0001\u0000wv\u0001\u0000"+
		"\u0000\u0000wx\u0001\u0000\u0000\u0000xz\u0001\u0000\u0000\u0000y{\u0005"+
		"\u0002\u0000\u0000zy\u0001\u0000\u0000\u0000z{\u0001\u0000\u0000\u0000"+
		"{}\u0001\u0000\u0000\u0000|~\u0003\u0002\u0001\u0000}|\u0001\u0000\u0000"+
		"\u0000}~\u0001\u0000\u0000\u0000~\u007f\u0001\u0000\u0000\u0000\u007f"+
		"\u00fe\u0003\u0006\u0003\u0000\u0080\u0082\u0005\u0013\u0000\u0000\u0081"+
		"\u0083\u0005\u000f\u0000\u0000\u0082\u0081\u0001\u0000\u0000\u0000\u0082"+
		"\u0083\u0001\u0000\u0000\u0000\u0083\u0085\u0001\u0000\u0000\u0000\u0084"+
		"\u0086\u0005\u0014\u0000\u0000\u0085\u0084\u0001\u0000\u0000\u0000\u0085"+
		"\u0086\u0001\u0000\u0000\u0000\u0086\u0087\u0001\u0000\u0000\u0000\u0087"+
		"\u0089\u0007\u0003\u0000\u0000\u0088\u008a\u0003\u0002\u0001\u0000\u0089"+
		"\u0088\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000\u0000\u0000\u008a"+
		"\u008c\u0001\u0000\u0000\u0000\u008b\u008d\u0005\u0002\u0000\u0000\u008c"+
		"\u008b\u0001\u0000\u0000\u0000\u008c\u008d\u0001\u0000\u0000\u0000\u008d"+
		"\u008e\u0001\u0000\u0000\u0000\u008e\u00fe\u0003\u0006\u0003\u0000\u008f"+
		"\u0091\u0005\u0003\u0000\u0000\u0090\u0092\u0003\u0002\u0001\u0000\u0091"+
		"\u0090\u0001\u0000\u0000\u0000\u0091\u0092\u0001\u0000\u0000\u0000\u0092"+
		"\u0096\u0001\u0000\u0000\u0000\u0093\u0097\u0005\u001b\u0000\u0000\u0094"+
		"\u0097\u0005$\u0000\u0000\u0095\u0097\u0003\b\u0004\u0000\u0096\u0093"+
		"\u0001\u0000\u0000\u0000\u0096\u0094\u0001\u0000\u0000\u0000\u0096\u0095"+
		"\u0001\u0000\u0000\u0000\u0097\u0099\u0001\u0000\u0000\u0000\u0098\u009a"+
		"\u0003\u0002\u0001\u0000\u0099\u0098\u0001\u0000\u0000\u0000\u0099\u009a"+
		"\u0001\u0000\u0000\u0000\u009a\u009d\u0001\u0000\u0000\u0000\u009b\u009c"+
		"\u0005\u001d\u0000\u0000\u009c\u009e\u0007\u0004\u0000\u0000\u009d\u009b"+
		"\u0001\u0000\u0000\u0000\u009d\u009e\u0001\u0000\u0000\u0000\u009e\u00a9"+
		"\u0001\u0000\u0000\u0000\u009f\u00a1\u0005\u0005\u0000\u0000\u00a0\u009f"+
		"\u0001\u0000\u0000\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1\u00a3"+
		"\u0001\u0000\u0000\u0000\u00a2\u00a4\u0003\u0002\u0001\u0000\u00a3\u00a2"+
		"\u0001\u0000\u0000\u0000\u00a3\u00a4\u0001\u0000\u0000\u0000\u00a4\u00a5"+
		"\u0001\u0000\u0000\u0000\u00a5\u00a7\u0003\n\u0005\u0000\u00a6\u00a8\u0003"+
		"\u0002\u0001\u0000\u00a7\u00a6\u0001\u0000\u0000\u0000\u00a7\u00a8\u0001"+
		"\u0000\u0000\u0000\u00a8\u00aa\u0001\u0000\u0000\u0000\u00a9\u00a0\u0001"+
		"\u0000\u0000\u0000\u00a9\u00aa\u0001\u0000\u0000\u0000\u00aa\u00b0\u0001"+
		"\u0000\u0000\u0000\u00ab\u00ad\u0005\u0004\u0000\u0000\u00ac\u00ae\u0003"+
		"\u0002\u0001\u0000\u00ad\u00ac\u0001\u0000\u0000\u0000\u00ad\u00ae\u0001"+
		"\u0000\u0000\u0000\u00ae\u00af\u0001\u0000\u0000\u0000\u00af\u00b1\u0003"+
		"\n\u0005\u0000\u00b0\u00ab\u0001\u0000\u0000\u0000\u00b0\u00b1\u0001\u0000"+
		"\u0000\u0000\u00b1\u00fe\u0001\u0000\u0000\u0000\u00b2\u00b4\u0005\b\u0000"+
		"\u0000\u00b3\u00b5\u0003\u0002\u0001\u0000\u00b4\u00b3\u0001\u0000\u0000"+
		"\u0000\u00b4\u00b5\u0001\u0000\u0000\u0000\u00b5\u00b6\u0001\u0000\u0000"+
		"\u0000\u00b6\u00b8\u0007\u0005\u0000\u0000\u00b7\u00b9\u0003\u0002\u0001"+
		"\u0000\u00b8\u00b7\u0001\u0000\u0000\u0000\u00b8\u00b9\u0001\u0000\u0000"+
		"\u0000\u00b9\u00fe\u0001\u0000\u0000\u0000\u00ba\u00bc\u0005\n\u0000\u0000"+
		"\u00bb\u00bd\u0003\u0002\u0001\u0000\u00bc\u00bb\u0001\u0000\u0000\u0000"+
		"\u00bc\u00bd\u0001\u0000\u0000\u0000\u00bd\u00be\u0001\u0000\u0000\u0000"+
		"\u00be\u00c0\u0005\u000b\u0000\u0000\u00bf\u00c1\u0003\u0002\u0001\u0000"+
		"\u00c0\u00bf\u0001\u0000\u0000\u0000\u00c0\u00c1\u0001\u0000\u0000\u0000"+
		"\u00c1\u00fe\u0001\u0000\u0000\u0000\u00c2\u00c4\u0005\u0006\u0000\u0000"+
		"\u00c3\u00c5\u0003\u0002\u0001\u0000\u00c4\u00c3\u0001\u0000\u0000\u0000"+
		"\u00c4\u00c5\u0001\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000\u0000\u0000"+
		"\u00c6\u00c8\u0005\u0007\u0000\u0000\u00c7\u00c9\u0003\u0002\u0001\u0000"+
		"\u00c8\u00c7\u0001\u0000\u0000\u0000\u00c8\u00c9\u0001\u0000\u0000\u0000"+
		"\u00c9\u00ca\u0001\u0000\u0000\u0000\u00ca\u00cc\u0005$\u0000\u0000\u00cb"+
		"\u00cd\u0003\u0002\u0001\u0000\u00cc\u00cb\u0001\u0000\u0000\u0000\u00cc"+
		"\u00cd\u0001\u0000\u0000\u0000\u00cd\u00ce\u0001\u0000\u0000\u0000\u00ce"+
		"\u00d0\u0005\u0019\u0000\u0000\u00cf\u00d1\u0003\u0002\u0001\u0000\u00d0"+
		"\u00cf\u0001\u0000\u0000\u0000\u00d0\u00d1\u0001\u0000\u0000\u0000\u00d1"+
		"\u00d2\u0001\u0000\u0000\u0000\u00d2\u00d7\u0005$\u0000\u0000\u00d3\u00d5"+
		"\u0005\u000b\u0000\u0000\u00d4\u00d6\u0003\u0002\u0001\u0000\u00d5\u00d4"+
		"\u0001\u0000\u0000\u0000\u00d5\u00d6\u0001\u0000\u0000\u0000\u00d6\u00d8"+
		"\u0001\u0000\u0000\u0000\u00d7\u00d3\u0001\u0000\u0000\u0000\u00d7\u00d8"+
		"\u0001\u0000\u0000\u0000\u00d8\u00fe\u0001\u0000\u0000\u0000\u00d9\u00db"+
		"\u0005\u001c\u0000\u0000\u00da\u00dc\u0003\u0002\u0001\u0000\u00db\u00da"+
		"\u0001\u0000\u0000\u0000\u00db\u00dc\u0001\u0000\u0000\u0000\u00dc\u00dd"+
		"\u0001\u0000\u0000\u0000\u00dd\u00df\u0003\u0006\u0003\u0000\u00de\u00e0"+
		"\u0003\u0002\u0001\u0000\u00df\u00de\u0001\u0000\u0000\u0000\u00df\u00e0"+
		"\u0001\u0000\u0000\u0000\u00e0\u00e1\u0001\u0000\u0000\u0000\u00e1\u00e2"+
		"\u0005$\u0000\u0000\u00e2\u00fe\u0001\u0000\u0000\u0000\u00e3\u00e4\u0005"+
		"\u0006\u0000\u0000\u00e4\u00e5\u0005\u0007\u0000\u0000\u00e5\u00e6\u0005"+
		"$\u0000\u0000\u00e6\u00e7\u0005\u0019\u0000\u0000\u00e7\u00fe\u0005$\u0000"+
		"\u0000\u00e8\u00ea\u0005\u001e\u0000\u0000\u00e9\u00eb\u0003\u0002\u0001"+
		"\u0000\u00ea\u00e9\u0001\u0000\u0000\u0000\u00ea\u00eb\u0001\u0000\u0000"+
		"\u0000\u00eb\u00ec\u0001\u0000\u0000\u0000\u00ec\u00fe\u0003\u0006\u0003"+
		"\u0000\u00ed\u00ef\u0005\u001f\u0000\u0000\u00ee\u00f0\u0003\u0002\u0001"+
		"\u0000\u00ef\u00ee\u0001\u0000\u0000\u0000\u00ef\u00f0\u0001\u0000\u0000"+
		"\u0000\u00f0\u00fe\u0001\u0000\u0000\u0000\u00f1\u00f3\u0005$\u0000\u0000"+
		"\u00f2\u00f4\u0005\u0015\u0000\u0000\u00f3\u00f2\u0001\u0000\u0000\u0000"+
		"\u00f3\u00f4\u0001\u0000\u0000\u0000\u00f4\u00f6\u0001\u0000\u0000\u0000"+
		"\u00f5\u00f7\u0003\u0002\u0001\u0000\u00f6\u00f5\u0001\u0000\u0000\u0000"+
		"\u00f6\u00f7\u0001\u0000\u0000\u0000\u00f7\u00f8\u0001\u0000\u0000\u0000"+
		"\u00f8\u00fa\u0005\u001e\u0000\u0000\u00f9\u00fb\u0003\u0002\u0001\u0000"+
		"\u00fa\u00f9\u0001\u0000\u0000\u0000\u00fa\u00fb\u0001\u0000\u0000\u0000"+
		"\u00fb\u00fc\u0001\u0000\u0000\u0000\u00fc\u00fe\u0003\u0006\u0003\u0000"+
		"\u00fd/\u0001\u0000\u0000\u0000\u00fd7\u0001\u0000\u0000\u0000\u00fdT"+
		"\u0001\u0000\u0000\u0000\u00fd`\u0001\u0000\u0000\u0000\u00fdu\u0001\u0000"+
		"\u0000\u0000\u00fd\u0080\u0001\u0000\u0000\u0000\u00fd\u008f\u0001\u0000"+
		"\u0000\u0000\u00fd\u00b2\u0001\u0000\u0000\u0000\u00fd\u00ba\u0001\u0000"+
		"\u0000\u0000\u00fd\u00c2\u0001\u0000\u0000\u0000\u00fd\u00d9\u0001\u0000"+
		"\u0000\u0000\u00fd\u00e3\u0001\u0000\u0000\u0000\u00fd\u00e8\u0001\u0000"+
		"\u0000\u0000\u00fd\u00ed\u0001\u0000\u0000\u0000\u00fd\u00f1\u0001\u0000"+
		"\u0000\u0000\u00fe\u010c\u0001\u0000\u0000\u0000\u00ff\u0101\n\u0001\u0000"+
		"\u0000\u0100\u0102\u0003\u0002\u0001\u0000\u0101\u0100\u0001\u0000\u0000"+
		"\u0000\u0101\u0102\u0001\u0000\u0000\u0000\u0102\u0103\u0001\u0000\u0000"+
		"\u0000\u0103\u010b\u0003\n\u0005\u0002\u0104\u0105\n\u0002\u0000\u0000"+
		"\u0105\u0108\u0005\u0001\u0000\u0000\u0106\u0109\u0003\n\u0005\u0000\u0107"+
		"\u0109\u0003\u0002\u0001\u0000\u0108\u0106\u0001\u0000\u0000\u0000\u0108"+
		"\u0107\u0001\u0000\u0000\u0000\u0109\u010b\u0001\u0000\u0000\u0000\u010a"+
		"\u00ff\u0001\u0000\u0000\u0000\u010a\u0104\u0001\u0000\u0000\u0000\u010b"+
		"\u010e\u0001\u0000\u0000\u0000\u010c\u010a\u0001\u0000\u0000\u0000\u010c"+
		"\u010d\u0001\u0000\u0000\u0000\u010d\u000b\u0001\u0000\u0000\u0000\u010e"+
		"\u010c\u0001\u0000\u0000\u0000@\r\u0011\u0018!\'+259=AFHKORVZ]behkpsw"+
		"z}\u0082\u0085\u0089\u008c\u0091\u0096\u0099\u009d\u00a0\u00a3\u00a7\u00a9"+
		"\u00ad\u00b0\u00b4\u00b8\u00bc\u00c0\u00c4\u00c8\u00cc\u00d0\u00d5\u00d7"+
		"\u00db\u00df\u00ea\u00ef\u00f3\u00f6\u00fa\u00fd\u0101\u0108\u010a\u010c";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}