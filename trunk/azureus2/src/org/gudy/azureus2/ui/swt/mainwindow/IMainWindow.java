package org.gudy.azureus2.ui.swt.mainwindow;

public interface IMainWindow
{

	public static final int WINDOW_ELEMENT_MENU = 1;

	public static final int WINDOW_ELEMENT_TOOLBAR = 2;

	public static final int WINDOW_ELEMENT_STATUSBAR = 3;

	public static final int WINDOW_ELEMENT_SEARCHBAR = 4;
	
	public static final int WINDOW_ELEMENT_TABBAR = 5;

	public boolean isVisible(int windowElement);

	public void setVisible(int windowElement, boolean value);

}
