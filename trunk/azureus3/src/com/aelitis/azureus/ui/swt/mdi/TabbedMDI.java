/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.mdi;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.MenuBuildUtils.MenuBuilder;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventCancelledException;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.IViewAlwaysInitialize;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTabFolder;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;

public class TabbedMDI
	extends BaseMDI
	implements TabbedMdiInterface, AEDiagnosticsEvidenceGenerator,
	ParameterListener, ObfusticateImage
{
	private CTabFolder tabFolder;

	private LinkedList<MdiEntry>	select_history = new LinkedList<MdiEntry>();

	protected boolean minimized;
	
	private int iFolderHeightAdj;

	private String props_prefix;

	private DownloadManager		maximizeTo;

	private int minimumCharacters = 25;

	protected boolean isMainMDI;

	private Map mapUserClosedTabs;

	private boolean maximizeVisible = false;

	private boolean minimizeVisible = false;

	private TabbedMdiMaximizeListener maximizeListener;

	public TabbedMDI() {
		super();
		AEDiagnostics.addEvidenceGenerator(this);
		mapUserClosedTabs = new HashMap();
		isMainMDI = true;
	}

	/**
	 * @param parent
	 */
	public TabbedMDI(Composite parent, String id) {
		this.props_prefix = id;
		minimumCharacters = 0;
		isMainMDI = false;
		setCloseableConfigFile(null);

		SWTSkin skin = SWTSkinFactory.getInstance();
		SWTSkinObjectTabFolder soFolder = new SWTSkinObjectTabFolder(skin,
				skin.getSkinProperties(), id, "tabfolder.fill", parent);
		setMainSkinObject(soFolder);
		soFolder.addListener(this);
		skin.addSkinObject(soFolder);
		
		String key = props_prefix + ".closedtabs";
		
		mapUserClosedTabs = COConfigurationManager.getMapParameter(key, new HashMap());
		COConfigurationManager.addParameterListener(key, this);
	}
	

	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		super.skinObjectCreated(skinObject, params);

		creatMDI();

		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMDI#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	@Override
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		String key = props_prefix + ".closedtabs";
		COConfigurationManager.removeParameterListener( key, this );
		
		return super.skinObjectDestroyed(skinObject, params);
	}
	
	private void creatMDI() {
		if (soMain instanceof SWTSkinObjectTabFolder) {
			tabFolder = ((SWTSkinObjectTabFolder) soMain).getTabFolder();
		} else {
			tabFolder = new CTabFolder((Composite) soMain.getControl(), SWT.TOP
					| SWT.BORDER | SWT.CLOSE);
		}

		iFolderHeightAdj = tabFolder.computeSize(SWT.DEFAULT, 0).y;

		if (isMainMDI) {
  		COConfigurationManager.addAndFireParameterListener("GUI_SWT_bFancyTab",
  				new ParameterListener() {
  					public void parameterChanged(String parameterName) {
  						Utils.execSWTThread(new AERunnable() {
  							public void runSupport() {
  								boolean simple = !COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab");
  								tabFolder.setSimple(simple);
  							}
  						});
  					}
  				});
  		tabFolder.setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
		} else {
			tabFolder.setSimple(true);
			tabFolder.setMaximizeVisible(maximizeVisible);
			tabFolder.setMinimizeVisible(minimizeVisible);
			tabFolder.setUnselectedCloseVisible(false);
		}

		Display display = tabFolder.getDisplay();

		float[] hsb = tabFolder.getBackground().getRGB().getHSB();
		hsb[2] *= (Constants.isOSX) ? 0.9 : 0.97;
		tabFolder.setBackground(ColorCache.getColor(display, hsb));

		hsb = tabFolder.getForeground().getRGB().getHSB();
		hsb[2] *= (Constants.isOSX) ? 1.1 : 0.03;
		tabFolder.setForeground(ColorCache.getColor(display, hsb));

		tabFolder.setSelectionBackground(new Color[] {
			display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
			display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
			display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
		}, new int[] {
			10,
			90
		}, true);
		tabFolder.setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		if (minimumCharacters > 0) {
			tabFolder.setMinimumCharacters(minimumCharacters);
		}

		// XXX TVSWT_Common had SWT.Activate too
		tabFolder.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TabbedEntry entry = (TabbedEntry) event.item.getData("TabbedEntry");				
				showEntry(entry);
			}
		});

		tabFolder.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (tabFolder.getMinimized()) {
					restore();
					// If the user clicked down on the restore button, and we restore
					// before the CTabFolder does, CTabFolder will minimize us again
					// There's no way that I know of to determine if the mouse is 
					// on that button!

					// one of these will tell tabFolder to cancel
					e.button = 0;
					tabFolder.notifyListeners(SWT.MouseExit, null);
				}
			}
			public void mouseDoubleClick(MouseEvent e) {
				if (!tabFolder.getMinimized() && tabFolder.getMaximizeVisible()) {
					minimize();
				}
			}
		});

		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void minimize(CTabFolderEvent event) {
				TabbedMDI.this.minimize();
			}


			public void maximize(CTabFolderEvent event) {
				if (maximizeListener != null) {
					maximizeListener.maximizePressed();
				}
			}


			public void restore(CTabFolderEvent event_maybe_null) {
				TabbedMDI.this.restore();
			}


			@Override
			public void close(CTabFolderEvent event) {
				final TabbedEntry entry = (TabbedEntry) event.item.getData(
						"TabbedEntry");

				if (select_history.remove(entry)) {

					if (select_history.size() > 0) {

						final MdiEntry next = select_history.getLast();

						if (!next.isDisposed() && next != entry) {
							
							// If tabfolder's selected entry is the one we are closing,
							// CTabFolder will try to move to next CTabItem.  Disable
							// this feature by moving tabfolder's selection away from us
							CTabItem[] items = tabFolder.getItems();
							for (int i = 0; i < items.length; i++) {
								CTabItem item = items[i];
								TabbedEntry scanEntry = getEntryFromTabItem(item);
								if (scanEntry == next) {
									tabFolder.setSelection(item);
									break;
								}
							}

							showEntry(next);
						}
					}
				}

				// since showEntry is slightly delayed, we must slightly delay 
				// the closing of the entry the user clicked.  Otherwise, it would close
				// first, and the first tab would auto-select (on windows), and then
				// the "next" tab would select.
				if (props_prefix != null) {
  				Utils.execSWTThreadLater(0, new AERunnable() {
  
  					@Override
  					public void runSupport() {
  						String view_id = entry.getViewID();
  						String key = props_prefix + ".closedtabs";
  
  						Map closedtabs = COConfigurationManager.getMapParameter(key,
  								new HashMap());
  
  						if (!closedtabs.containsKey(view_id)) {
  
  							closedtabs.put(view_id, entry.getTitle());
  
  							// this will trigger listener which will remove the tab
  							COConfigurationManager.setParameter(key, closedtabs);
  						}
  					}
  				});
				}
			}
		});
		
		if (isMainMDI) {
  		tabFolder.getDisplay().addFilter(SWT.KeyDown, new Listener() {
  			public void handleEvent(Event event) {
  				if ( tabFolder.isDisposed()){
  					return;
  				}
  				// Another window has control, skip filter
  				Control focus_control = tabFolder.getDisplay().getFocusControl();
  				if (focus_control != null
  						&& focus_control.getShell() != tabFolder.getShell()) {
  					return;
  				}
  
  				int key = event.character;
  				if ((event.stateMask & SWT.MOD1) != 0 && event.character <= 26
  						&& event.character > 0)
  					key += 'a' - 1;
  
  				// ESC or CTRL+F4 closes current Tab
  				if (key == SWT.ESC
  						|| (event.keyCode == SWT.F4 && event.stateMask == SWT.CTRL)) {
  					MdiEntry entry = getCurrentEntry();
  					if (entry != null) {
  						entry.close(false);
  					}
  					event.doit = false;
  				} else if (event.keyCode == SWT.F6
  						|| (event.character == SWT.TAB && (event.stateMask & SWT.CTRL) != 0)) {
  					// F6 or Ctrl-Tab selects next Tab
  					// On Windows the tab key will not reach this filter, as it is
  					// processed by the traversal TRAVERSE_TAB_NEXT.  It's unknown
  					// what other OSes do, so the code is here in case we get TAB
  					if ((event.stateMask & SWT.SHIFT) == 0) {
  						event.doit = false;
  						selectNextTab(true);
  						// Shift+F6 or Ctrl+Shift+Tab selects previous Tab
  					} else if (event.stateMask == SWT.SHIFT) {
  						selectNextTab(false);
  						event.doit = false;
  					}
  				}
  			}
  		});
		}

		tabFolder.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				saveCloseables();
			}
		});
		
		tabFolder.getTabHeight();
		final Menu menu = new Menu( tabFolder );
		tabFolder.setMenu(menu);
		MenuBuildUtils.addMaintenanceListenerForMenu(menu, new MenuBuilder() {
			public void buildMenu(Menu root_menu, MenuEvent event) {
				Point cursorLocation = event.display.getCursorLocation();
				Point ptOnControl = tabFolder.toControl(cursorLocation.x,
						cursorLocation.y);
				if (ptOnControl.y > tabFolder.getTabHeight()) {
					return;
				}

				final CTabItem item = tabFolder.getItem(
						tabFolder.toControl(cursorLocation.x, cursorLocation.y));

				boolean need_sep = false;

				if (item == null) {

					need_sep = mapUserClosedTabs.size() > 0;
					if (need_sep) {
  					for (Object id : mapUserClosedTabs.keySet()) {
  						final String view_id = (String) id;

  						MenuItem mi = new MenuItem(menu, SWT.PUSH);

  						String title;
  						
  						Object oTitle = mapUserClosedTabs.get(id);
  						if (oTitle instanceof String && ((String) oTitle).length() > 0) {
								title = (String) oTitle;
							} else {
								title = MessageText.getString(getViewTitleID(view_id));
							}
  						mi.setText(title);

  						mi.addListener(SWT.Selection, new Listener() {
  							public void handleEvent(Event event) {
  								String key = props_prefix + ".closedtabs";

  								Map closedtabs = COConfigurationManager.getMapParameter(key,
  										new HashMap());

  								if (closedtabs.containsKey(view_id)) {

  									closedtabs.remove(view_id);

  									COConfigurationManager.setParameter(key, closedtabs);
  								}
  								
  								showEntryByID(view_id);
  							}
  						});
  						
  					}
					}
				}

				if (need_sep) {
					new MenuItem(menu, SWT.SEPARATOR);
				}

				
				TabbedEntry entry = null;
				if (item != null) {
					entry = getEntryFromTabItem(item);
					
					
					showEntry(entry);
				}

				fillMenu(menu, entry, isMainMDI ? "sidebar" : props_prefix);

			}
		});

		CTabFolderRenderer renderer = new CTabFolderRenderer(tabFolder) {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.custom.CTabFolderRenderer#computeSize(int, int, org.eclipse.swt.graphics.GC, int, int)
			 */
			@Override
			protected Point computeSize(int part, int state, GC gc, int wHint,
					int hHint) {
				gc.setAntialias(SWT.ON);
				Point pt = super.computeSize(part, state, gc, wHint, hHint);
				if (tabFolder.isDisposed()) {
					return pt;
				}

				if (part >= 0) {
					TabbedEntry entry = getEntryFromTabItem(tabFolder.getItem(part));
					if (entry != null) {
						ViewTitleInfo viewTitleInfo = entry.getViewTitleInfo();
						if (viewTitleInfo != null) {
							Object titleRight = viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
							if (titleRight != null) {
								Point size = gc.textExtent(titleRight.toString(), 0);
								pt.x += size.x + 10 + 2;
							}
						}
						
						
						MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
						ImageLoader imageLoader = ImageLoader.getInstance();
						for (MdiEntryVitalityImage mdiEntryVitalityImage : vitalityImages) {
							if (mdiEntryVitalityImage != null && mdiEntryVitalityImage.isVisible()) {
								String imageID = mdiEntryVitalityImage.getImageID();
								Image image = imageLoader.getImage(imageID);
								if (ImageLoader.isRealImage(image)) {
									pt.x += image.getBounds().x + 1;
								}
							}
							
						}
					}
				}
				return pt;
			}
			
			/* (non-Javadoc)
			 * @see org.eclipse.swt.custom.CTabFolderRenderer#draw(int, int, org.eclipse.swt.graphics.Rectangle, org.eclipse.swt.graphics.GC)
			 */
			@Override
			protected void draw(int part, int state, Rectangle bounds, GC gc) {
				try {
					//super.draw(part, state & ~(SWT.FOREGROUND), bounds, gc);
					super.draw(part, state, bounds, gc);
				} catch (Throwable t) {
					Debug.out(t);
				}
				if (part < 0) {
					return;
				}
				try {
					CTabItem item = getTabFolder().getItem(part);
					TabbedEntry entry = getEntryFromTabItem(item);
					if (entry == null) {
						return;
					}
					
					ViewTitleInfo viewTitleInfo = entry.getViewTitleInfo();
					if (viewTitleInfo != null) {
						Object titleRight = viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
						if (titleRight != null) {
							String textIndicator = titleRight.toString();
							int x1IndicatorOfs = 0;
							int SIDEBAR_SPACING = 0;
							int x2 = bounds.x + bounds.width;
							
							if (item.getShowClose()) {
								try {
									Field fldCloseRect = item.getClass().getDeclaredField("closeRect");
									fldCloseRect.setAccessible(true);
									Rectangle closeBounds = (Rectangle) fldCloseRect.get(item);
									if (closeBounds != null && closeBounds.x > 0) {
										x2 = closeBounds.x;
									}
								} catch (Exception e) {
									x2 -= 20;
								}
							}
							gc.setAntialias(SWT.ON);

							Point textSize = gc.textExtent(textIndicator);
							//Point minTextSize = gc.textExtent("99");
							//if (textSize.x < minTextSize.x + 2) {
							//	textSize.x = minTextSize.x + 2;
							//}

							int width = textSize.x + 10;
							x1IndicatorOfs += width + SIDEBAR_SPACING;
							int startX = x2 - x1IndicatorOfs;

							int textOffsetY = 0;

							int height = textSize.y + 1;
							int startY = bounds.y + ((bounds.height - height) / 2) + 1;

							//gc.setBackground(((state & SWT.SELECTED) != 0 ) ? item.getParent().getSelectionBackground() : item.getParent().getBackground());
							//gc.fillRectangle(startX - 5, startY, width + 5, height);

							//Pattern pattern;
							//Color color1;
							//Color color2;

							//gc.fillRectangle(startX, startY, width, height);


							Color default_color = ColorCache.getSchemedColor(gc.getDevice(), "#5b6e87");
							
							Object color =  viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_COLOR);

							if ( color instanceof int[] ){
								
								gc.setBackground(ColorCache.getColor( gc.getDevice(),(int[])color ));
								
							}else{
								
								gc.setBackground( default_color );
							}
							

							Color text_color = Colors.white;

							gc.fillRoundRectangle(startX, startY, width, height, textSize.y * 2 / 3,
									height * 2 / 3);
							
							if ( color != null ){
								
								Color bg = gc.getBackground();
								
								int	red 	= bg.getRed();
								int green 	= bg.getGreen();
								int blue	= bg.getBlue();
								
								double brightness = Math.sqrt( red*red*0.299+green*green*0.587+blue*blue*0.114);
								
								if ( brightness >= 130 ){
									text_color = Colors.black;
								}
								
								gc.setBackground( default_color );
								
								gc.drawRoundRectangle(startX, startY, width, height, textSize.y * 2 / 3,
										height * 2 / 3);
							}
							gc.setForeground(text_color);
							GCStringPrinter.printString(gc, textIndicator, new Rectangle(startX,
									startY + textOffsetY, width, height), true, false, SWT.CENTER);
						
						}
					}

				} catch (Throwable t) {
					Debug.out(t);
				}
			}
		};
		tabFolder.setRenderer(renderer);
		
		if (minimizeVisible) {
			boolean toMinimize = ConfigurationManager.getInstance().getBooleanParameter(props_prefix + ".subViews.minimized");
			setMinimized(toMinimize);
		}
	}
	
	private String
	getViewTitleID(
		String	view_id )
	{
		String history_key = "swt.ui.table.tab.view.namecache." + view_id;

		String id = COConfigurationManager.getStringParameter( history_key, "" );

		if ( id.length() == 0 ){
			
			String test = view_id + ".title.full";
			
			if ( MessageText.keyExists( test )){
			
				return( test );
			}
			
			id = "!" + view_id + "!";
		}
		
		return( id );
	}



	private void minimize() {
		minimized = true;

		tabFolder.setMinimized(true);
		CTabItem[] items = tabFolder.getItems();
		String tt = MessageText.getString("label.click.to.restore");
		for (int i = 0; i < items.length; i++) {
			CTabItem tabItem = items[i];
			tabItem.setToolTipText(tt);
			Control control = tabItem.getControl();
			if (control != null && !control.isDisposed()) {
				tabItem.getControl().setVisible(false);
			}
		}

		tabFolder.getParent().notifyListeners(SWT.Resize, null);

		showEntry(null);

		ConfigurationManager configMan = ConfigurationManager.getInstance();
		configMan.setParameter(props_prefix + ".subViews.minimized", true);
	}
	
	private void restore() {

		minimized = false;
		tabFolder.setMinimized(false);
		CTabItem selection = tabFolder.getSelection();
		if (selection != null) {
			TabbedEntry tabbedEntry = getEntryFromTabItem(selection);

			showEntry(tabbedEntry);

			/* Already done by TabbedEntry.swt_build 
			Control control = selection.getControl();
			if (control == null || control.isDisposed()) {
				selectedView.initialize(tabFolder);
				selection.setControl(selectedView.getComposite());
				control = selection.getControl();
				triggerTabViewDataSourceChanged(selectedView, tv, new Object[][] {
					null,
					null
				});
			}
			selection.getControl().setVisible(true);
			*/
			tabbedEntry.updateUI();
		}

		if (tabFolder.getMaximizeVisible()) {
			CTabItem[] items = tabFolder.getItems();
			String tt = MessageText.getString("label.dblclick.to.min");

			for (int i = 0; i < items.length; i++) {
				CTabItem tabItem = items[i];
				tabItem.setToolTipText(tt);
			}
		}

		tabFolder.getParent().notifyListeners(SWT.Resize, null);

		ConfigurationManager configMan = ConfigurationManager.getInstance();
		configMan.setParameter(props_prefix + ".subViews.minimized", false);
	}



	private void selectNextTab(boolean selectNext) {
		if (tabFolder == null || tabFolder.isDisposed()) {
			return;
		}

		final int nextOrPrevious = selectNext ? 1 : -1;
		int index = tabFolder.getSelectionIndex() + nextOrPrevious;
		if (index == 0 && selectNext || index == -2 || tabFolder.getItemCount() < 2) {
			return;
		}
		if (index == tabFolder.getItemCount()) {
			index = 0;
		} else if (index < 0) {
			index = tabFolder.getItemCount() - 1;
		}

		// instead of .setSelection, use showEntry which will ensure view de/activations
		CTabItem item = tabFolder.getItem(index);
		MdiEntry entry = getEntryFromTabItem(item);

		if (entry != null) {
			showEntry(entry);
		}
	}
	
	protected boolean wasEntryLoadedOnce(String id) {
		@SuppressWarnings("deprecation")
		boolean loadedOnce = COConfigurationManager.getBooleanParameter("tab.once."
				+ id, false);
		return loadedOnce;
	}
	
	protected void setEntryLoadedOnce(String id) {
		COConfigurationManager.setParameter("tab.once." + id, true);
	}

	public void showEntry(final MdiEntry newEntry) {
		if (newEntry == null) {
			return;
		}
		
		if (newEntry != null) {
  		select_history.remove( newEntry );
  		
  		select_history.add( newEntry );
  			
  		if ( select_history.size() > 64 ){
  			
  			select_history.removeFirst();
  		}
		}
		
		MdiEntry oldEntry = currentEntry;
		if (newEntry == oldEntry && oldEntry != null) {
			((BaseMdiEntry) newEntry).show();
			triggerSelectionListener(newEntry, newEntry);
			return;
		}

		if (oldEntry != null) {
			oldEntry.hide();
		}

		currentEntry = (MdiEntrySWT) newEntry; // assumed MdiEntrySWT

		if (currentEntry instanceof BaseMdiEntry) {
			((BaseMdiEntry) newEntry).show();
		}

		triggerSelectionListener(newEntry, oldEntry);
	}

	private MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, int index) {
		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		TabbedEntry entry = new TabbedEntry(this, skin, id, null);
		entry.setTitle(title);
		entry.setSkinRef(configID, params);
		entry.setViewTitleInfo(titleInfo);
		
		setupNewEntry(entry, id, index, closeable);
		return entry;
	}
	
	// @see com.aelitis.azureus.ui.swt.mdi.BaseMDI#createEntryFromSkinRef(java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo, java.lang.Object, boolean, java.lang.String)
	public MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, String preferedAfterID) {
		// afterid not fully supported yet
		return createEntryFromSkinRef(parentID, id, configID, title, titleInfo,
				params, closeable, "".equals(preferedAfterID) ? 0 : -1);
	}

	public MdiEntry createEntryFromEventListener(String parentEntryID,
			String parentViewID, UISWTViewEventListener l, String id,
			boolean closeable, Object datasource, String preferredAfterID) {
		if (isEntryClosedByUser(id)) {
			return null;
		}
		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		TabbedEntry entry = new TabbedEntry(this, skin, id, parentViewID);

		try {
			entry.setEventListener(l, true);
		} catch (UISWTViewEventCancelledException e) {
			entry.close(true);
			return null;
		}
		entry.setDatasource(datasource);
		entry.setPreferredAfterID(preferredAfterID);

		setupNewEntry(entry, id, -1, closeable);

		if (l instanceof IViewAlwaysInitialize) {
			entry.build();
		}

		return entry;
	}

	private boolean isEntryClosedByUser(String id) {
		
		if (mapUserClosedTabs.containsKey(id)) {
			return true;
		}
		// TODO Auto-generated method stub
		return false;
	}

	private void setupNewEntry(final TabbedEntry entry, final String id,
			final int index, boolean closeable) {
		addItem( entry );

		entry.setCloseable(closeable);

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				swt_setupNewEntry(entry, id, index);
			}
		});
	}

	private void swt_setupNewEntry(TabbedEntry entry, String id, int index) {
		if (tabFolder == null || tabFolder.isDisposed()) {
			return;
		}
		if (index < 0 || index >= tabFolder.getItemCount()) {
			index = tabFolder.getItemCount();
		}
		CTabItem cTabItem = new CTabItem(tabFolder, SWT.NONE, index);
		cTabItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (tabFolder.getItemCount() == 0) {
					currentEntry = null;
				}
			}
		});
		cTabItem.setData("TabbedEntry", entry);
		entry.setSwtItem(cTabItem);
		
		if (tabFolder.getItemCount() == 1) {
  		Utils.execSWTThreadLater(0, new AERunnable() {
  			
  			@Override
  			public void runSupport() {
  				if (currentEntry != null || tabFolder.isDisposed()) {
  					return;
  				}
  				CTabItem selection = tabFolder.getSelection();
  				if (selection == null) {
  					return;
  				}
  				TabbedEntry entry = getEntryFromTabItem(selection);
  				showEntry(entry);
  			}
  		});
		}
	}
	
	private TabbedEntry getEntryFromTabItem(CTabItem item) {
		if (item.isDisposed()) {
			return null;
		}
		return (TabbedEntry) item.getData("TabbedEntry");
	}

	public String getUpdateUIName() {
		String name = "MDI";
		MdiEntry entry = getCurrentEntry();
		if (entry != null) {
			name += "-" + entry.getId();
		}
		return name;
	}

	public void generate(IndentWriter writer) {
		MdiEntrySWT[] entries = getEntriesSWT();
		for (MdiEntrySWT entry : entries) {
			if (entry == null) {
				continue;
			}

			
			if (!(entry instanceof AEDiagnosticsEvidenceGenerator)) {
				writer.println("TabbedMdi View (No Generator): " + entry.getId());
				try {
					writer.indent();

					writer.println("Parent: " + entry.getParentID());
					writer.println("Title: " + entry.getTitle());
				} catch (Exception e) {

				} finally {

					writer.exdent();
				}
			}
		}
	}

	// @see com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT#getEntryFromSkinObject(org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject)
	public MdiEntrySWT getEntryFromSkinObject(PluginUISWTSkinObject pluginSkinObject) {
		if (pluginSkinObject instanceof SWTSkinObject) {
			Control control = ((SWTSkinObject) pluginSkinObject).getControl();
			while (control != null && !control.isDisposed()) {
				Object entry = control.getData("BaseMDIEntry");
				if (entry instanceof BaseMdiEntry) {
					BaseMdiEntry mdiEntry = (BaseMdiEntry) entry;
					return mdiEntry;
				}
				control = control.getParent();
			}
		}
		return null;
	}
	
	public MdiEntry createHeader(String id, String title, String preferredAfterID) {
		return null;
	}
	
	public CTabFolder getTabFolder() {
		return tabFolder;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.TabbedMdiInterface#setMaximizeVisible(boolean)
	 */
	public void setMaximizeVisible(final boolean visible) {
		maximizeVisible = visible;
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (tabFolder == null || tabFolder.isDisposed()) {
					return;
				}
				tabFolder.setMaximizeVisible(visible);
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.TabbedMdiInterface#setMinimizeVisible(boolean)
	 */
	public void setMinimizeVisible(final boolean visible) {
		minimizeVisible = visible;
		if (minimizeVisible) {
			boolean toMinimize = ConfigurationManager.getInstance().getBooleanParameter(props_prefix + ".subViews.minimized");
			setMinimized(toMinimize);
		}
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (tabFolder == null || tabFolder.isDisposed()) {
					return;
				}
				tabFolder.setMinimizeVisible(visible);
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.TabbedMdiInterface#getMinimized()
	 */
	public boolean getMinimized() {
		return minimized;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.TabbedMdiInterface#setMinimized(boolean)
	 */
	public void setMinimized(final boolean minimized) {
		this.minimized = minimized;
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (tabFolder == null || tabFolder.isDisposed()) {
					return;
				}
				
				if (minimized) {
					minimize(); 
				} else {
					restore();
				}
			}
		});
	}
	
	public int getFolderHeight() {
		return iFolderHeightAdj;
	}
	

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#dataSourceChanged(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	@Override
	public Object dataSourceChanged(SWTSkinObject skinObject, final Object ds) {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				if (tabFolder == null || tabFolder.isDisposed()) {
					return;
				}

				if (ds instanceof Object[]) {
					Object[] temp = (Object[]) ds;
					if (temp.length == 1) {
						Object obj = temp[0];

						if (obj instanceof DownloadManager) {
							maximizeTo = (DownloadManager) obj;
						} else if (obj instanceof Download) {
							maximizeTo = PluginCoreUtils.unwrap((Download) obj);
						}
					}
				}

				setMaximizeVisible(maximizeTo != null);

			}
		});

		return super.dataSourceChanged(skinObject, ds);
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
	 */
	public void parameterChanged(String parameterName) {
		if (isDisposed()) {
			return;
		}

		mapUserClosedTabs = COConfigurationManager.getMapParameter(parameterName, new HashMap());
		
		for (Object id : mapUserClosedTabs.keySet()) {
			String view_id = (String) id;
			if (entryExists(view_id)) {
				closeEntry(view_id);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.TabbedMdiInterface#setTabbedMdiMaximizeListener(com.aelitis.azureus.ui.swt.mdi.TabbedMdiMaximizeListener)
	 */
	public void setTabbedMdiMaximizeListener(TabbedMdiMaximizeListener l) {
		maximizeListener = l;
	}

	// @see org.gudy.azureus2.ui.swt.debug.ObfusticateImage#obfusticatedImage(org.eclipse.swt.graphics.Image)
	public Image obfusticatedImage(Image image) {
		MdiEntry[] entries = getEntries();
		for (MdiEntry entry : entries) {
			if (entry instanceof ObfusticateImage) {
				ObfusticateImage oi = (ObfusticateImage) entry;
				image = oi.obfusticatedImage(image);
			}
		}
		return image;
	}
	
	@Override
	protected MdiEntry 
	createEntryByCreationListener(String id, Object ds, Map<?, ?> autoOpenMap)
	{
		final TabbedEntry result = (TabbedEntry)super.createEntryByCreationListener(id, ds, autoOpenMap);
		
		if ( result != null ){
			PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
			PluginInterface pi = pm.getDefaultPluginInterface();
			UIManager uim = pi.getUIManager();
			MenuManager menuManager = uim.getMenuManager();
			org.gudy.azureus2.plugins.ui.menus.MenuItem menuItem = menuManager.addMenuItem(id + "._end_", "menu.pop.out");
			
			menuItem.addFillListener(
				new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
						
					public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
						
						menu.setVisible( result.canBuildStandAlone());
					}
				});
			
			menuItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
				public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
					
					SkinnedDialog skinnedDialog = 
							new SkinnedDialog( 
									"skin3_dlg_sidebar_popout", 
									"shell",
									null,	// standalone
									SWT.RESIZE | SWT.MAX | SWT.DIALOG_TRIM);
	
					SWTSkin skin = skinnedDialog.getSkin();
					
					SWTSkinObjectContainer cont = result.buildStandAlone((SWTSkinObjectContainer)skin.getSkinObject( "content-area" ));
						
					if ( cont != null ){
							
						Object ds = result.getDatasource();
						
						if ( ds instanceof Object[]){
							
							Object[] temp = (Object[])ds;
							
							if ( temp.length > 0 ){
								
								ds = temp[0];
							}
						}
						
						String ds_str = "";
						
						if ( ds instanceof Download ){
							
							ds_str = ((Download)ds).getName();
							
						}else if ( ds instanceof DownloadManager ){
							
							ds_str = ((DownloadManager)ds).getDisplayName();
						}
						
						skinnedDialog.setTitle( result.getTitle() + (ds_str.length()==0?"":(" - " + ds_str )));
						
						skinnedDialog.open();
						
					}else{
						
						skinnedDialog.close();
					}
				}
			});
			
		}
		
		return( result );
	}
	
	@Override
	public void fillMenu(Menu menu, final MdiEntry entry, String menuID) {

		super.fillMenu(menu, entry, menuID);
		
		if ( entry != null ){
			org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items = MenuItemManager.getInstance().getAllAsArray(entry.getId() + "._end_");
	
			if ( menu_items.length > 0 ){
				
				MenuBuildUtils.addPluginMenuItems(menu_items, menu, false, true,
						new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
							entry
						}));
			}
		}
	}
}
