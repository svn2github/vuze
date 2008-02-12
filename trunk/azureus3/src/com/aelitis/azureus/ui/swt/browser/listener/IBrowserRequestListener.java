package com.aelitis.azureus.ui.swt.browser.listener;

/**
 * An interface for listeners to a browser operation request
 * @author knguyen
 *
 */
public interface IBrowserRequestListener
{

	public static final String OP_CLOSE = "close";
	
	public static final String OP_CLOSE_PARAM_STATUS = "status";
	
	public static final String OP_CLOSE_PARAM_DISPLAY = "display";

	public static final String OP_REFRESH = "refresh";

	public static final String OP_OPEN_URL = "open-url";

	public static final String OP_OPEN_URL_PARAM_URL = "url";

	public static final String OP_OPEN_URL_PARAM_WIDTH = "width";

	public static final String OP_OPEN_URL_PARAM_HEIGHT = "height";

	public static final String OP_OPEN_URL_PARAM_MOVABLE = "movable";
	
	public static final String OP_OPEN_URL_PARAM_RESIZABLE = "resizable";

	public static final String OP_OPEN_URL_PARAM_TITLE_PREFIX_VERIFIER = "title-prefix-verifier";

	public static final String OP_RESIZE = "resize";

	public static final String OP_RESIZE_PARAM_WIDTH = "width";

	public static final String OP_RESIZE_PARAM_HEIGHT = "height";

	public void handleOpenURL();

	public void handleResize();

	public void handleClose();

	public void handleRefresh();
	
	public boolean isMovable();
	
	public boolean isResizable();
	
	public int getWidth();
	
	public int getHeight();
	
	public String getPrefixVerifier();
	
	public String getURL();

}