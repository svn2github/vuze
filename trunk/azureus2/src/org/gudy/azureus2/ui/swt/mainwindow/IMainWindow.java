package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.graphics.Rectangle;

public interface IMainWindow
{

	public static final int WINDOW_ELEMENT_MENU = 1;

	public static final int WINDOW_ELEMENT_TOOLBAR = 2;

	public static final int WINDOW_ELEMENT_STATUSBAR = 3;

	public static final int WINDOW_ELEMENT_TOPBAR = 4;

	// 3.2 TODO: rename to searchbar or profilebar or something
	public static final int WINDOW_ELEMENT_TABBAR = 5;
	
	public static final int WINDOW_CLIENT_AREA = 6;
	
	public static final int WINDOW_CONTENT_DISPLAY_AREA = 7;

	public boolean isVisible(int windowElement);

	public void setVisible(int windowElement, boolean value);

	public Rectangle getMetrics(int windowElement);

}
