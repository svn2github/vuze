/**
 * Created on Jun 27, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.*;
import org.gudy.azureus2.ui.swt.views.stats.VivaldiView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;

/**
 * @author TuxPaper
 * @created Jun 27, 2008
 *
 */
public class TopBarView
	extends SkinView
{
	private List<UISWTViewCore> topbarViews = new ArrayList<UISWTViewCore>();

	private UISWTViewCore activeTopBar;

	private SWTSkin skin;

	private org.eclipse.swt.widgets.List listPlugins;

	private Composite cPluginArea;

	private static boolean registeredCoreSubViews = false;

	
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		this.skin = skinObject.getSkin();
		
		skin.addListener("topbar-plugins", new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == SWTSkinObjectListener.EVENT_SHOW) {
					skin.removeListener("topbar-plugins", this);
					// building needs UISWTInstance, which needs core.
					AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
						public void azureusCoreRunning(AzureusCore core) {
							Utils.execSWTThreadLater(0, new AERunnable() {
								public void runSupport() {
									buildTopBarViews();
								}
							});
						}
					});
				}
				return null;
			}
		});
		// trigger autobuild
		skin.getSkinObject("topbar-area-plugin");
		return null;
	}

	/**
	 * @param skinObject
	 *
	 * @since 3.0.1.1
	 */
	public void buildTopBarViews() {
		// TODO actually use plugins..
		SWTSkinObject skinObject = skin.getSkinObject("topbar-plugins");
		if (skinObject == null) {
			return;
		}

		try {

			cPluginArea = (Composite) skinObject.getControl();

			final UIUpdatable updatable = new UIUpdatable() {
				public void updateUI() {
					Object[] views = topbarViews.toArray();
					for (int i = 0; i < views.length; i++) {
						try {
							UISWTViewCore view = (UISWTViewCore) views[i];
							if (view.getComposite().isVisible()) {
								view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
							}
						} catch (Exception e) {
							Debug.out(e);
						}
					}
				}

				public String getUpdateUIName() {
					return "TopBar";
				}
			};
			try {
				UIFunctionsManager.getUIFunctions().getUIUpdater().addUpdater(updatable);
			} catch (Exception e) {
				Debug.out(e);
			}

			skinObject.getControl().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					try {
						UIFunctionsManager.getUIFunctions().getUIUpdater().removeUpdater(
								updatable);
					} catch (Exception ex) {
						Debug.out(ex);
					}
					Object[] views = topbarViews.toArray();
					topbarViews.clear();
					for (int i = 0; i < views.length; i++) {
						UISWTViewCore view = (UISWTViewCore) views[i];
						if (view != null) {
							view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
						}
					}
				}
			});

			SWTSkinObject soPrev = skin.getSkinObject("topbar-plugin-prev");
			if (soPrev != null) {
				SWTSkinButtonUtility btnPrev = new SWTSkinButtonUtility(soPrev);
				btnPrev.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						//System.out.println("prev click " + activeTopBar + " ; "
						//		+ topbarViews.size());
						if (activeTopBar == null || topbarViews.size() <= 1) {
							return;
						}
						int i = topbarViews.indexOf(activeTopBar) - 1;
						if (i < 0) {
							i = topbarViews.size() - 1;
						}
						activateTopBar((UISWTViewCore) topbarViews.get(i));
					}
				});
			}

			SWTSkinObject soNext = skin.getSkinObject("topbar-plugin-next");
			if (soNext != null) {
				SWTSkinButtonUtility btnNext = new SWTSkinButtonUtility(soNext);
				btnNext.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						//System.out.println("next click");
						if (activeTopBar == null || topbarViews.size() <= 1) {
							return;
						}
						int i = topbarViews.indexOf(activeTopBar) + 1;
						if (i >= topbarViews.size()) {
							i = 0;
						}
						activateTopBar((UISWTViewCore) topbarViews.get(i));
					}
				});
			}

			SWTSkinObject soTitle = skin.getSkinObject("topbar-plugin-title");
			if (soTitle != null) {
				final Composite cTitle = (Composite) soTitle.getControl();
				cTitle.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent e) {
						e.gc.setAdvanced(true);
						//Font font = new Font(e.gc.getDevice(), "Sans", 8, SWT.NORMAL);
						//e.gc.setFont(font);
						if (e.gc.getAdvanced() && activeTopBar != null) {
							try {
								e.gc.setTextAntialias(SWT.ON);
							} catch (Exception ex) {
								// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
							}

							try {
								Transform transform = new Transform(e.gc.getDevice());
								transform.rotate(270);
								e.gc.setTransform(transform);

								String s = activeTopBar.getFullTitle();
								Point size = e.gc.textExtent(s);
								e.gc.drawText(s, -size.x, 0, true);
								//e.gc.drawText(s, 0,0, true);
								transform.dispose();
							} catch (Exception ex) {
								// setTransform can trhow a ERROR_NO_GRAPHICS_LIBRARY error
								// no use trying to draw.. it would look weird
							}
							//font.dispose();
						}
					}
				});
			}

			SWTSkinObject soList = skin.getSkinObject("topbar-plugin-list");
			if (soList != null) {
				final Composite cList = (Composite) soList.getControl();
				listPlugins = new org.eclipse.swt.widgets.List(cList, SWT.None);
				listPlugins.setLayoutData(Utils.getFilledFormData());
				listPlugins.setBackground(cList.getBackground());
				listPlugins.setForeground(cList.getForeground());
				listPlugins.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						int i = listPlugins.getSelectionIndex();
						if (i >= 0 && i < topbarViews.size()) {
							activateTopBar((UISWTViewCore) topbarViews.get(i));
							COConfigurationManager.setParameter("topbar.viewindex", i);
						}
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
			}

			skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				Listener l = new Listener() {
					private int mouseDownAt = 0;

					public void handleEvent(Event event) {
						Composite c = (Composite) event.widget;
						if (event.type == SWT.MouseDown) {
							Rectangle clientArea = c.getClientArea();
							if (event.y > clientArea.height - 10) {
								mouseDownAt = event.y;
							}
						} else if (event.type == SWT.MouseUp && mouseDownAt > 0) {
							int diff = event.y - mouseDownAt;
							mouseDownAt = 0;
							FormData formData = (FormData) c.getLayoutData();
							formData.height += diff;
							if (formData.height < 50) {
								formData.height = 50;
							} else {
								Rectangle clientArea = c.getShell().getClientArea();
								int max = clientArea.height - 350;
								if (formData.height > max) {
									formData.height = max;
								}
							}
							COConfigurationManager.setParameter("v3.topbar.height",
									formData.height);
							Utils.relayout(c);
						} else if (event.type == SWT.MouseMove) {
							Rectangle clientArea = c.getClientArea();
							boolean draggable = (event.y > clientArea.height - 10);
							c.setCursor(draggable ? c.getDisplay().getSystemCursor(
									SWT.CURSOR_SIZENS) : null);
						} else if (event.type == SWT.MouseExit) {
							c.setCursor(null);
						}
					}
				};
				Control control = skinObject.getControl();
				control.addListener(SWT.MouseDown, l);
				control.addListener(SWT.MouseUp, l);
				control.addListener(SWT.MouseMove, l);
				control.addListener(SWT.MouseExit, l);

				skinObject.addListener(new SWTSkinObjectListener() {
					public Object eventOccured(SWTSkinObject skinObject, int eventType,
							Object params) {
						if (eventType == EVENT_SHOW) {
							int h = COConfigurationManager.getIntParameter("v3.topbar.height");
							Control control = skinObject.getControl();
							FormData formData = (FormData) control.getLayoutData();
							formData.height = h;
							control.setLayoutData(formData);
							Utils.relayout(control);
						}
						return null;
					}
				});
			}

			int toActiveView = COConfigurationManager.getIntParameter(
					"topbar.viewindex", 0);
			int viewIndex = toActiveView;

			UISWTInstanceImpl uiSWTinstance = (UISWTInstanceImpl) UIFunctionsManagerSWT.getUIFunctionsSWT().getUISWTInstance();
			
			if (uiSWTinstance != null && !registeredCoreSubViews ) {
				uiSWTinstance.addView(UISWTInstance.VIEW_TOPBAR, "ViewDownSpeedGraph",
						new ViewDownSpeedGraph());
				uiSWTinstance.addView(UISWTInstance.VIEW_TOPBAR, "ViewUpSpeedGraph",
						new ViewUpSpeedGraph());
				uiSWTinstance.addView(UISWTInstance.VIEW_TOPBAR, "VivaldiView",
						new VivaldiView(false));

				registeredCoreSubViews = true;
			}

			UISWTViewEventListenerHolder[] pluginViews = uiSWTinstance.getViewListeners(UISWTInstance.VIEW_TOPBAR);
			for (UISWTViewEventListenerHolder l : pluginViews) {
				if (l != null) {
					try {
						UISWTViewImpl view = new UISWTViewImpl(UISWTInstance.VIEW_TOPBAR,
								l.getViewID(), l, null);
						addTopBarView(view, cPluginArea);
						if (toActiveView-- == 0) {
							activateTopBar(view);
							if (listPlugins != null) {
								listPlugins.setSelection(viewIndex);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						// skip, plugin probably specifically asked to not be added
					}
				}
			}

			if (toActiveView >= 0 && topbarViews.size() > 0) {
				activeTopBar = topbarViews.get(0);
				activeTopBar.getComposite().setVisible(true);
			}

			skinObject.getControl().getParent().layout(true);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	/**
	 * @param view
	 *
	 * @since 3.0.1.1
	 */
	protected void activateTopBar(UISWTViewCore view) {
		if (activeTopBar != null) {
			Composite c = activeTopBar.getComposite();
			while (c.getParent() != cPluginArea) {
				c = c.getParent();
			}
			c.setVisible(false);
		}
		activeTopBar = view;
		Composite c = activeTopBar.getComposite();
		while (c.getParent() != cPluginArea) {
			c = c.getParent();
		}
		c.setVisible(true);

		SWTSkinObject soTitle = skin.getSkinObject("topbar-plugin-title");
		//System.out.println("Hello" + soTitle);
		if (soTitle != null) {
			soTitle.getControl().redraw();
		}
	}

	/**
	 * @param view
	 *
	 * @since 3.0.1.1
	 */
	private void addTopBarView(UISWTViewCore view, Composite composite) {
		Composite parent = new Composite(composite, SWT.None);
		parent.setLayoutData(Utils.getFilledFormData());
		parent.setLayout(new FormLayout());

		view.initialize(parent);
		parent.setVisible(false);

		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			Object ld = control.getLayoutData();
			boolean useGridLayout = ld != null && (ld instanceof GridData);
			if (useGridLayout) {
				GridLayout gridLayout = new GridLayout();
				gridLayout.horizontalSpacing = 0;
				gridLayout.marginHeight = 0;
				gridLayout.marginWidth = 0;
				gridLayout.verticalSpacing = 0;
				parent.setLayout(gridLayout);
				break;
			} else if (ld == null) {
				control.setLayoutData(Utils.getFilledFormData());
			}
		}

		topbarViews.add(view);
		if (listPlugins != null) {
			String s = view.getFullTitle();
			if (MessageText.keyExists(s)) {
				s = MessageText.getString(s);
			}
			listPlugins.add(s);
		}
	}

}
