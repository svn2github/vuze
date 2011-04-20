package com.aelitis.azureus.ui.common;

import java.util.Map;

import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;

public interface ToolBarEnabler2 extends UIToolBarActivationListener, ToolBarEnablerBase
{
	public final static long STATE_ENABLED = 0x1;
	public final static long STATE_DROPDOWN = 0x2;

	/**
	 * Fill in list with the toolbar ids and states you wish to set
	 * @param list
	 */
	public void refreshToolBarItems(Map<String, Long> list);
}
