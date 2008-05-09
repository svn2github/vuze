package com.aelitis.azureus.ui.swt.browser.listener;

public interface IStatusMessageListener
{

	public static final String OP_LOGIN_STATUS = "login-status";
	
	public static final String OP_LOGIN_UPDATE = "login-update";

	public static final String OP_LOGIN_UPDATE_PARAM_USER_NAME = "user-name";

	public static final String OP_LOGIN_UPDATE_PARAM_DISPLAY_NAME = "display-name";

	public static final String OP_PK = "pk";
	
	public void handleLoginStatus();
	
	public void handleLoginUpdate();

	public String getUserName();

	public String getDisplayName();
	
	public String getPK();

}