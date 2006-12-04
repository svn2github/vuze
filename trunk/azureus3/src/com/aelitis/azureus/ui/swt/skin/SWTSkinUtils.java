/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

/**
 * @author TuxPaper
 * @created Jun 8, 2006
 *
 */
public class SWTSkinUtils
{

	public static final int TILE_NONE = 0;

	public static final int TILE_Y = 1;

	public static final int TILE_X = 2;

	public static final int TILE_CENTER_X = 4;

	public static final int TILE_CENTER_Y = 8;

	public static final int TILE_BOTH = TILE_X | TILE_Y;

	private static Listener imageDownListener;

	private static Listener imageOverListener;

	static {
		imageOverListener = new SWTSkinImageChanger("-over", SWT.MouseEnter,
				SWT.MouseExit);
		imageDownListener = new SWTSkinImageChanger("-down", SWT.MouseDown,
				SWT.MouseUp);
	}

	public static int getAlignment(String sAlign, int def) {
		int align;

		if (sAlign.equalsIgnoreCase("center")) {
			align = SWT.CENTER;
		} else if (sAlign.equalsIgnoreCase("bottom")) {
			align = SWT.BOTTOM;
		} else if (sAlign.equalsIgnoreCase("top")) {
			align = SWT.TOP;
		} else if (sAlign.equalsIgnoreCase("left")) {
			align = SWT.LEFT;
		} else if (sAlign.equalsIgnoreCase("right")) {
			align = SWT.RIGHT;
		} else {
			align = def;
		}

		return align;
	}

	/**
	 * @param tileMode
	 * @return
	 */
	public static int getTileMode(String sTileMode) {
		int tileMode = TILE_NONE;
		if (sTileMode == null || sTileMode == "") {
			return tileMode;
		}

		sTileMode = sTileMode.toLowerCase();

		if (sTileMode.equals("tile")) {
			tileMode = TILE_X | TILE_Y;
		} else if (sTileMode.equals("tile-x")) {
			tileMode = TILE_X;
		} else if (sTileMode.equals("tile-y")) {
			tileMode = TILE_Y;
		} else if (sTileMode.equals("center-x")) {
			tileMode = TILE_CENTER_X;
		} else if (sTileMode.equals("center-y")) {
			tileMode = TILE_CENTER_Y;
		}

		return tileMode;
	}

	static void addMouseImageChangeListeners(Widget widget) {
		if (widget.getData("hasMICL") != null) {
			return;
		}

		widget.addListener(SWT.MouseEnter, imageOverListener);
		widget.addListener(SWT.MouseExit, imageOverListener);
		//		new MouseEnterExitListener(widget);

		widget.addListener(SWT.MouseDown, imageDownListener);
		widget.addListener(SWT.MouseUp, imageDownListener);

		widget.setData("hasMICL", "1");
	}

	public static class MouseEnterExitListener implements Listener
	{

		boolean bOver = false;

		public MouseEnterExitListener(Widget widget) {

			widget.addListener(SWT.MouseMove, this);
			widget.addListener(SWT.MouseExit, this);
		}

		public void handleEvent(Event event) {
			Control control = (Control) event.widget;

			SWTSkinObject skinObject = (SWTSkinObject) control.getData("SkinObject");

			if (event.type == SWT.MouseMove) {
				if (bOver) {
					return;
				}
				System.out.println(System.currentTimeMillis() + ": " + skinObject
						+ "-- OVER");
				bOver = true;
				skinObject.switchSuffix("-over", 2, true);

			} else {
				bOver = false;
				skinObject.switchSuffix("", 2, true);
			}

		}

	}
}
