package com.aelitis.azureus.ui.swt.views.skin;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.util.Constants;

public class DetailPanel
	extends SkinView
{
	private SWTSkin skin = null;

	private LightBoxShell lbShell = null;

	private Composite detailPanel;

	private Map pages = new HashMap();

	private StackLayout stackLayout;

	public DetailPanel() {

	}

	public Object showSupport(SWTSkinObject skinObject, Object params) {

		skin = skinObject.getSkin();

		SWTSkinObject detailPanelSkin = skin.getSkinObject("detail-panel");
		if (null == detailPanelSkin) {
			return null;
		}

		detailPanel = (Composite) detailPanelSkin.getControl();

		if (null == detailPanel) {
			return null;
		}

		/*
		 * Move the panel above all others since we want it to overlap all other widgets
		 */
		detailPanel.moveAbove(null);

		stackLayout = new StackLayout();
		stackLayout.marginHeight = 6;
		stackLayout.marginWidth = 6;
		detailPanel.setLayout(stackLayout);

		createDefaultPages();

		/*
		 * Paints in the border
		 */
		detailPanel.addPaintListener(new PaintListener() {
			Color borderColor = ColorCache.getColor(detailPanel.getDisplay(), 38, 38,
					38);

			public void paintControl(PaintEvent e) {
				Rectangle bounds = detailPanel.getClientArea();
				e.gc.setForeground(borderColor);
				int r = 6;
				e.gc.setLineWidth(r);
				e.gc.drawRectangle(bounds.x + 3, bounds.y + 3, bounds.width - r,
						bounds.height - r);
			}
		});

		/*
		 * TODO: Add a listener to the parent shell and recalculate the appropriate height for the detail panel
		 */

		return null;
	}

	private void createDefaultPages() {
		/*
		 * Create the Share flow page
		 */

		SharePage sharePage = new SharePage(this);

		/*
		 * Add to manager to make it accessible from the menu and other places
		 */
		VuzeShareUtils.getInstance().setSharePage(sharePage);

		addPage(sharePage);

		/*
		 * Create the Invite flow page
		 */

		addPage(new InvitePage(this));
	}

	/**
	 * Creates a <code>Composite</code> to host custom content using the give <code>pageID</code> as the key
	 * @param pageID
	 * @param style <code>SWT</code> style bit mask appropriate for a <code>Composite</code>
	 * @return
	 */
	public void addPage(IDetailPage page) {
		if (null == detailPanel) {
			throw new NullPointerException(
					"An error has occured... the detail panel has not been properly initialized");
		}

		if (true == pages.containsKey(page.getPageID())) {
			throw new IllegalArgumentException(page.getPageID()
					+ " is already in use by an existing page");
		}
		page.createControls(detailPanel);

		pages.put(page.getPageID(), page);

		/*
		 * By default the last page created is on top
		 */
		stackLayout.topControl = page.getControl();
		detailPanel.layout();
	}

	public void removePage(String pageID) {
		if (true == pages.containsKey(pageID)) {
			pages.remove(pageID);
		}
	}

	public IDetailPage getPage(String pageID) {
		if (true == pages.containsKey(pageID)) {
			return (IDetailPage) pages.get(pageID);
		}
		return null;
	}

	/**
	 * Show/hide the detail panel
	 * @param value if <code>true</code> show the panel; otherwise hide the panel
	 */
	public void show(boolean value) {
		show(value, null);
	}

	/**
	 * Show/hide the detail panel
	 * @param value if <code>true</code> show the panel; otherwise hide the panel
	 * @param pageID if <code>value</code> is <code>true</code> then optionally loaded this page if specified
	 */
	public void show(final boolean value, final String pageID) {

		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				int DETAIL_PANEL_HEIGHT = 463;
				SWTSkinObject detailPanelObject = skin.getSkinObject(SkinConstants.VIEWID_DETAIL_PANEL);
				Control control = detailPanelObject.getControl();

				Point size = detailPanelObject.getControl().getSize();

				if (detailPanelObject != null) {
					UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();

					IMainWindow mainWindow = uiFunctions.getMainWindow();

					if (true == value) {

						/*
						 * LightBox to cover the elements above this detail panel
						 */
						if (null != lbShell) {
							lbShell.close();
						}

						lbShell = new LightBoxShell(uiFunctions.getMainShell(), false);
						/*
						 * Calculate the offset from the bottom for the lightbox
						 * We're subtracting the status bar
						 */
						int offsetHeight = DETAIL_PANEL_HEIGHT;
						offsetHeight += mainWindow.getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR).height;
						SWTSkinObject footerObject = skin.getSkinObject(SkinConstants.VIEWID_FOOTER);
						if (null != footerObject) {
							offsetHeight += footerObject.getControl().getSize().y;
						}

						SWTSkinObject buttonBarObject = skin.getSkinObject(SkinConstants.VIEWID_BUTTON_BAR);
						if (null != buttonBarObject) {
							offsetHeight += buttonBarObject.getControl().getSize().y;
						}

						lbShell.setInsets(0, offsetHeight, 0, 0);
						lbShell.setStyleMask(LightBoxShell.RESIZE_HORIZONTAL
								| LightBoxShell.RESIZE_VERTICAL);
						lbShell.setAlphaLevel(200);
						lbShell.open();

						/*
						 * Hack into the SWTSkinUtils.setVisibility() behavior by overriding the height
						 * of the detail panel dynamically; this parameter is not normally recalculated so if
						 * the application window is resized then the previous value may not match with the new window size.
						 * 
						 * For other SkinView the existing behavior is OK because of the auto-layout built in but the detail panel
						 * is different in that it needs to grow enough in height to 'push' other views up and
						 * out of visibility.
						 */

						size.y = DETAIL_PANEL_HEIGHT;

					} else {
						if (null != lbShell) {
							lbShell.close();
							lbShell = null;
						}
					}
					/*
					 * Move the specified page on top if found
					 */
					if (true == pages.containsKey(pageID)) {
						IDetailPage page = ((IDetailPage) pages.get(pageID));
						page.refresh();
						stackLayout.topControl = page.getControl();
						detailPanel.layout();
					}

					FormData fd = (FormData) control.getLayoutData();
					SWTSkinUtils.slide(control, fd, value
							? new Point(SWT.DEFAULT, size.y) : new Point(0, 0));

					/*
					 * For OSX after the layout operation is done must set focus so the ui will repaint properly
					 */
					if (true == Constants.isOSX && true == value) {
						detailPanel.setFocus();
					}
				}
			}
		});
	}
}
