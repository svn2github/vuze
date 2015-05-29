/**
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

package org.gudy.azureus2.ui.swt;

import java.io.File;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.GraphicURI;
import org.gudy.azureus2.plugins.ui.menus.MenuBuilder;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuItemImpl;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.IMenuConstants;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.mainwindow.SelectableSpeedMenu;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.shells.AdvRenameWindow;
import org.gudy.azureus2.ui.swt.views.table.TableSelectedRowsListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.TableContextMenuManager;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.speedmanager.SpeedLimitHandler;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author TuxPaper
 * @created Jan 20, 2015
 *
 */
public class TorrentMenuFancy
{
	private static final String HEADER_CONTROL = "Control";

	private static final String HEADER_SOCIAL = "Social";

	private static final String HEADER_ORGANIZE = "Organize";

	private static final String HEADER_MENU = "Other";

	private static final String HEADER_MSG_PREFIX = "FancyMenu.Header.";

	private static final String HEADER_CONTENT = "Content";

	private static class HeaderInfo
	{
		private Runnable runnable;

		private Composite composite;

		private String id;

		public HeaderInfo(String id, Runnable runnable, Composite composite) {
			this.id = id;
			this.runnable = runnable;
			this.composite = composite;
		}
	}

	private static class FancyRowInfo
	{
		private Listener listener;

		private Label lblText;

		private Label lblRight;

		private Label lblIcon;

		private Label lblCheck;

		private Composite cRow;

		private boolean keepMenu;

		private boolean isSelected;

		private boolean hasSubMenu;

		public void setEnabled(boolean enabled) {
			cRow.setEnabled(enabled);
		}

		public Label getRightLabel() {
			if (lblRight == null) {
				lblRight = new Label(cRow, SWT.NONE);
				GridData gd = new GridData();
				gd.horizontalIndent = 10;
				lblRight.setLayoutData(gd);
				lblRight.setEnabled(false);
			}
			return lblRight;
		}

		public Listener getListener() {
			return listener;
		}

		public void setListener(Listener listener) {
			this.listener = listener;
		}

		public Label getText() {
			return lblText;
		}

		public void setText(Label lblText) {
			this.lblText = lblText;
		}

		public void setRightLabel(Label lblRight) {
			this.lblRight = lblRight;
		}

		public void setRightLabelText(String s) {
			getRightLabel().setText(s);
		}

		public Label getIconLabel() {
			return lblIcon;
		}

		public void setIconLabel(Label lblIcon) {
			this.lblIcon = lblIcon;
		}

		public Composite getRow() {
			return cRow;
		}

		public void setRow(Composite cRow) {
			this.cRow = cRow;
		}

		public boolean keepMenu() {
			return keepMenu;
		}

		public void setKeepMenu(boolean keepMenu) {
			this.keepMenu = keepMenu;
		}

		public void setSelection(boolean isSelected) {
			this.isSelected = isSelected;
			ImageLoader.getInstance().setLabelImage(lblCheck,
					isSelected ? "check_yes" : "check_no");
		}

		public boolean isSelected() {
			return isSelected;
		}

		public void setCheckLabel(Label lblCheck) {
			this.lblCheck = lblCheck;
		}

		public boolean hasSubMenu() {
			return hasSubMenu;
		}

		public void setHasSubMenu(boolean hasSubMenu) {
			this.hasSubMenu = hasSubMenu;
		}
	}

	private static class FancyMenuRowInfo
		extends FancyRowInfo
	{
		private Menu menu;

		public Menu getMenu() {
			return menu;
		}

		public void setMenu(Menu menu) {
			this.menu = menu;
		}
	}

	private interface FancyMenuRowInfoListener
	{
		public void buildMenu(Menu menu);
	}

	protected static final boolean DEBUG_MENU = false;

	private static final int SHELL_MARGIN = 1;

	private List<FancyRowInfo> listRowInfos = new ArrayList<FancyRowInfo>();

	private List<HeaderInfo> listHeaders = new ArrayList<HeaderInfo>();

	private Composite topArea;

	private Composite detailArea;

	private Listener headerListener;

	private TableViewSWT<?> tv;

	private boolean isSeedingView;

	private Shell parentShell;

	private DownloadManager[] dms;

	private String tableID;

	private boolean hasSelection;

	private Map<String, String[]> mapMovedPluginMenus = new HashMap<String, String[]>();

	private Map<String, Integer> mapMovedPluginMenuUserMode = new HashMap<String, Integer>();

	private java.util.List<String> listMovedPluginIDs = new ArrayList<String>();

	private Shell shell;

	private Listener listenerForTrigger;

	private Listener listenerRow;

	private PaintListener listenerRowPaint;

	private TableColumnCore column;

	private HeaderInfo activatedHeader;

	private Menu currentMenu;

	private FancyRowInfo currentRowInfo;

	private Point originalShellLocation;

	private boolean subMenuVisible;

	private PaintListener paintListenerArrow;

	public TorrentMenuFancy(final TableViewSWT<?> tv,
			final boolean isSeedingView, Shell parentShell,
			final DownloadManager[] dms, final String tableID) {
		this.tv = tv;
		this.isSeedingView = isSeedingView;
		this.parentShell = parentShell;
		this.dms = dms;
		this.tableID = tableID;
		hasSelection = dms.length > 0;

		String[] ids_control = {
			"azpeerinjector.contextmenu.inject",
			"tablemenu.main.item",
			"StartStopRules.menu.viewDebug",
			"MyTorrentsView.menu.rename.displayed"
		};
		mapMovedPluginMenuUserMode.put("tablemenu.main.item", 2);
		mapMovedPluginMenuUserMode.put("azpeerinjector.contextmenu.inject", 2);

		mapMovedPluginMenus.put(HEADER_CONTROL, ids_control);
		listMovedPluginIDs.addAll(Arrays.asList(ids_control));

		String[] ids_social = {
			"azsubs.contextmenu.lookupassoc",
			"rcm.contextmenu.lookupassoc",
			"rcm.contextmenu.lookupsize",
			"MagnetPlugin.contextmenu.exporturi",
			"azbuddy.contextmenu",
			"RatingPlugin.contextmenu.manageRating",
			"label.chat",
		};
		mapMovedPluginMenus.put(HEADER_SOCIAL, ids_social);
		listMovedPluginIDs.addAll(Arrays.asList(ids_social));

		String[] ids_content = {
			"upnpmediaserver.contextmenu",
			"devices.contextmenu.xcode",
			"antivirus.ui.contextmenu.scan",
			"vuzexcode.transcode",
			"burn.menu.addtodvd"
		};
		mapMovedPluginMenus.put(HEADER_CONTENT, ids_content);
		listMovedPluginIDs.addAll(Arrays.asList(ids_content));

		listenerForTrigger = new Listener() {
			public void handleEvent(Event event) {
				FancyRowInfo rowInfo = findRowInfo(event.widget);
				if (rowInfo != null) {
					if (!rowInfo.keepMenu()) {
						shell.dispose();
					}

					if (rowInfo.getListener() != null) {
						rowInfo.getListener().handleEvent(event);
					}
				} else {
					shell.dispose();
				}
			}
		};

		paintListenerArrow = new PaintListener() {
			public void paintControl(PaintEvent e) {
				Control c = (Control) e.widget;
				Point size = c.getSize();
				int arrowSize = 8;
				int xStart = size.x - arrowSize;
				int yStart = size.y - (size.y + arrowSize) / 2;
				e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
				e.gc.setAntialias(SWT.ON);
				e.gc.fillPolygon(new int[] {
					xStart,
					yStart,
					xStart + arrowSize,
					yStart + 4,
					xStart,
					yStart + 8,
				});
			}
		};

		listenerRow = new Listener() {
			public void handleEvent(Event event) {
				Composite parent = detailArea;
				Rectangle bounds = parent.getBounds();
				if (event.type == SWT.MouseExit) {
					currentRowInfo = null;
					parent.redraw(0, 0, bounds.width, bounds.height, true);
				} else if (event.type == SWT.MouseEnter) {
					FancyRowInfo rowInfo = findRowInfo(event.widget);
					currentRowInfo = rowInfo;
					parent.redraw(0, 0, bounds.width, bounds.height, true);
				}
			}
		};

		listenerRowPaint = new PaintListener() {
			public void paintControl(PaintEvent e) {
				FancyRowInfo rowInfo = findRowInfo(e.widget);
				if (rowInfo == null) {
					return;
				}

				boolean isSelected = currentRowInfo == rowInfo;

				if (!isSelected) {
					for (Control control : ((Composite) e.widget).getChildren()) {
						control.setBackground(null);
						control.setForeground(null);
					}
					//System.out.println("bounds=" + bounds + "/" + cursorLocation + "/" + cursorLocationRel + "; clip=" + e.gc.getClipping());
					return;
				}
				Rectangle bounds = ((Control) e.widget).getBounds();

				Color bg = e.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				int arc = bounds.height / 3;
				e.gc.setBackground(bg);
				e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
				e.gc.setAntialias(SWT.ON);
				//System.out.println("clip=" + e.gc.getClipping());
				e.gc.fillRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, arc,
						arc);
				e.gc.setAlpha(100);
				e.gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, arc,
						arc);

				Color fg = e.display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
				for (Control control : ((Composite) e.widget).getChildren()) {
					control.setBackground(bg);
					control.setForeground(fg);
				}

			}
		};

		Collections.sort(listMovedPluginIDs);
	}

	public void showMenu(TableColumnCore acolumn, final Menu fallbackMenu ) {
		this.column = acolumn;
		Display d = parentShell.getDisplay();

		// We don't get mouse down notifications on trim or borders..
		shell = new Shell(parentShell, SWT.NO_TRIM | SWT.DOUBLE_BUFFERED) {
			protected void checkSubclass() {
			}

			public void dispose() {
				if (DEBUG_MENU) {
					System.out.println("Dispose via " + Debug.getCompressedStackTrace());
				}
				super.dispose();
			};
		};

		//FormLayout shellLayout = new FormLayout();
		RowLayout shellLayout = new RowLayout(SWT.VERTICAL);
		shellLayout.fill = true;
		shellLayout.marginBottom = shellLayout.marginLeft = shellLayout.marginRight = shellLayout.marginTop = 0;
		shellLayout.marginWidth = shellLayout.marginHeight = SHELL_MARGIN;

		shell.setLayout(shellLayout);
		shell.setBackgroundMode(SWT.INHERIT_FORCE);

		topArea = new Composite(shell, SWT.DOUBLE_BUFFERED);
		detailArea = new Composite(shell, SWT.DOUBLE_BUFFERED);

		topArea.setBackground(d.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		topArea.setForeground(d.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		FormData fd = Utils.getFilledFormData();
		fd.bottom = null;
		RowLayout topLayout = new RowLayout(SWT.HORIZONTAL);
		topLayout.spacing = 0;
		topLayout.pack = true;
		topLayout.marginBottom = topLayout.marginTop = topLayout.marginLeft = topLayout.marginRight = 0;
		topArea.setLayout(topLayout);

		//detailArea.setBackground(ColorCache.getRandomColor());
		fd = Utils.getFilledFormData();
		fd.top = new FormAttachment(topArea, 0, SWT.BOTTOM);
		FormLayout layoutDetailsArea = new FormLayout();
		layoutDetailsArea.marginWidth = 2;
		layoutDetailsArea.marginBottom = 2;
		detailArea.setLayout(layoutDetailsArea);

		headerListener = new Listener() {
			// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			public void handleEvent(Event e) {
				Control control = (Control) e.widget;
				if (e.type == SWT.Paint) {
					Rectangle bounds = control.getBounds();
					int y = bounds.height - 2;
					e.gc.drawLine(0, y, bounds.width, y);
				} else if (e.type == SWT.MouseEnter || e.type == SWT.Touch) {
					Object data = e.widget.getData("ID");

					if (data instanceof HeaderInfo) {
						HeaderInfo header = (HeaderInfo) data;
						if (DEBUG_MENU) {
							System.out.println("enter : " + data);
						}

						activateHeader(header);
					}
				}
			}
		};

		HeaderInfo firstHeader = addHeader(HEADER_CONTROL, HEADER_MSG_PREFIX
				+ HEADER_CONTROL, new AERunnable() {
			public void runSupport() {
				buildTorrentCustomMenu_Control(detailArea, dms);
			}
		});
		addHeader(HEADER_CONTENT, HEADER_MSG_PREFIX + HEADER_CONTENT,
				new AERunnable() {
					public void runSupport() {
						buildTorrentCustomMenu_Content(detailArea, dms);
					}
				});
		addHeader(HEADER_ORGANIZE, HEADER_MSG_PREFIX + HEADER_ORGANIZE,
				new AERunnable() {
					public void runSupport() {
						buildTorrentCustomMenu_Organize(detailArea, dms);
					}
				});
		addHeader(HEADER_SOCIAL, HEADER_MSG_PREFIX + HEADER_SOCIAL,
				new AERunnable() {
					public void runSupport() {
						buildTorrentCustomMenu_Social(detailArea);
					}
				});

		// Add table specific items
		final List<org.gudy.azureus2.plugins.ui.menus.MenuItem> listOtherItems = new ArrayList<org.gudy.azureus2.plugins.ui.menus.MenuItem>();

		TableContextMenuItem[] items = TableContextMenuManager.getInstance().getAllAsArray(
				tableID);

		for (TableContextMenuItem item : items) {
			if (Collections.binarySearch(listMovedPluginIDs, item.getResourceKey()) >= 0) {
				continue;
			}
			listOtherItems.add(item);
		}

		// Add Download Context specific menu items
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items = MenuItemManager.getInstance().getAllAsArray(
				MenuManager.MENU_DOWNLOAD_CONTEXT);
		for (org.gudy.azureus2.plugins.ui.menus.MenuItem item : menu_items) {
			if (Collections.binarySearch(listMovedPluginIDs, item.getResourceKey()) >= 0) {
				continue;
			}
			listOtherItems.add(item);
		}

		// Add Plugin Context menus..
		if (column != null) {
			TableContextMenuItem[] columnItems = column.getContextMenuItems(TableColumnCore.MENU_STYLE_COLUMN_DATA);
			for (TableContextMenuItem item : columnItems) {
				if (Collections.binarySearch(listMovedPluginIDs, item.getResourceKey()) >= 0) {
					continue;
				}
				listOtherItems.add(item);
			}
		}

		if (listOtherItems.size() > 0) {
			addHeader(HEADER_MENU, HEADER_MSG_PREFIX + HEADER_MENU, new AERunnable() {
				public void runSupport() {
					buildTorrentCustomMenu_Other(detailArea, listOtherItems);
				}
			});
		}

		originalShellLocation = d.getCursorLocation();
		originalShellLocation.x -= 5;
		originalShellLocation.y -= 16;

		shell.setLocation(originalShellLocation);

		shell.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_BORDER));
				Rectangle clientArea = shell.getClientArea();
				e.gc.drawRectangle(0, 0, clientArea.width - 1, clientArea.height - 1);
			}
		});

		shell.addKeyListener(new KeyListener() {

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if (currentRowInfo == null) {
						currentRowInfo = listRowInfos.get(0);
					} else {
						boolean next = false;
						for (FancyRowInfo rowInfo : listRowInfos) {
							if (next) {
								currentRowInfo = rowInfo;
								next = false;
								break;
							}
							if (rowInfo == currentRowInfo) {
								next = true;
							}
						}
						if (next) {
							currentRowInfo = listRowInfos.get(0);
						}
					}
					Rectangle bounds = detailArea.getBounds();
					detailArea.redraw(0, 0, bounds.width, bounds.height, true);
				} else if (e.keyCode == SWT.ARROW_UP) {
					if (currentRowInfo == null) {
						currentRowInfo = listRowInfos.get(listRowInfos.size() - 1);
					} else {
						FancyRowInfo previous = listRowInfos.get(listRowInfos.size() - 1);
						for (FancyRowInfo rowInfo : listRowInfos) {
							if (rowInfo == currentRowInfo) {
								currentRowInfo = previous;
								break;
							}
							previous = rowInfo;
						}
					}
					Rectangle bounds = detailArea.getBounds();
					detailArea.redraw(0, 0, bounds.width, bounds.height, true);
				} else if (e.keyCode == SWT.ARROW_LEFT) {
					HeaderInfo previous = listHeaders.get(listHeaders.size() - 1);
					for (HeaderInfo header : listHeaders) {
						if (header == activatedHeader) {
							activateHeader(previous);
							break;
						}
						previous = header;
					}
				} else if (e.keyCode == SWT.ARROW_RIGHT) {
					if (currentRowInfo != null && currentRowInfo.hasSubMenu()) {
						Event event = new Event();
						event.display = e.display;
						event.widget = currentRowInfo.cRow;
						listenerForTrigger.handleEvent(event);
					} else {
						boolean next = false;
						for (HeaderInfo header : listHeaders) {
							if (next) {
								activateHeader(header);
								next = false;
								break;
							}
							if (header == activatedHeader) {
								next = true;
							}
						}
						if (next) {
							activateHeader(listHeaders.get(0));
						}
					}
				}
			}
		});

		if ( fallbackMenu != null ){
			
			firstHeader.composite.addMenuDetectListener(
					new MenuDetectListener() {
						
						public void menuDetected(MenuDetectEvent e) {
							shell.dispose();
							fallbackMenu.setVisible( true );
						}
					});
		}
		
		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					if (DEBUG_MENU) {
						System.out.println("Dispose via ESCAPE");
					}
					shell.dispose();
				} else if (e.detail == SWT.TRAVERSE_RETURN) {
					if (currentRowInfo != null) {
						Event event = new Event();
						event.display = e.display;
						event.widget = currentRowInfo.cRow;
						listenerForTrigger.handleEvent(event);
					}
				}
			}
		});

		shell.addShellListener(new ShellListener() {
			public void shellIconified(ShellEvent e) {
			}

			public void shellDeiconified(ShellEvent e) {
			}

			public void shellDeactivated(ShellEvent e) {
				// Must do later, so clicks go to wherever
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						if (subMenuVisible) {
							return;
						}
						if (shell.isDisposed()) {
							return;
						}
						Shell[] shells = shell.getShells();
						if (shells != null && shells.length > 0) {
							for (Shell aShell : shells) {
								if (!aShell.isDisposed()) {
									return;
								}
							}
						}
						shell.dispose();
					}
				});
			}

			public void shellClosed(ShellEvent e) {
			}

			public void shellActivated(ShellEvent e) {
			}
		});

		activateHeader(firstHeader);

		shell.open();
	}

	protected void activateHeader(HeaderInfo header) {
		if (header == null || activatedHeader == header) {
			return;
		}

		if (currentMenu != null && !currentMenu.isDisposed()) {
			currentMenu.setVisible(false);
		}
		Display d = header.composite.getDisplay();
		header.composite.setBackground(d.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		header.composite.setForeground(d.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

		Utils.disposeSWTObjects(detailArea.getChildren());
		listRowInfos.clear();
		currentRowInfo = null;

		if (header.runnable != null) {
			header.runnable.run();
		}

		String[] ids = mapMovedPluginMenus.get(header.id);
		if (ids != null) {
			addTableItemsWithID(detailArea, tableID, ids);
			addMenuItemsWithID(detailArea, MenuManager.MENU_DOWNLOAD_CONTEXT, ids);
			if (column != null) {
				TableContextMenuItem[] columnItems = column.getContextMenuItems(TableColumnCore.MENU_STYLE_COLUMN_DATA);
				addItemsArray(detailArea, columnItems, ids);
			}
		}

		Control lastControl = null;
		for (Control child : detailArea.getChildren()) {
			FormData fd = new FormData();
			if (lastControl == null) {
				fd.top = new FormAttachment(0);
			} else {
				fd.top = new FormAttachment(lastControl);
			}
			fd.left = new FormAttachment(0, 0);
			fd.right = new FormAttachment(100, 0);
			child.setLayoutData(fd);
			lastControl = child;
		}

		shell.setLocation(shell.getLocation().x, originalShellLocation.y);
		detailArea.moveBelow(null);
		shell.pack(true);
		detailArea.layout(true, true);

		Point shellSize = shell.getSize();
		Point ptBottomRight = shell.toDisplay(shellSize);
		Rectangle monitorArea = shell.getMonitor().getClientArea();
		if (ptBottomRight.x > monitorArea.x + monitorArea.width) {
			shell.setLocation(monitorArea.x + monitorArea.width - shellSize.x,
					shell.getLocation().y);
		}

		if (ptBottomRight.y > monitorArea.y + monitorArea.height) {
			// Bottom-Up
			if (shell.getChildren()[0] != detailArea) {
				shell.setLocation(shell.getLocation().x, originalShellLocation.y
						- detailArea.getSize().y - 3);
				detailArea.moveAbove(null);
				lastControl = null;
				Control[] children = detailArea.getChildren();
				for (int i = 0; i < children.length; i++) {
					Control child = children[children.length - i - 1];
					FormData fd = new FormData();
					if (lastControl == null) {
						fd.top = new FormAttachment(0);
					} else {
						fd.top = new FormAttachment(lastControl);
					}
					fd.left = new FormAttachment(0, 0);
					fd.right = new FormAttachment(100, 0);
					child.setLayoutData(fd);
					lastControl = child;
				}
				shell.layout(true, true);
			}
		}

		if (activatedHeader != null) {
			activatedHeader.composite.setBackground(d.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			activatedHeader.composite.setForeground(d.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		}

		activatedHeader = header;
	}

	public void buildTorrentCustomMenu_Control(final Composite cParent,
			final DownloadManager[] dms) {
		final int userMode = COConfigurationManager.getIntParameter("User Mode");

		boolean start = false;
		boolean stop = false;
		boolean recheck = false;
		boolean barsOpened = true;
		boolean bChangeDir = hasSelection;

		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];

			if (barsOpened && !DownloadBar.getManager().isOpen(dm)) {
				barsOpened = false;
			}
			stop = stop || ManagerUtils.isStopable(dm);

			start = start || ManagerUtils.isStartable(dm);

			recheck = recheck || dm.canForceRecheck();

			boolean stopped = ManagerUtils.isStopped(dm);

			int state = dm.getState();
			bChangeDir &= (state == DownloadManager.STATE_ERROR
					|| state == DownloadManager.STATE_STOPPED || state == DownloadManager.STATE_QUEUED);
			;
			/**
			 * Only perform a test on disk if:
			 *    1) We are currently set to allow the "Change Data Directory" option, and
			 *    2) We've only got one item selected - otherwise, we may potentially end up checking massive
			 *       amounts of files across multiple torrents before we generate a menu.
			 */
			if (bChangeDir && dms.length == 1) {
				bChangeDir = dm.isDataAlreadyAllocated();
				if (bChangeDir && state == DownloadManager.STATE_ERROR) {
					// filesExist is way too slow!
					bChangeDir = !dm.filesExist(true);
				} else {
					DiskManagerFileInfo[] files = dm.getDiskManagerFileInfoSet().getFiles();
					bChangeDir = false;
					for (DiskManagerFileInfo info : files) {
						if (info.isSkipped()) {
							continue;
						}
						bChangeDir = !info.getFile(true).exists();
						break;
					}
				}
			}
		}
		Composite cQuickCommands = new Composite(cParent, SWT.NONE);
		//cQuickCommands.setBackground(ColorCache.getRandomColor());
		RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.justify = true;
		rowLayout.marginLeft = 0;
		rowLayout.marginRight = 0;
		cQuickCommands.setLayout(rowLayout);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		cQuickCommands.setLayoutData(gd);

		// Queue
		createActionButton(dms, cQuickCommands, "MyTorrentsView.menu.queue",
				"start", start, new ListenerGetOffSWT() {
					void handleEventOffSWT(Event event) {
						TorrentUtil.queueDataSources(dms, true);
					}
				});

		// Force Start
		if (userMode > 0) {
			boolean forceStart = false;
			boolean forceStartEnabled = false;

			for (int i = 0; i < dms.length; i++) {
				DownloadManager dm = dms[i];

				forceStartEnabled = forceStartEnabled
						|| ManagerUtils.isForceStartable(dm);

				forceStart = forceStart || dm.isForceStart();
			}

			final boolean newForceStart = !forceStart;

			createActionButton(dms, cQuickCommands, "MyTorrentsView.menu.forceStart",
					"forcestart", forceStartEnabled, forceStart ? SWT.BORDER : SWT.PUSH,
					new ListenerDMTask(dms) {
						public void run(DownloadManager dm) {
							if (ManagerUtils.isForceStartable(dm)) {
								dm.setForceStart(newForceStart);
							}
						}
					});
		}

		// Pause
		if (userMode > 0) {
			createActionButton(dms, cQuickCommands, "v3.MainWindow.button.pause",
					"pause", stop, new ListenerGetOffSWT() {
						public void handleEventOffSWT(Event event) {
							TorrentUtil.pauseDataSources(dms);
						}
					});
		}

		// Stop
		createActionButton(dms, cQuickCommands, "MyTorrentsView.menu.stop", "stop",
				stop, new ListenerGetOffSWT() {
					public void handleEventOffSWT(Event event) {
						TorrentUtil.stopDataSources(dms);
					}
				});

		// Force Recheck
		createActionButton(dms, cQuickCommands, "MyTorrentsView.menu.recheck",
				"recheck", recheck, new ListenerDMTask(dms) {
					public void run(DownloadManager dm) {
						if (dm.canForceRecheck()) {
							dm.forceRecheck();
						}
					}
				});

		// Delete
		createActionButton(dms, cQuickCommands, "menu.delete.options", "delete",
				hasSelection, new Listener() {
					public void handleEvent(Event event) {
						TorrentUtil.removeDownloads(dms, null, true);
					}
				});

		///////////////////////////////////////////////////////////////////////////

		if (bChangeDir) {
			createRow(cParent, "MyTorrentsView.menu.changeDirectory", null,
					new Listener() {
						public void handleEvent(Event e) {
							TorrentUtil.changeDirSelectedTorrents(dms, parentShell);
						}
					});
		}

		// Open Details
		if (hasSelection) {
			createRow(cParent, "MyTorrentsView.menu.showdetails", "details",
					new ListenerDMTask(dms) {
						public void run(DownloadManager dm) {
							UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
							if (uiFunctions != null) {
								uiFunctions.getMDI().showEntryByID(
										MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_DETAILS, dm);
							}
						}
					});
		}

		// Open Bar
		if (hasSelection) {
			FancyRowInfo row = createRow(cParent,
					"MyTorrentsView.menu.showdownloadbar", "downloadBar",
					new ListenerDMTask(dms) {
						public void run(DownloadManager dm) {
							if (DownloadBar.getManager().isOpen(dm)) {
								DownloadBar.close(dm);
							} else {
								DownloadBar.open(dm, parentShell);
							}
						} // run
					});
			row.setSelection(barsOpened);
		}

		//////////////////////////////////////

		if (hasSelection) {
			FancyRowInfo rowSpeedDL = createRow(cParent,
					"MyTorrentsView.menu.downSpeedLimit", "image.torrentspeed.down",
					false, new Listener() {
						public void handleEvent(Event e) {
							Event event = new Event();
							event.type = SWT.MouseUp;
							event.widget = e.widget;
							event.stateMask = e.stateMask;
							event.button = e.button;
							e.display.post(event);

							AzureusCore core = AzureusCoreFactory.getSingleton();
							SelectableSpeedMenu.invokeSlider((Control) event.widget, core,
									dms, false, shell);
							if (e.display.getActiveShell() != shell) {
								if (!shell.isDisposed()) {
									shell.dispose();
								}
								return;
							}
							FancyRowInfo rowInfo = findRowInfo(event.widget);
							if (rowInfo != null) {
								updateRowSpeed(rowInfo, false);
							}

						}
					});
			rowSpeedDL.keepMenu = true;

			updateRowSpeed(rowSpeedDL, false);
		}

		if (hasSelection) {
			FancyRowInfo rowSpeedUL = createRow(cParent,
					"MyTorrentsView.menu.upSpeedLimit", "image.torrentspeed.up", false,
					new Listener() {
						public void handleEvent(Event e) {
							Event event = new Event();
							event.type = SWT.MouseUp;
							event.widget = e.widget;
							event.stateMask = e.stateMask;
							event.button = e.button;
							e.display.post(event);

							AzureusCore core = AzureusCoreFactory.getSingleton();
							SelectableSpeedMenu.invokeSlider((Control) e.widget, core, dms,
									true, shell);
							if (e.display.getActiveShell() != shell) {
								if (!shell.isDisposed()) {
									shell.dispose();
								}
								return;
							}
							FancyRowInfo rowInfo = findRowInfo(event.widget);
							if (rowInfo != null) {
								updateRowSpeed(rowInfo, true);
							}
						}
					});
			rowSpeedUL.keepMenu = true;

			updateRowSpeed(rowSpeedUL, true);
		}

		//////////////////////////////////////

		if (hasSelection && userMode > 0) {
			createMenuRow(cParent, "MyTorrentsView.menu.tracker", null,
					new FancyMenuRowInfoListener() {
						public void buildMenu(Menu menu) {
							boolean changeUrl = hasSelection;
							boolean manualUpdate = true;
							boolean allStopped = true;

							int userMode = COConfigurationManager.getIntParameter("User Mode");
							final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");

							for (DownloadManager dm : dms) {
								boolean stopped = ManagerUtils.isStopped(dm);

								allStopped &= stopped;

								if (userMode < 2) {
									TRTrackerAnnouncer trackerClient = dm.getTrackerClient();

									if (trackerClient != null) {
										boolean update_state = ((SystemTime.getCurrentTime() / 1000
												- trackerClient.getLastUpdateTime() >= TRTrackerAnnouncer.REFRESH_MINIMUM_SECS));
										manualUpdate = manualUpdate & update_state;
									}
								}

							}

							TorrentUtil.addTrackerTorrentMenu(menu, dms, changeUrl,
									manualUpdate, allStopped, use_open_containing_folder);
						}

					});
		}

		if (hasSelection) {
			AzureusCore azureus_core = AzureusCoreFactory.getSingleton();

			SpeedLimitHandler slh = SpeedLimitHandler.getSingleton(azureus_core);

			if (slh.hasAnyProfiles()) {
				
				createMenuRow(cParent, IMenuConstants.MENU_ID_SPEED_LIMITS, null,
						new FancyMenuRowInfoListener() {
							public void buildMenu(Menu menu) {
								TorrentUtil.addSpeedLimitsMenu(dms, menu);
							}
						});
			}
		}

		if (userMode > 0 && hasSelection) {

			boolean can_pause = false;

			for (int i = 0; i < dms.length; i++) {
				DownloadManager dm = dms[i];
				if (ManagerUtils.isPauseable(dm)) {
					can_pause = true;
					break;
				}
			}

			if ( can_pause ){
				
				createRow(detailArea, "MainWindow.menu.transfers.pausetransfersfor",
					null, new Listener() {
						public void handleEvent(Event event) {
							TorrentUtil.pauseDownloadsFor(dms);
						}
					});
			}
		}

		// === advanced > options ===
		// ===========================

		if (userMode > 0 && dms.length > 1) {
			createRow(cParent, "label.options.and.info", null,
					new ListenerDMTask(dms) {
						public void run(DownloadManager[] dms) {
							UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
							if (uiFunctions != null) {
								uiFunctions.getMDI().showEntryByID(
										MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_OPTIONS,
										dms);
							}
						}
					});
		}

		// === advanced > peer sources ===
		// ===============================

		if (userMode > 0) {
			createMenuRow(cParent, "MyTorrentsView.menu.peersource", null,
					new FancyMenuRowInfoListener() {
						public void buildMenu(Menu menu) {
							TorrentUtil.addPeerSourceSubMenu(dms, menu);
						}
					});
		}

		// IP Filter Enable
		if (userMode > 0
				&& IpFilterManagerFactory.getSingleton().getIPFilter().isEnabled()) {

			boolean allEnabled = true;
			boolean allDisabled = true;

			for (int j = 0; j < dms.length; j++) {
				DownloadManager dm = dms[j];

				boolean filterDisabled = dm.getDownloadState().getFlag(
						DownloadManagerState.FLAG_DISABLE_IP_FILTER);

				if (filterDisabled) {
					allEnabled = false;
				} else {
					allDisabled = false;
				}
			}

			boolean bChecked;

			if (allEnabled) {
				bChecked = true;
			} else if (allDisabled) {
				bChecked = false;
			} else {
				bChecked = false;
			}

			final boolean newDisable = bChecked;

			FancyRowInfo row = createRow(cParent, "MyTorrentsView.menu.ipf_enable",
					null, new ListenerDMTask(dms) {
						public void run(DownloadManager dm) {
							dm.getDownloadState().setFlag(
									DownloadManagerState.FLAG_DISABLE_IP_FILTER, newDisable);
						}
					});

			row.setSelection(bChecked);
		}

		// === advanced > networks ===
		// ===========================

		if (userMode > 1) {
			createMenuRow(cParent, "MyTorrentsView.menu.networks", null,
					new FancyMenuRowInfoListener() {
						public void buildMenu(Menu menu) {
							TorrentUtil.addNetworksSubMenu(dms, menu);
						}
					});
		}

		// Advanced menu with stuff I don't know where to put
		if (userMode > 0) {
			createMenuRow(cParent, "MyTorrentsView.menu.advancedmenu", null,
					new FancyMenuRowInfoListener() {
						public void buildMenu(Menu menu) {

							boolean allStopped = true;
							boolean allScanSelected = true;
							boolean allScanNotSelected = true;
							boolean fileMove = true;

							for (DownloadManager dm : dms) {
								boolean stopped = ManagerUtils.isStopped(dm);

								allStopped &= stopped;

								fileMove = fileMove && dm.canMoveDataFiles();

								boolean scan = dm.getDownloadState().getFlag(
										DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES);

								// include DND files in incomplete stat, since a recheck may
								// find those files have been completed
								boolean incomplete = !dm.isDownloadComplete(true);

								allScanSelected = incomplete && allScanSelected && scan;
								allScanNotSelected = incomplete && allScanNotSelected && !scan;
							}

							boolean fileRescan = allScanSelected || allScanNotSelected;

							final MenuItem itemFileMoveTorrent = new MenuItem(menu, SWT.PUSH);
							Messages.setLanguageText(itemFileMoveTorrent,
									"MyTorrentsView.menu.movetorrent");
							itemFileMoveTorrent.addListener(SWT.Selection,
									new ListenerDMTask(dms) {
										public void run(DownloadManager[] dms) {
											TorrentUtil.moveTorrentFile(parentShell, dms);
										}
									});
							itemFileMoveTorrent.setEnabled(fileMove);

							final MenuItem itemFileRescan = new MenuItem(menu, SWT.CHECK);
							Messages.setLanguageText(itemFileRescan,
									"MyTorrentsView.menu.rescanfile");
							itemFileRescan.addListener(SWT.Selection,
									new ListenerDMTask(dms) {
										public void run(DownloadManager dm) {
											dm.getDownloadState().setFlag(
													DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES,
													itemFileRescan.getSelection());
										}
									});
							itemFileRescan.setSelection(allScanSelected);
							itemFileRescan.setEnabled(fileRescan);

							// clear allocation

							MenuItem itemFileClearAlloc = new MenuItem(menu, SWT.PUSH);
							Messages.setLanguageText(itemFileClearAlloc,
									"MyTorrentsView.menu.clear_alloc_data");
							itemFileClearAlloc.addListener(SWT.Selection, new ListenerDMTask(
									dms) {
								public void run(DownloadManager dm) {
									dm.setDataAlreadyAllocated(false);
								}
							});

							itemFileClearAlloc.setEnabled(allStopped);

							// clear resume

							MenuItem itemFileClearResume = new MenuItem(menu, SWT.PUSH);
							Messages.setLanguageText(itemFileClearResume,
									"MyTorrentsView.menu.clear_resume_data");
							itemFileClearResume.addListener(SWT.Selection,
									new ListenerDMTask(dms) {
										public void run(DownloadManager dm) {
											dm.getDownloadState().clearResumeData();
										}
									});
							itemFileClearResume.setEnabled(allStopped);

							if (userMode > 1 && isSeedingView) {

								boolean canSetSuperSeed = false;
								boolean superSeedAllYes = true;
								boolean superSeedAllNo = true;
								for (DownloadManager dm : dms) {
									PEPeerManager pm = dm.getPeerManager();

									if (pm != null) {

										if (pm.canToggleSuperSeedMode()) {

											canSetSuperSeed = true;
										}

										if (pm.isSuperSeedMode()) {

											superSeedAllYes = false;

										} else {

											superSeedAllNo = false;
										}
									} else {
										superSeedAllYes = false;
										superSeedAllNo = false;
									}
								}

								final MenuItem itemSuperSeed = new MenuItem(menu, SWT.CHECK);

								Messages.setLanguageText(itemSuperSeed,
										"ManagerItem.superseeding");

								boolean enabled = canSetSuperSeed
										&& (superSeedAllNo || superSeedAllYes);

								itemSuperSeed.setEnabled(enabled);

								final boolean selected = superSeedAllNo;

								if (enabled) {

									itemSuperSeed.setSelection(selected);

									itemSuperSeed.addListener(SWT.Selection, new ListenerDMTask(
											dms) {
										public void run(DownloadManager dm) {
											PEPeerManager pm = dm.getPeerManager();

											if (pm != null) {

												if (pm.isSuperSeedMode() == selected
														&& pm.canToggleSuperSeedMode()) {

													pm.setSuperSeedMode(!selected);
												}
											}
										}
									});
								}
							}

						}
					});
		}
	}

	private void updateRowSpeed(FancyRowInfo row, boolean isUpload) {
		int dlRate = isUpload
				? dms[0].getStats().getUploadRateLimitBytesPerSecond()
				: dms[0].getStats().getDownloadRateLimitBytesPerSecond();
		for (DownloadManager dm : dms) {
			int dlRate2 = isUpload ? dm.getStats().getUploadRateLimitBytesPerSecond()
					: dm.getStats().getDownloadRateLimitBytesPerSecond();
			if (dlRate != dlRate2) {
				dlRate = -2;
				break;
			}
		}
		if (dlRate != -2) {
			String currentSpeed;
			if (dlRate == 0) {
				currentSpeed = MessageText.getString("MyTorrentsView.menu.setSpeed.unlimited");
			} else if (dlRate < 0) {
				currentSpeed = MessageText.getString("MyTorrentsView.menu.setSpeed.disabled");
			} else {
				currentSpeed = DisplayFormatters.formatByteCountToKiBEtcPerSec(dlRate);
			}
			row.setRightLabelText(currentSpeed);
			row.cRow.layout();
		}
	}

	private FancyMenuRowInfo createMenuRow(Composite cParent, String keyTitle,
			String keyImage, final FancyMenuRowInfoListener listener) {

		Listener showSWTMenuListener = new Listener() {
			int lastX = 0;

			int lastY = 0;

			public void handleEvent(final Event event) {
				if (event.type == SWT.MouseHover && lastX == event.x
						&& lastY == event.y) {
					return;
				}
				lastX = event.x;
				lastY = event.y;

				FancyMenuRowInfo rowInfo;

				FancyRowInfo findRowInfo = findRowInfo(event.widget);
				if (!(findRowInfo instanceof FancyMenuRowInfo)) {
					return;
				}

				rowInfo = (FancyMenuRowInfo) findRowInfo;
				currentMenu = rowInfo.getMenu();
				if (currentMenu != null && !currentMenu.isDisposed()) {
					return;
				}

				currentMenu = new Menu(parentShell, SWT.POP_UP);
				rowInfo.setMenu(currentMenu);

				currentMenu.addMenuListener(new MenuListener() {

					public void menuShown(MenuEvent arg0) {
						subMenuVisible = true;
					}

					public void menuHidden(final MenuEvent arg0) {
						subMenuVisible = false;
						currentMenu = null;
						Utils.execSWTThreadLater(0, new Runnable() {

							public void run() {
								arg0.widget.dispose();
							}
						});
					}
				});
				listener.buildMenu(currentMenu);

				Composite rowComposite = rowInfo.getRow();

				if (rowComposite != null) {
					Point size = rowComposite.getSize();
					Point menuLocation = rowComposite.toDisplay(size.x - 3, -3);
					currentMenu.setLocation(menuLocation);
				}
				if (currentMenu.getItemCount() > 0) {
					currentMenu.setVisible(true);

					addMenuItemListener(currentMenu, listenerForTrigger);

					final FancyMenuRowInfo currentRow = rowInfo;
					final Point currentMousePos = event.display.getCursorLocation();
					// Once the menu is visible, we don't get mouse events (even with addFilter)
					Utils.execSWTThreadLater(300, new Runnable() {
						public void run() {
							Point cursorLocation = event.display.getCursorLocation();
							if (currentMousePos.equals(cursorLocation)) {
								Utils.execSWTThreadLater(300, this);
								return;
							}

							Control control = Utils.getCursorControl();

							if (control != null) {
								Object data = control.getData("ID");
								if (data instanceof HeaderInfo) {
									HeaderInfo header = (HeaderInfo) data;
									activateHeader(header);
								}
							}

							Menu submenu = currentRow.getMenu();
							if (submenu == null || submenu.isDisposed()
									|| !submenu.isVisible()) {
								return;
							}
							FancyRowInfo rowInfo = findRowInfo(control);
							if (rowInfo != null && rowInfo != currentRow) {
								submenu.setVisible(false);
								return;
							}
							Utils.execSWTThreadLater(300, this);
						}
					});
				} else {
					currentMenu.dispose();
					currentMenu = null;
				}
			}
		};

		FancyMenuRowInfo row = new FancyMenuRowInfo();
		createRow(cParent, keyTitle, keyImage, true, showSWTMenuListener, row);
		row.setHasSubMenu(true);

		Composite cRow = row.getRow();
		Utils.addListenerAndChildren(cRow, SWT.MouseHover, showSWTMenuListener);

		row.setKeepMenu(true);
		//row.getRightLabel().setText("\u25B6");
		Label rightLabel = row.getRightLabel();
		GridData gd = new GridData(12, SWT.DEFAULT);
		rightLabel.setLayoutData(gd);
		row.getRightLabel().addPaintListener(paintListenerArrow);

		return row;
	}

	protected void addMenuItemListener(Menu menu, Listener l) {
		for (MenuItem item : menu.getItems()) {
			if (item.getStyle() == SWT.CASCADE) {
				addMenuItemListener(item.getMenu(), l);
			} else {
				item.addListener(SWT.Selection, l);
			}
		}
	}

	private FancyRowInfo createRow(Composite cParent, String keyTitle,
			final String keyImage, final Listener triggerListener) {
		return createRow(cParent, keyTitle, keyImage, true, triggerListener,
				new FancyRowInfo());
	}

	private FancyRowInfo createRow(Composite cParent, String keyTitle,
			String keyImage, boolean triggerOnUp, Listener triggerListener) {
		return createRow(cParent, keyTitle, keyImage, triggerOnUp, triggerListener,
				new FancyRowInfo());
	}

	private FancyRowInfo createRow(Composite cParent, String keyTitle,
			String keyImage, boolean triggerOnUp, Listener triggerListener,
			FancyRowInfo rowInfo) {

		Composite cRow = new Composite(cParent, SWT.NONE);
		//cRow.setBackground(ColorCache.getRandomColor());

		cRow.setData("ID", rowInfo);
		GridLayout gridLayout = new GridLayout(4, false);
		gridLayout.marginWidth = 1;
		gridLayout.marginHeight = 3;
		gridLayout.marginRight = 4;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		cRow.setLayout(gridLayout);

		GridData gridData;

		Label lblIcon = new Label(cRow, SWT.CENTER | SWT.NONE);
		gridData = new GridData();
		gridData.widthHint = 20;
		lblIcon.setLayoutData(gridData);
		if (keyImage != null) {
			ImageLoader.getInstance().setLabelImage(lblIcon, keyImage);
		}

		Label item = new Label(cRow, SWT.NONE);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalIndent = 2;
		item.setLayoutData(gridData);
		Messages.setLanguageText(item, keyTitle);

		Label lblCheck = new Label(cRow, SWT.CENTER);
		gridData = new GridData();
		gridData.widthHint = 13;
		lblCheck.setLayoutData(gridData);

		if (triggerListener != null) {
			Utils.addListenerAndChildren(cRow, triggerOnUp ? SWT.MouseUp
					: SWT.MouseDown, listenerForTrigger);
		}

		Utils.addListenerAndChildren(cRow, SWT.MouseEnter, listenerRow);
		Utils.addListenerAndChildren(cRow, SWT.MouseExit, listenerRow);

		cRow.addPaintListener(listenerRowPaint);

		rowInfo.setListener(triggerListener);
		rowInfo.setRow(cRow);
		rowInfo.setIconLabel(lblIcon);
		rowInfo.setText(item);
		rowInfo.setRightLabel(null);
		rowInfo.setCheckLabel(lblCheck);

		listRowInfos.add(rowInfo);
		return rowInfo;
	}

	private FancyRowInfo findRowInfo(Widget widget) {
		Object findData = findData(widget, "ID");
		if (findData instanceof FancyRowInfo) {
			return (FancyRowInfo) findData;

		}
		return null;
	}

	protected Object findData(Widget widget, String id) {
		if (widget == null || widget.isDisposed()) {
			return null;
		}
		Object o = widget.getData(id);
		if (o != null) {
			return o;
		}
		if (widget instanceof Control) {
			Control control = ((Control) widget).getParent();
			while (control != null) {
				o = control.getData(id);
				if (o != null) {
					return o;
				}
				control = control.getParent();
			}
		}
		return null;
	}

	private Control createActionButton(final DownloadManager[] dms,
			Composite cParent, String keyToolTip, String keyImage, boolean enable,
			Listener listener) {
		return createActionButton(dms, cParent, keyToolTip, keyImage, enable,
				SWT.BORDER, listener);
	}

	private Control createActionButton(final DownloadManager[] dms,
			Composite cParent, String keyToolTip, final String keyImage,
			boolean enable, int style, final Listener listener) {
		final Canvas item = new Canvas(cParent, SWT.NO_BACKGROUND
				| SWT.DOUBLE_BUFFERED);

		Listener l = new Listener() {
			private boolean inWidget;

			public void handleEvent(Event e) {
				Control c = (Control) e.widget;
				if (e.type == SWT.Paint) {
					Point size = c.getSize();
					if (inWidget) {
						e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
					} else {
						e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
					}
					e.gc.setAdvanced(true);
					e.gc.setAntialias(SWT.ON);
					e.gc.fillRoundRectangle(0, 0, size.x - 1, size.y - 1, 6, 6);
					e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
					e.gc.drawRoundRectangle(0, 0, size.x - 1, size.y - 1, 6, 6);
					e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
					e.gc.drawRoundRectangle(1, 1, size.x - 3, size.y - 3, 6, 6);

					Image image = ImageLoader.getInstance().getImage(
							c.isEnabled() ? keyImage : keyImage + "-disabled");
					Rectangle bounds = image.getBounds();
					int x = size.x / 2 - bounds.width / 2;
					int y = size.y / 2 - bounds.height / 2;

					e.gc.drawImage(image, x, y);
				} else if (e.type == SWT.MouseEnter) {
					inWidget = true;
					c.redraw();
				} else if (e.type == SWT.MouseExit) {
					inWidget = false;
					c.redraw();
				}
			}
		};

		item.addListener(SWT.MouseEnter, l);
		item.addListener(SWT.MouseExit, l);
		item.addListener(SWT.Paint, l);

		Messages.setLanguageTooltip(item, keyToolTip);
		item.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				listener.handleEvent(event);
				shell.dispose();
			}
		});
		item.setEnabled(enable);

		RowData rowData = new RowData(30, 21);
		item.setLayoutData(rowData);

		return item;
	}

	public void buildTorrentCustomMenu_Organize(final Composite detailArea,
			final DownloadManager[] dms) {

		if (!hasSelection) {
			return;
		}
		
			// assign tags
			
		createMenuRow(detailArea, "label.tags", "image.sidebar.tag-overview",
				new FancyMenuRowInfoListener() {
					public void buildMenu(Menu menu) {
						TagUIUtils.addLibraryViewTagsSubMenu(dms, menu, detailArea);
					}
				});

			// assign cats
		
		createMenuRow(detailArea, "MyTorrentsView.menu.setCategory",
				"image.sidebar.library", new FancyMenuRowInfoListener() {
					public void buildMenu(Menu menu) {
						TorrentUtil.addCategorySubMenu(dms, menu, detailArea);
					}
				});

			// Archive
		
		final List<Download>	ar_dms = new ArrayList<Download>();
		
		for ( DownloadManager dm: dms ){
			
			Download stub = PluginCoreUtils.wrap(dm);
			
			if ( !stub.canStubbify()){
				
				continue;
			}
			
			ar_dms.add( stub );
		}
		
		if ( ar_dms.size() > 0 ){
			
			createRow(
				detailArea, 
				"MyTorrentsView.menu.archive", "image.sidebar.archive",
				new Listener() 
				{
					public void handleEvent(Event event) {
							
						ManagerUtils.moveToArchive( ar_dms, null );
					}
				});
		}
		

			// Advanced - > Rename
		
		createRow(detailArea, "MyTorrentsView.menu.rename", null, new Listener() {
			public void handleEvent(Event event) {
				for (DownloadManager dm : dms) {
					AdvRenameWindow window = new AdvRenameWindow();
					window.open(dm);
				}
			}
		});

			// Reposition
		
		createRow(detailArea, "MyTorrentsView.menu.reposition.manual", null,
				new Listener() {
					public void handleEvent(Event event) {
						TorrentUtil.repositionManual(tv, dms, parentShell, isSeedingView);
					}
				});

			// Filter
		
		if (tv.getSWTFilter() != null) {
			createRow(detailArea, "MyTorrentsView.menu.filter", null, new Listener() {
				public void handleEvent(Event event) {
					tv.openFilterDialog();
				}
			});
		}

	}

	public void buildTorrentCustomMenu_Social(Composite detailArea) {

		boolean isTrackerOn = TRTrackerUtils.isTrackerEnabled();
		int userMode = COConfigurationManager.getIntParameter("User Mode");

		if (hasSelection) {
			createMenuRow(detailArea, "ConfigView.section.interface.alerts", null,
					new FancyMenuRowInfoListener() {
						public void buildMenu(Menu menu) {
							MenuFactory.addAlertsMenu(menu, false, dms);
						}
					});
		}

		if (userMode > 0 && isTrackerOn && hasSelection) {
			// Host
			createRow(detailArea, "MyTorrentsView.menu.host", "host", new Listener() {
				public void handleEvent(Event event) {
					TorrentUtil.hostTorrents(dms);
				}
			});

			// Publish
			createRow(detailArea, "MyTorrentsView.menu.publish", "publish",
					new Listener() {
						public void handleEvent(Event event) {
							TorrentUtil.publishTorrents(dms);
						}
					});
		}

		if (userMode > 0) {
			// Advanced > Export > Export XML
			if (dms.length == 1) {
				String title = MessageText.getString("MyTorrentsView.menu.exportmenu")
						+ ": " + MessageText.getString("MyTorrentsView.menu.export");
				FancyRowInfo row = createRow(detailArea, null, null,
						new ListenerDMTask(dms) {
							public void run(DownloadManager dm) {
								if (dm != null) {
									new ExportTorrentWizard(parentShell.getDisplay(), dm);
								}
							}
						});
				row.getText().setText(title);
			}

			// Advanced > Export > Export Torrent
			String title = MessageText.getString("MyTorrentsView.menu.exportmenu")
					+ ": " + MessageText.getString("MyTorrentsView.menu.exporttorrent");
			FancyRowInfo row = createRow(detailArea, null, null, new ListenerDMTask(
					dms) {
				public void run(DownloadManager[] dms) {
					TorrentUtil.exportTorrent(dms, parentShell);
				}
			});
			row.getText().setText(title);

			// Advanced > Export > WebSeed URL
			createRow(detailArea, "MyTorrentsView.menu.exporthttpseeds", null,
					new ListenerDMTask(dms) {
						public void run(DownloadManager[] dms) {
							TorrentUtil.exportHTTPSeeds(dms);
						}
					});
		}

		// personal share
		if (isSeedingView) {
			createRow(detailArea, "MyTorrentsView.menu.create_personal_share", null,
					new ListenerDMTask(dms, false) {
						public void run(DownloadManager dm) {
							File file = dm.getSaveLocation();

							Map<String, String> properties = new HashMap<String, String>();

							properties.put(ShareManager.PR_PERSONAL, "true");

							if (file.isFile()) {

								ShareUtils.shareFile(file.getAbsolutePath(), properties);

							} else if (file.isDirectory()) {

								ShareUtils.shareDir(file.getAbsolutePath(), properties);
							}
						}
					});
		}

	}

	public void addTableItemsWithID(Composite detailArea, String menuID,
			String[] ids) {

		TableContextMenuItem[] items = TableContextMenuManager.getInstance().getAllAsArray(
				menuID);
		if (DEBUG_MENU) {
			System.out.println("AddItemsWithID " + menuID + ": " + items.length);
		}

		addItemsArray(detailArea, items, ids);

	}

	public void addMenuItemsWithID(Composite detailArea, String menuID,
			String[] ids) {

		org.gudy.azureus2.plugins.ui.menus.MenuItem[] items = MenuItemManager.getInstance().getAllAsArray(
				menuID);
		if (DEBUG_MENU) {
			System.out.println("AddItemsWithID " + menuID + ": " + items.length);
		}

		addItemsArray(detailArea, items, ids);

	}

	public void addItemsArray(final Composite detailArea,
			org.gudy.azureus2.plugins.ui.menus.MenuItem[] items, String[] onlyIDs) {
		int userMode = COConfigurationManager.getIntParameter("User Mode");

		for (int i = 0; i < onlyIDs.length; i++) {
			String id = onlyIDs[i];
			if (DEBUG_MENU) {
				System.out.println(" addItemsArray " + id);
			}
			Integer requiredUserMode = mapMovedPluginMenuUserMode.get(id);
			if (requiredUserMode != null && userMode < requiredUserMode) {
				if (DEBUG_MENU) {
					System.out.println(" skipped, usermode is " + userMode
							+ " but requires " + requiredUserMode);
				}
				continue;
			}

			for (final org.gudy.azureus2.plugins.ui.menus.MenuItem item : items) {
				String key = item.getResourceKey();
				if (!id.equals(key)) {
					continue;
				}

				addPluginItem(detailArea, item);
				if (DEBUG_MENU) {
					System.out.println(" found, added");
				}
				break;
			}
		}
	}

	public void buildTorrentCustomMenu_Other(final Composite detailArea,
			List<org.gudy.azureus2.plugins.ui.menus.MenuItem> items) {

		for (org.gudy.azureus2.plugins.ui.menus.MenuItem item : items) {

			if (DEBUG_MENU) {
				System.out.println(item.getText() + ": " + item.getResourceKey());
			}

			// TableContextMenuItems get rows as datasource.. the rest get DownloadManagers
			addPluginItem(detailArea, item);
		}

	}

	private Object[] getTarget(org.gudy.azureus2.plugins.ui.menus.MenuItem item) {
		if (MenuManager.MENU_TABLE.equals(item.getMenuID())) {
			return tv.getSelectedRows();
		}
		Object[] dataSources = tv.getSelectedDataSources(false);
		Download[] downloads = new Download[dataSources.length];
		System.arraycopy(dataSources, 0, downloads, 0, dataSources.length);
		return downloads;
	}

	private void addPluginItem(Composite detailArea,
			final org.gudy.azureus2.plugins.ui.menus.MenuItem item) {

		// menuWillBeShown listeners might change the visibility, so run before check
		MenuItemImpl menuImpl = ((MenuItemImpl) item);
		menuImpl.invokeMenuWillBeShownListeners(getTarget(item));

		if (!item.isVisible()) {
			if (DEBUG_MENU) {
				System.out.println("Menu Not Visible: " + item.getText() + ": "
						+ item.getMenuID());
			}
			return;
		}

		Graphic graphic = item.getGraphic();
		FancyRowInfo row;

		if (DEBUG_MENU) {
			System.out.println("Menu " + item.getText() + ": " + item.getMenuID());
		}

		if (item.getStyle() == org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_MENU) {

			row = createMenuRow(detailArea, item.getResourceKey(), null,
					new FancyMenuRowInfoListener() {
						public void buildMenu(Menu menu) {
							if (dms.length != 0) {
								MenuBuilder submenuBuilder = ((MenuItemImpl) item).getSubmenuBuilder();
								if (submenuBuilder != null) {
									try {
										item.removeAllChildItems();
										submenuBuilder.buildSubmenu(item, getTarget(item));
									} catch (Throwable t) {
										Debug.out(t);
									}
								}

								MenuBuildUtils.addPluginMenuItems(item.getItems(), menu, false,
										true, new MenuBuildUtils.PluginMenuController() {

											public Listener makeSelectionListener(
													final org.gudy.azureus2.plugins.ui.menus.MenuItem plugin_menu_item) {
												return new TableSelectedRowsListener(tv, false) {
													public boolean run(TableRowCore[] rows) {
														if (rows.length != 0) {
															((MenuItemImpl) plugin_menu_item).invokeListenersMulti(getTarget(item));
														}
														return true;
													}
												};
											}

											public void notifyFillListeners(
													org.gudy.azureus2.plugins.ui.menus.MenuItem menu_item) {
												((MenuItemImpl) menu_item).invokeMenuWillBeShownListeners(getTarget(item));
											}

											// @see org.gudy.azureus2.ui.swt.MenuBuildUtils.PluginMenuController#buildSubmenu(org.gudy.azureus2.plugins.ui.menus.MenuItem)
											public void buildSubmenu(
													org.gudy.azureus2.plugins.ui.menus.MenuItem parent) {
												org.gudy.azureus2.plugins.ui.menus.MenuBuilder submenuBuilder = ((MenuItemImpl) parent).getSubmenuBuilder();
												if (submenuBuilder != null) {
													try {
														parent.removeAllChildItems();
														submenuBuilder.buildSubmenu(parent, getTarget(item));
													} catch (Throwable t) {
														Debug.out(t);
													}
												}
											}
										});
							}
						}

					});
		} else {
			row = createRow(detailArea, item.getResourceKey(), null,
					new TableSelectedRowsListener(tv, false) {

						public boolean run(TableRowCore[] rows) {
							if (rows.length != 0) {
								((MenuItemImpl) item).invokeListenersMulti(getTarget(item));
							}
							return true;
						}

					});
		}

		row.setEnabled(item.isEnabled());
		if (graphic instanceof UISWTGraphic) {
			row.getIconLabel().setImage(((UISWTGraphic) graphic).getImage());
		} else if (graphic instanceof GraphicURI) {
			ImageLoader.getInstance().setLabelImage(row.getIconLabel(),
					((GraphicURI) graphic).getURI().toString());
		}
	}

	protected void buildTorrentCustomMenu_Content(Composite detailArea,
			final DownloadManager[] dms) {

		// Run Data File
		if (hasSelection) {
			createRow(detailArea, "MyTorrentsView.menu.open", "run",
					new ListenerDMTask(dms) {
						public void run(DownloadManager[] dms) {
							TorrentUtil.runDataSources(dms);
						}
					});
		}

		// Explore (or open containing folder)
		if (hasSelection) {
			final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
			createRow(detailArea, "MyTorrentsView.menu."
					+ (use_open_containing_folder ? "open_parent_folder" : "explore"),
					null, new ListenerDMTask(dms, false) {
						public void run(DownloadManager dm) {
							ManagerUtils.open(dm, use_open_containing_folder);
						}
					});
		}

		// Open In Browser
		
		if (hasSelection) {
			createMenuRow(
				detailArea, 
				"MyTorrentsView.menu.browse",
				null, 
				new FancyMenuRowInfoListener() 
				{
					public void 
					buildMenu(
						Menu menuBrowse ) 
					{
						final MenuItem itemBrowsePublic = new MenuItem(menuBrowse, SWT.PUSH);
						itemBrowsePublic.setText( MessageText.getString( "label.public" ) + "..." );
						itemBrowsePublic.addListener(
							SWT.Selection, 
							new ListenerDMTask(dms, false) {
								public void run(DownloadManager dm) {
									ManagerUtils.browse( dm, false, true );
								}
							});
						
						final MenuItem itemBrowseAnon = new MenuItem(menuBrowse, SWT.PUSH);
						itemBrowseAnon.setText( MessageText.getString( "label.anon" ) + "..." );
						itemBrowseAnon.addListener(
							SWT.Selection, 
							new ListenerDMTask(dms, false) {
								public void run(DownloadManager dm) {
									ManagerUtils.browse( dm, true, true );
								}
							});
						
						new MenuItem(menuBrowse, SWT.SEPARATOR);
						
						final MenuItem itemBrowseURL = new MenuItem(menuBrowse, SWT.PUSH);
						Messages.setLanguageText(itemBrowseURL, "label.copy.url.to.clip" );
						itemBrowseURL.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event){
								Utils.getOffOfSWTThread(
									new AERunnable() {
										@Override
										public void runSupport() {
											String url = ManagerUtils.browse(dms[0], true, false );
											if ( url != null ){
												ClipboardCopy.copyToClipBoard( url );
											}
										}
									});
							}});
						
						itemBrowseURL.setEnabled( dms.length == 1 );
						
						new MenuItem(menuBrowse, SWT.SEPARATOR);

						final MenuItem itemBrowseDir = new MenuItem(menuBrowse, SWT.CHECK);
						Messages.setLanguageText(itemBrowseDir, "library.launch.web.in.browser.dir.list");
						itemBrowseDir.setSelection(COConfigurationManager.getBooleanParameter( "Library.LaunchWebsiteInBrowserDirList"));
						itemBrowseDir.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event) {
								COConfigurationManager.setParameter( "Library.LaunchWebsiteInBrowserDirList", itemBrowseDir.getSelection());
							}
						});
					}
				});
		}
		
		// Move Data Files
		boolean fileMove = true;
		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
			if (!dm.canMoveDataFiles()) {
				fileMove = false;
				break;
			}
		}
		if (fileMove) {
			createRow(detailArea, "MyTorrentsView.menu.movedata", null,
					new ListenerDMTask(dms) {
						public void run(DownloadManager[] dms) {
							TorrentUtil.moveDataFiles(parentShell, dms);
						}
					});
		}

		createRow(detailArea, "MyTorrentsView.menu.checkfilesexist", null,
				new ListenerDMTask(dms) {
					public void run(DownloadManager dm) {
						dm.filesExist(true);
					}
				});

		createRow(detailArea, "MyTorrentsView.menu.thisColumn.toClipboard", null,
				new Listener() {
					public void handleEvent(Event event) {
						String sToClipboard = "";
						if (column == null) {
							return;
						}
						String columnName = column.getName();
						if (columnName == null) {
							return;
						}
						TableRowCore[] rows = tv.getSelectedRows();
						for (TableRowCore row : rows) {
							if (row != rows[0]) {
								sToClipboard += "\n";
							}
							TableCellCore cell = row.getTableCellCore(columnName);
							if (cell != null) {
								sToClipboard += cell.getClipboardText();
							}
						}
						if (sToClipboard.length() == 0) {
							return;
						}
						new Clipboard(Display.getDefault()).setContents(new Object[] {
							sToClipboard
						}, new Transfer[] {
							TextTransfer.getInstance()
						});
					}
				});
	}

	private HeaderInfo addHeader(String id, String title, AERunnable runnable) {
		Composite composite = new Composite(topArea, SWT.NONE);

		HeaderInfo headerInfo = new HeaderInfo(id, runnable, composite);

		composite.setBackgroundMode(SWT.INHERIT_FORCE);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginWidth = 6;
		fillLayout.marginHeight = 2;
		composite.setLayout(fillLayout);
		Display d = composite.getDisplay();
		composite.setBackground(d.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		composite.setForeground(d.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		Label control = new Label(composite, SWT.NONE);
		Messages.setLanguageText(control, title);
		control.setData("ID", headerInfo);

		control.addListener(SWT.MouseEnter, headerListener);
		control.addListener(SWT.Touch, headerListener);
		control.addListener(SWT.MouseExit, headerListener);
		control.addListener(SWT.Paint, headerListener);

		listHeaders.add(headerInfo);
		return headerInfo;
	}

}
