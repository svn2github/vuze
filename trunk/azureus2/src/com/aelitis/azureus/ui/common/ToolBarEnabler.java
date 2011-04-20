package com.aelitis.azureus.ui.common;

import java.util.Map;

// Change with caution: Some internal plugins use this directly
public interface ToolBarEnabler
	extends ToolBarEnablerBase
{
	public void refreshToolBar(Map<String, Boolean> list);

	public boolean toolBarItemActivated(String itemKey);
}
