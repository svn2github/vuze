package com.aelitis.azureus.core.metasearch.impl;

import java.util.regex.Pattern;

public class FieldRemapping {
	
	private String  match_str;
	
	private Pattern match;
	private String replace;
	
	public FieldRemapping(String match, String replace) {
		
		this.match_str = match;
		this.match = Pattern.compile(match);
		this.replace =replace;
	}
	
	public String
	getMatchString()
	{
		return(match_str );
	}
	
	public Pattern getMatch() {
		return match;
	}
	
	public String getReplacement() {
		return replace;
	}
	
	
	
}
