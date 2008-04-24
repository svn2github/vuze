package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class DetailPanel
	extends SkinView
{
	private SWTSkin skin = null;

	private LightBoxShell lbShell = null;

	public DetailPanel() {

	}

	public Object showSupport(SWTSkinObject skinObject, Object params) {

		skin = skinObject.getSkin();

		SWTSkinObject detailPanelSkin = skin.getSkinObject("detail-panel");
		if (null == detailPanelSkin) {
			return null;
		}

		final Composite detailPanel = (Composite) detailPanelSkin.getControl();

		if (null != detailPanel) {

			detailPanel.addPaintListener(new PaintListener() {
				Color borderColor = ColorCache.getColor(detailPanel.getDisplay(), 38,
						38, 38);

				public void paintControl(PaintEvent e) {
					Rectangle bounds = detailPanel.getClientArea();
					e.gc.setForeground(borderColor);
					int r = 6;
					e.gc.setLineWidth(r);
					e.gc.drawRoundRectangle(bounds.x + 3, bounds.y + 3, bounds.width - r,
							bounds.height - r, 15, 15);
				}
			});

			/*
			 * TODO: Add a listener to the parent shell and recalculate the appropriate height for the detail panel
			 */

		}
		return null;
	}

	public void show(boolean value) {

		SWTSkinObject detailPanelObject = skin.getSkinObject(SkinConstants.VIEWID_DETAIL_PANEL);
		Control control = detailPanelObject.getControl();

		/*
		 * Hack into SWTSkinUtils.setVisibility() behavior by inspecting whether this shell
		 * is in the middle of sliding; if that's the case then ignore this button press
		 * and do nothing.  The main problem this is overcoming is when the user click (or double-click)
		 * the button multiple times; we want a single click to initiate and finish the sliding before 
		 * handling the next click
		 */
		if (control.getData("Sliding") != null) {
			return;
		}

		Point size = detailPanelObject.getControl().getSize();
		if (detailPanelObject != null) {

			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();

			IMainWindow mainWindow = uiFunctions.getMainWindow();

			if (true == value) {

				/*
				 * LightBox to cover the elements above this detail panel
				 */
				lbShell = new LightBoxShell(uiFunctions.getMainShell(), false);
				int insetHeight = mainWindow.getMetrics(IMainWindow.WINDOW_CONTENT_DISPLAY_AREA).height;
				insetHeight += mainWindow.getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR).height;
				lbShell.setInsets(0, insetHeight, 0, 0);
				lbShell.setStyleMask(LightBoxShell.RESIZE_HORIZONTAL);
				lbShell.open();

				/*
				 * Calculate height of detail panel
				 */
				size.y = mainWindow.getMetrics(IMainWindow.WINDOW_CONTENT_DISPLAY_AREA).height;

				SWTSkinObject footerObject = skin.getSkinObject(SkinConstants.VIEWID_FOOTER);
				if (null != footerObject) {
					size.y -= footerObject.getControl().getSize().y;
				}

				SWTSkinObject buttonBarObject = skin.getSkinObject(SkinConstants.VIEWID_BUTTON_BAR);
				if (null != buttonBarObject) {
					size.y -= buttonBarObject.getControl().getSize().y;
				}

				size.y -= 19; // minus the margin height

				/*
				 * Hack into the SWTSkinUtils.setVisibility() behavior by overriding the height
				 * of the detail panel dynamically; this parameter is not normally recalculated so if
				 * the application window is resized then the previous value may not match with the new window size.
				 * 
				 * For other SkinView the existing behavior is OK because of the auto-layout built in but the detail panel
				 * is different in that it needs to grow enough in height to 'push' other views up and
				 * out of visibility.
				 */
				control.setData("v3.oldHeight", size);

			} else {
				if (null != lbShell) {
					lbShell.close();
					lbShell = null;
				}
			}

			SWTSkinUtils.setVisibility(skin, null, SkinConstants.VIEWID_DETAIL_PANEL,
					value, false, false);
		}

	}
}
