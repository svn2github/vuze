package com.aelitis.azureus.ui.swt.mdi;

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.swt.views.IViewAlwaysInitialize;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTabFolder;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class TabbedMDI
	extends BaseMDI
	implements AEDiagnosticsEvidenceGenerator
{
	private CTabFolder tabFolder;

	private LinkedList<MdiEntry>	select_history = new LinkedList<MdiEntry>();
	
	public TabbedMDI() {
		super();
		AEDiagnostics.addEvidenceGenerator(this);
	}

	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		super.skinObjectCreated(skinObject, params);

		creatMDI();

		try {
			UIFunctionsManager.getUIFunctions().getUIUpdater().addUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}

		return null;
	}
	
	// @see com.aelitis.azureus.ui.swt.mdi.BaseMDI#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {

		UIManager ui_manager = PluginInitializer.getDefaultInterface().getUIManager();
		ui_manager.addUIListener(new UIManagerListener() {
			public void UIDetached(UIInstance instance) {
			}
			
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							try {
								loadCloseables();
							} catch (Throwable t) {
								Debug.out(t);
							}

							setupPluginViews();
						}
					});
				}
			}
		});

		return super.skinObjectInitialShow(skinObject, params);
	}

	private void creatMDI() {
		if (soMain instanceof SWTSkinObjectTabFolder) {
			tabFolder = ((SWTSkinObjectTabFolder) soMain).getTabFolder();
		} else {
			tabFolder = new CTabFolder((Composite) soMain.getControl(), SWT.TOP
					| SWT.BORDER | SWT.CLOSE);
		}

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

		tabFolder.setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));

		tabFolder.setMinimumCharacters(25);

		tabFolder.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TabbedEntry entry = (TabbedEntry) event.item.getData("TabbedEntry");				
				showEntry(entry);
			}
		});
		
		tabFolder.addCTabFolder2Listener(
			new CTabFolder2Adapter()
			{
				@Override
				public void 
				close(
					CTabFolderEvent event )
				{
					final TabbedEntry entry = (TabbedEntry) event.item.getData("TabbedEntry");
					
					if ( select_history.remove( entry )){
						
						if ( select_history.size() > 0 ){
							
							MdiEntry next = select_history.getLast();
							
							if ( !next.isDisposed() && next != entry ){
								
								event.doit = false;

								showEntry( next );
								
								Utils.execSWTThreadLater(
									0, 
									new AERunnable() 
									{
										public void 
										runSupport() 
										{
											entry.close( true );
										}
									});
							}
						}
					}
				}
			});
		
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

		tabFolder.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				saveCloseables();
			}
		});
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
		MdiEntry entry = (MdiEntry) item.getData("TabbedEntry");

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
		
		select_history.remove( newEntry );
		
		select_history.add( newEntry );
			
		if ( select_history.size() > 64 ){
			
			select_history.removeFirst();
		}
		
		MdiEntry oldEntry = currentEntry;
		if (newEntry == oldEntry) {
			triggerSelectionListener(newEntry, newEntry);
			return;
		}

		if (oldEntry != null) {
			oldEntry.hide();
		}

		currentEntry = (MdiEntrySWT) newEntry; // assumed MdiEntrySWT

		((BaseMdiEntry) newEntry).show();

		triggerSelectionListener(newEntry, oldEntry);
	}

	public void updateUI() {
		MdiEntry currentEntry = getCurrentEntry();
		if (currentEntry != null) {
			currentEntry.updateUI();
		}
	}
	
	private MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, int index) {
		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		TabbedEntry entry = new TabbedEntry(this, skin, id);
		entry.setTitle(title);
		entry.setSkinRef(configID, params);
		setupNewEntry(entry, id, index);
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

	public MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource, String preferredAfterID) {
		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		TabbedEntry entry = new TabbedEntry(this, skin, id);

		entry.setDatasource(datasource);
		entry.setPreferredAfterID(preferredAfterID);
		entry.setEventListener(l);

		setupNewEntry(entry, id, -1);

		if (l instanceof IViewAlwaysInitialize) {
			entry.build();
		}

		return entry;
	}

	public MdiEntry createEntryFromView(String parentID, UISWTViewCore view,
			String id, Object datasource, boolean closeable, boolean show,
			boolean expand) {
		if (id == null) {
			id = view.getClass().getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			if (show) {
				showEntry(oldEntry);
			}
			return oldEntry;
		}

		TabbedEntry entry = new TabbedEntry(this, skin, id);

		entry.setCoreView(view);
		entry.setDatasource(datasource);

		setupNewEntry(entry, id, -1);

		if (view instanceof IViewAlwaysInitialize) {
			entry.build();
		}
		
		if (show) {
			showEntry(entry);
		}
		return entry;
	}

	private void setupNewEntry(final TabbedEntry entry, final String id,
			final int index) {
		synchronized (mapIdToEntry) {
			mapIdToEntry.put(id, entry);
		}

		entry.setCloseable(true);

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				swt_setupNewEntry(entry, id, index);
			}
		});
	}

	private void swt_setupNewEntry(TabbedEntry entry, String id, int index) {
		if (index < 0 || index >= tabFolder.getItemCount()) {
			index = tabFolder.getItemCount();
		}
		CTabItem cTabItem = new CTabItem(tabFolder, SWT.CLOSE, index);
		cTabItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (tabFolder.getItemCount() == 0) {
					currentEntry = null;
				}
			}
		});
		cTabItem.setData("TabbedEntry", entry);
		entry.setSwtItem(cTabItem);
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

			
			UISWTViewCore view = entry.getCoreView();
			if (!(view instanceof AEDiagnosticsEvidenceGenerator)) {
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
}
