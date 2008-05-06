package com.aelitis.azureus.core.metasearch;

public class SearchParameter {
	
	private String matchPattern;
	private String value;
	
	
	public SearchParameter(String matchPattern, String value) {
		this.matchPattern = matchPattern;
		this.value = value;
	}


	public String getMatchPattern() {
		return matchPattern;
	}


	public String getValue() {
		return value;
	}

}
