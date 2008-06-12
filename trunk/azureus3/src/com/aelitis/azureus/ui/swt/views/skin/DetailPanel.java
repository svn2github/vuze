package com.aelitis.azureus.ui.swt.views.skin;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
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

	private BlankDetailPage blankPage = null;

	private String currentPageID = null;

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

		detailPanel.setBackground(ColorCache.getColor(detailPanel.getDisplay(), 22,
				22, 22));
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

		detailPanel.addControlListener(new ControlListener() {

			public void controlResized(ControlEvent e) {
				calculateLightBoxDimensions(lbShell);
			}

			public void controlMoved(ControlEvent e) {
			}
		});

		return null;
	}

	private void calculateLightBoxDimensions(LightBoxShell shell) {
		if (null != shell) {
			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();

			IMainWindow mainWindow = uiFunctions.getMainWindow();

			/*
			 * Calculate the offset from the bottom for the lightbox
			 * We're subtracting the status bar, the footer, and the button bar
			 */
			int offsetHeight = ((FormData) detailPanel.getLayoutData()).height;
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

		}

	}

	private void createDefaultPages() {

		blankPage = new BlankDetailPage(this, "blankPage");
		currentPageID = "blankPage";

		blankPage.createControls(detailPanel);
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

		/*
		 * Must bring lightbox back on top when ever a browser component is activated
		 */
		if (Constants.isOSX) {
			page.setActivationListener(new IDetailPage.ActivationListener() {
				public void pageActivated() {
					if (null != getLbShell()) {
						getLbShell().moveAbove(null);
					}
				}
			});
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

		Utils.execSWTThreadLater(0, new AERunnable() {

			public void runSupport() {
				int DETAIL_PANEL_HEIGHT = 463;
				SWTSkinObject detailPanelObject = skin.getSkinObject(SkinConstants.VIEWID_DETAIL_PANEL);

				Point size = detailPanelObject.getControl().getSize();

				if (detailPanelObject != null) {
					UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();

					if (true == value) {

						DETAIL_PANEL_HEIGHT = getPanelHeight(uiFunctions,
								DETAIL_PANEL_HEIGHT);
						/*
						 * LightBox to cover the elements above this detail panel
						 */
						if (null != lbShell) {
							lbShell.close();
						}

						lbShell = new LightBoxShell(uiFunctions.getMainShell(), false);
						lbShell.setStyleMask(LightBoxShell.RESIZE_HORIZONTAL
								| LightBoxShell.RESIZE_VERTICAL);
						lbShell.setAlphaLevel(200);
						//						calculateLightBoxDimensions(lbShell);
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

						stackLayout.topControl = blankPage.getControl();
						detailPanel.layout(true, true);

					}
					AERunnable runWhenDone = new AERunnable() {

						public void runSupport() {
							if (false == value) {
								if (null != lbShell) {
									lbShell.close();
									lbShell = null;
								}
							} else {
								/*
								 * If the page is found call it's .refresh() method with a RefreshListener;
								 * in the RefreshListener bring the page to the top and update the UI
								 */
								if (true == pages.containsKey(pageID)) {
									currentPageID = pageID;
									final IDetailPage page = ((IDetailPage) pages.get(pageID));
									blankPage.showBusy(true, 0);
									page.refresh(new IDetailPage.RefreshListener() {
										public void refreshCompleted() {
											Utils.execSWTThreadLater(0, new AERunnable() {
												public void runSupport() {
													blankPage.showBusy(false);
													Utils.relayout(detailPanel);
													stackLayout.topControl = page.getControl();
													detailPanel.layout();

													/*
													 * For OSX after the layout operation is done must set focus so the ui will repaint properly
													 */
													if (true == Constants.isOSX && true == value) {
														detailPanel.setFocus();
													}
												}
											});

										}
									});
								}
							}

						}
					};

					Point destSize = value ? new Point(SWT.DEFAULT, size.y) : new Point(
							0, 0);

					SWTSkinUtils.setVisibility(skin, null, detailPanelObject, destSize,
							false, true, runWhenDone);

				}
			}
		});
	}

	public void showBusy(boolean value, long delay) {
		if (true == value) {
			stackLayout.topControl = blankPage.getControl();
			detailPanel.layout();
			blankPage.showBusy(value, delay);
		} else {
			IDetailPage page = getPage(currentPageID);
			if (null != page) {
				stackLayout.topControl = page.getControl();
				detailPanel.layout();
				blankPage.showBusy(false, 0);
			}
		}
	}

	private LightBoxShell getLbShell() {
		return lbShell;
	}

	/**
	 * Calculates and return the optimum height to fit the panel in the main window without overlap
	 * @param uiFunctions
	 * @param heightHint
	 * @return
	 */
	private int getPanelHeight(UIFunctionsSWT uiFunctions, int heightHint) {

		return heightHint;
	}
}
