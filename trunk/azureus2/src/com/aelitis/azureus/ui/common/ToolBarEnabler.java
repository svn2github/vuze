package com.aelitis.azureus.ui.common;

import java.util.Map;

public interface ToolBarEnabler {
	public void refreshToolBar(Map<String, Boolean> list);

  public boolean toolBarItemActivated(String itemKey);
}
