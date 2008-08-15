package com.aelitis.azureus.core.metasearch.impl;

public interface ExternalLoginListener {

	public void cookiesFound(String cookies);
	
	public void canceled();
	
	public void done(String cookies);
	
}
