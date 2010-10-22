/*
 * Created on 2 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * Torrent download view, consisting of several information tabs
 * 
 * @author Olivier
 * 
 */
public class ManagerView
	implements DownloadManagerListener, ObfusticateTab, ObfusticateImage,
	ViewTitleInfo, UISWTViewCoreEventListener, ToolBarEnabler, UIUpdatable
{

  private DownloadManager 	manager;
  private CTabFolder folder;
  private ArrayList tabViews = new ArrayList();
  
  int lastCompleted = -1;
	private UISWTView swtView;
	private GlobalManagerAdapter gmListener;
	private Composite parent;
	protected IView activeView;
  
  /**
	 * 
	 */
	public ManagerView() {
		// assumed if we are opening a Download Manager View that we
		// have a DownloadManager and thus an AzureusCore
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		gmListener = new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				if (dm.equals(manager)) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							delete();
						}
					});
				}
			}
		};
		gm.addListener(gmListener, false);
		
		UIFunctionsManagerSWT.getUIFunctionsSWT().getUIUpdater().addUpdater(this);
	}
  
  private void dataSourceChanged(Object newDataSource) {
    if (manager != null) {
    	manager.removeListener(this);
    }

  	DownloadImpl dataSourcePlugin = null;
  	if (newDataSource instanceof DownloadImpl) {
  		dataSourcePlugin = (DownloadImpl) newDataSource;
  		manager = dataSourcePlugin.getDownload();
  	} else if (newDataSource instanceof DownloadManager) {
    	manager = (DownloadManager) newDataSource;
      try {
      	dataSourcePlugin = DownloadManagerImpl.getDownloadStatic(manager);
      } catch (DownloadException e) { /* Ignore */ }
  	} else {
  		manager = null;
  	}
  	
    if (manager != null) {
    	manager.addListener(this);
    }

		for (int i = 0; i < tabViews.size(); i++) {
			IView view = (IView) tabViews.get(i);
			if (view != null) {
				if (view instanceof UISWTViewImpl) {
					((UISWTViewImpl) view).dataSourceChanged(dataSourcePlugin);
				} else {
					view.dataSourceChanged(newDataSource);
				}
			}
		}
		
		refreshTitle();		
		ViewTitleInfoManager.refreshTitleInfo(this);
  }

  private void delete() {
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.removeManagerView(manager);
  		uiFunctions.getUIUpdater().removeUpdater(this);
  	}
  	if (manager != null) {
  		manager.removeListener(this);
  	}
    
  	try {
  		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
  		gm.removeListener(gmListener);
  	} catch (Exception e) {
  		Debug.out(e);
  	}

    if (folder != null && !folder.isDisposed()){
    	
    	folder.setSelection(0);
    }
    
    //Don't ask me why, but without this an exception is thrown further
    // (in folder.dispose() )
    //TODO : Investigate to see if it's a platform (OSX-Carbon) BUG, and report to SWT team.
    if(Utils.isCarbon) {
      if(folder != null && !folder.isDisposed()) {
        CTabItem[] items = folder.getItems();
        for(int i=0 ; i < items.length ; i++) {
          if (!items[i].isDisposed())
            items[i].dispose();
        }
      }
    }

    for (int i = 0; i < tabViews.size(); i++) {
    	IView view = (IView) tabViews.get(i);
    	try {
      	if (view != null) {
      		view.delete();
      	}
    	} catch (Throwable t) {
    		Debug.out(t);
    	}
    }
    tabViews.clear();

    Utils.disposeSWTObjects(new Object[] { folder, parent });
  }

  private void initialize(Composite composite) {

  	this.parent = composite;
		if (folder == null) {
			folder = new CTabFolder(composite, SWT.LEFT);
			folder.setBorderVisible(true);
		} else {
			System.out.println("ManagerView::initialize : folder isn't null !!!");
		}
  	if (composite.getLayout() instanceof FormLayout) {
  		folder.setLayoutData(Utils.getFilledFormData());
  	} else if (composite.getLayout() instanceof GridLayout) {
  		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
  	}
  	
  	Label lblClose = new Label(folder, SWT.WRAP);
  	lblClose.setText("x");
  	lblClose.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				delete();
			}
		});
  	folder.setTopRight(lblClose);
  	folder.setTabHeight(20);
  	
  	ArrayList iviews_to_use = new ArrayList();
  	iviews_to_use.add(new GeneralView());
  	iviews_to_use.add(new TrackerView());
  	iviews_to_use.add(new PeersView());
  	iviews_to_use.add(new PeersGraphicView());
  	iviews_to_use.add(new PiecesView());
  	iviews_to_use.add(new FilesView());
  	iviews_to_use.add(new TorrentInfoView());
  	iviews_to_use.add(new TorrentOptionsView());
  	if (Logger.isEnabled()) {
  		iviews_to_use.add(new LoggerView(true));
  	}
  	
  	final IView[] views = (IView[])iviews_to_use.toArray(new IView[iviews_to_use.size()]);

  	for (int i = 0; i < views.length; i++)
		addSection(views[i], manager);


    // Call plugin listeners
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstanceImpl pluginUI = uiFunctions.getSWTPluginInstanceImpl();
			Map pluginViews = pluginUI == null ? null
					: pluginUI.getViewListeners(UISWTInstance.VIEW_MYTORRENTS);
			if (pluginViews != null) {
				String[] sNames = (String[]) pluginViews.keySet().toArray(new String[0]);
				for (int i = 0; i < sNames.length; i++) {
					UISWTViewEventListener l = (UISWTViewEventListener) pluginViews.get(sNames[i]);
					if (l != null) {
						try {
							UISWTViewImpl view = new UISWTViewImpl(
									UISWTInstance.VIEW_MYTORRENTS, sNames[i], l);
							addSection(view);
						} catch (Exception e) {
							// skip
						}
					}
				}
			}
		}
		

    // Initialize view when user selects it
    folder.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
      	folder.getShell().setCursor(e.display.getSystemCursor(SWT.CURSOR_WAIT));
      	try {
        	// Send one last refresh to previous tab, just in case it
        	// wants to do something when view goes invisible
          refresh();
  
          CTabItem item = (CTabItem)e.item;
          if (item != null) {
          	IView view = (IView)item.getData("IView");
            activeView = view;
          	 
          	if (item.getControl() == null) {
            	view.initialize(folder);
            	item.setControl(view.getComposite());
          	}
          	
          	item.getControl().setFocus();

      	    UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
      			if (uiFunctions != null) {
      				uiFunctions.refreshIconBar(); // For edit columns view
      			}
          }
          refresh();
      		ViewTitleInfoManager.refreshTitleInfo(ManagerView.this);
      	} finally {
      		folder.getShell().setCursor(null);
      	}
      }
    });
    
    Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				views[0].initialize(folder);
				folder.getItem(0).setControl(views[0].getComposite());
				views[0].refresh();
				views[0].getComposite().layout(true);
				views[0].getComposite().setFocus();
				activeView = views[0];
				folder.setSelection(0);
				ViewTitleInfoManager.refreshTitleInfo(ManagerView.this);
			}
		});
  }
  
  private IView getActiveView() {
  	return activeView;
  }

  /**
   * Called when view is visible
   */
  private void refresh() {
		if (folder == null || folder.isDisposed())
			return;

		try {
			IView view = getActiveView();
			if (view != null)
				view.refresh();

			CTabItem[] items = folder.getItems();
			
	    for (int i = 0; i < items.length; i++) {
	    	CTabItem item = items[i];
	    	view = (IView) item.getData("IView");
        try {
          if (item.isDisposed())
            continue;
          String lastTitle = item.getText();
          String newTitle = view.getShortTitle();
          if (lastTitle == null || !lastTitle.equals(newTitle)) {
            item.setText(escapeAccelerators(newTitle));
          }
          String lastToolTip = item.getToolTipText();
          String newToolTip = view.getFullTitle();
          if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
            item.setToolTipText(newToolTip);
          }
        }
        catch (Exception e){
        	Debug.printStackTrace(e);
        }
      }
	    
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
	}

  /**
	 * 
	 *
	 * @since 3.1.0.1
	 */
	private void refreshTitle() {
		if (swtView != null) {
			int completed = manager == null ? -1
					: manager.getStats().getCompleted();
			if (lastCompleted != completed) {
				ViewTitleInfoManager.refreshTitleInfo(this);
				lastCompleted = completed;
			}
		}
	}

	protected static String
  escapeAccelerators(
	 String	str )
  {
	  if ( str == null ){
		  
		  return( str );
	  }
	  
	  return( str.replaceAll( "&", "&&" ));
  }
  
	public void refreshToolBar(Map<String, Boolean> list) {
		IView active_view = getActiveView();
		if (active_view instanceof ToolBarEnabler) {
			((ToolBarEnabler) active_view).refreshToolBar(list);
			return;
		}

		list.put("run", true);
		list.put("start", ManagerUtils.isStartable(manager));
		list.put("stop", ManagerUtils.isStopable(manager));
		list.put("remove", true);
	}

	public boolean toolBarItemActivated(String itemKey) {
		IView active_view = getActiveView();
		if (active_view instanceof ToolBarEnabler) {
			return ((ToolBarEnabler) active_view).toolBarItemActivated(itemKey);
		}

		if (itemKey.equals("run")) {
			ManagerUtils.run(manager);
			return true;
		}
		
		if (itemKey.equals("start")) {
			ManagerUtils.queue(manager, folder.getShell());
			return true;
		}
		
		if (itemKey.equals("stop")) {
			ManagerUtils.stop(manager, folder.getShell());
			return true;
		}
		
		if (itemKey.equals("remove")) {
			TorrentUtil.removeDownloads(new DownloadManager[] {
				manager
			}, null);
			return true;
		}
		
		if (itemKey.equals("editcolumns")) {
			if (active_view != null) {
				active_view.itemActivated(itemKey);
				return true;
			}
		}
		
		return false;
	}
  
  
  public void downloadComplete(DownloadManager manager) {   
  }

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
  }

  public void
  filePriorityChanged( DownloadManager download, org.gudy.azureus2.core3.disk.DiskManagerFileInfo file )
  {	  
  }
  
  public void stateChanged(DownloadManager manager, int state) {
    if(folder == null || folder.isDisposed())
      return;    
    Display display = folder.getDisplay();
    if(display == null || display.isDisposed())
      return;
    Utils.execSWTThread(new AERunnable() {
	    public void runSupport() {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
	    }
    });    
  }

  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  }

	public void addSection(UISWTViewImpl view) {
		Object pluginDataSource = null;
		try {
			pluginDataSource = DownloadManagerImpl.getDownloadStatic(manager);
		} catch (DownloadException e) { 
			/* Ignore */
		}
		addSection(view, pluginDataSource);
	}
	
	private void addSection(IView view, Object dataSource) {
		if (view == null)
			return;

		view.dataSourceChanged(dataSource);

		CTabItem item = new CTabItem(folder, SWT.NULL);
		Messages.setLanguageText(item, view.getData());
		item.setData("IView", view);
		tabViews.add(view);
	}

	public Image obfusticatedImage(Image image) {
		IView view = getActiveView();
		if (view instanceof ObfusticateImage) {
			try {
				((ObfusticateImage)view).obfusticatedImage(image);
			} catch (Exception e) {
				Debug.out("Obfusticating " + view, e);
			}
		}
		return image;
	}

	public String getObfusticatedHeader() {
    int completed = manager.getStats().getCompleted();
    return DisplayFormatters.formatPercentFromThousands(completed) + " : " + manager;
	}
	
	public DownloadManager getDownload() {return manager;}

	// @see com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo#getTitleInfoProperty(int)
	public Object getTitleInfoProperty(int propertyID) {
		if (propertyID == TITLE_TEXT) {
			if (Utils.isAZ2UI()) {
				if (manager == null) {
					return null;
				}
		    int completed = manager.getStats().getCompleted();
				swtView.setTitle(DisplayFormatters.formatPercentFromThousands(completed)
						+ " : " + manager.getDisplayName());
			} else {
				return manager == null ? "" : manager.getDisplayName();
			}
		}

		if (manager == null) {
			return null;
		}
		if (propertyID == TITLE_INDICATOR_TEXT && !Utils.isAZ2UI()) {
	    int completed = manager.getStats().getCompleted();
	    if (completed != 1000) {
	    	return (completed / 10) + "%";
	    }
		} else if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
			String s = "";
	    int completed = manager.getStats().getCompleted();
	    if (completed != 1000) {
	    	s = (completed / 10) + "% Complete\n";
	    }
	    String eta	= DisplayFormatters.formatETA(manager.getStats().getETA());
	    if (eta.length() > 0) {
	    	s += MessageText.getString("TableColumn.header.eta") + ": " + eta + "\n";
	    }
	    
	    return s;
		} else if (propertyID == TITLE_LOGID) {
			String id;
			if (activeView instanceof UISWTViewImpl) {
				id = "" + ((UISWTViewImpl)activeView).getViewID();
		    id = id.substring(id.lastIndexOf(".")+1);
			} else if (activeView != null) {
		    String simpleName = activeView.getClass().getName();
		    id = simpleName.substring(simpleName.lastIndexOf(".")+1);
			} else {
				id = "??";
			}
			return "DMDetails-" + id;
		} else if (propertyID == TITLE_IMAGEID) {
			return "image.sidebar.details";
		}
		return null;
	}
	
	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = (UISWTView)event.getData();
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(folder);
        break;

      case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
      	dataSourceChanged(event.getData());
        break;

        
      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
    }

    return true;
  }

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		return false;
	}
	
	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "DMDetails";
	}
	
	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		refreshTitle();
	}
}
