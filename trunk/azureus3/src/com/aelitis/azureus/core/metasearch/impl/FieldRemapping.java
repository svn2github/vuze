package com.aelitis.azureus.core.metasearch.impl;

import java.util.regex.Pattern;

public class FieldRemapping {
	
	private Pattern match;
	private String replace;
	
	public FieldRemapping(String match, String replace) {
		super();
		this.match = Pattern.compile(match);
		this.replace =replace;
	}
	
	public Pattern getMatch() {
		return match;
	}
	
	public String getReplacement() {
		return replace;
	}
	
	
	
}
