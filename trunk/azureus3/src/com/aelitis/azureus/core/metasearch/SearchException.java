package com.aelitis.azureus.core.metasearch;

public class SearchException extends Exception {
	
	public SearchException(Throwable t) {
		super(t);
	}
	
	public SearchException(String description,Throwable t) {
		super(description,t);
	}
	
	public SearchException(String description) {
		super(description);
	}

}
