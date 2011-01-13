package com.aelitis.azureus.ui.common;

import java.util.Map;

import org.gudy.azureus2.ui.swt.IconBarEnabler;

public interface ToolBarEnabler {
	public void refreshToolBar(Map<String, Boolean> list);

  public boolean toolBarItemActivated(String itemKey);
}
