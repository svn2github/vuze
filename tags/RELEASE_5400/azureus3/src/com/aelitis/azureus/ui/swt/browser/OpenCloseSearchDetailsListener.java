package com.aelitis.azureus.ui.swt.browser;

import java.util.Map;

public interface OpenCloseSearchDetailsListener {

	public void openSearchResults(final Map params);

	public void closeSearchResults(final Map params);
	
	public void resizeMainBrowser();
	
	public void resizeSecondaryBrowser();

}
