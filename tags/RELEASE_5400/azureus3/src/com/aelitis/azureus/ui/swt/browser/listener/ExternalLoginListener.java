package com.aelitis.azureus.ui.swt.browser.listener;

public interface ExternalLoginListener {

	public void cookiesFound(ExternalLoginWindow window,String cookies);
	
	public void canceled(ExternalLoginWindow window);
	
	public void done(ExternalLoginWindow window,String cookies);
	
}
