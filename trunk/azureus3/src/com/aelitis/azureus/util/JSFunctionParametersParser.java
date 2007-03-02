package com.aelitis.azureus.util;

import java.util.ArrayList;
import java.util.List;

public class JSFunctionParametersParser
{

	private ParserState currentState;

	private String params;

	private List parameters;

	private StringBuffer currentParameter;

	private abstract class ParserState
	{
		protected ParserState previousState;

		public ParserState(ParserState previousState) {
			this.previousState = previousState;
		}

		public abstract void processCharacter(char c);
	}

	private class ParserStateInList
		extends ParserState
	{

		public ParserStateInList(ParserState previousState) {
			super(previousState);
		}

		public void processCharacter(char c) {
			switch (c) {
				case '|':
					parameters.add(currentParameter.toString());
					currentParameter = new StringBuffer();
					break;

				case '\\':
					currentState = new ParserStateInEscapedCharacter(this);
					break;

				default:
					currentParameter.append(c);
					break;
			}
		}
	}

	private class ParserStateInEscapedCharacter
		extends ParserState
	{

		public ParserStateInEscapedCharacter(ParserState previousState) {
			super(previousState);
		}

		public void processCharacter(char c) {
			//In all cases we have to return to the previous state
			currentState = previousState;

			switch (c) {
				case '\\':
					currentParameter.append(c);
					break;

				case '|':
					currentParameter.append(c);
					break;

				default:
					//Invalid character escaped...
					break;
			}
		}
	}

	private JSFunctionParametersParser(String params) {
		this.params = params;
	}

	private void parse() {
		parameters = new ArrayList();
		currentState = new ParserStateInList(null);
		currentParameter = new StringBuffer();

		for (int i = 0; i < params.length(); i++) {
			char c = params.charAt(i);
			currentState.processCharacter(c);
		}
		parameters.add(currentParameter.toString());
	}

	public static List parse(String params) {
		JSFunctionParametersParser parser = new JSFunctionParametersParser(params);
		parser.parse();
		return parser.parameters;
	}

}
