/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

/**
 * @author Olivier
 * 
 */
public class ManagerView extends AbstractIView implements DownloadManagerListener {

  AzureusCore		azureus_core;
  DownloadManager 	manager;
  TabFolder folder;
  
  TabItem itemGeneral;
  TabItem itemDetails;
  TabItem itemGraphic;
  TabItem itemPieces;
  TabItem itemFiles;
  

  IView viewGeneral;
  IView viewDetails;
  IView viewGraphic;
  IView viewPieces;
  IView viewFiles;

  public 
  ManagerView(
  	AzureusCore		_azureus_core,
	DownloadManager manager) 
  {
  	azureus_core	= _azureus_core;
    this.manager 	= manager;
    
    manager.addListener(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().removeManagerView(manager);
    manager.removeListener(this);
    
    if ( !folder.isDisposed()){
    	
    	folder.setSelection(0);
    }
    
    //Don't ask me why, but without this an exception is thrown further
    // (in folder.dispose() )
    //TODO : Investigate to see if it's a platform (OSX-Carbon) BUG, and report to SWT team.
    if(Constants.isOSX) {
      if(folder != null && !folder.isDisposed()) {
        TabItem[] items = folder.getItems();
        for(int i=0 ; i < items.length ; i++) {
          if (!items[i].isDisposed())
            items[i].dispose();
        }
      }
    }
    
    if (viewGeneral != null)
      viewGeneral.delete();
    if (viewDetails != null)
      viewDetails.delete();
    if(viewGraphic != null)
      viewGraphic.delete();
    if (viewPieces != null)
      viewPieces.delete();
    if (viewFiles != null)
      viewFiles.delete();
    if (folder != null && !folder.isDisposed()) {
      folder.dispose();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return folder;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    int completed = manager.getStats().getCompleted();
    return DisplayFormatters.formatPercentFromThousands(completed) + " : " + manager.getDisplayName();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    
	  	if (folder == null) {
	    folder = new TabFolder(composite, SWT.LEFT);
	    folder.setBackground(Colors.background);
	  	} else {
	  	  System.out.println("ManagerView::initialize : folder isn't null !!!");
	  	}
	  	
    itemGeneral = new TabItem(folder, SWT.NULL);
    itemDetails = new TabItem(folder, SWT.NULL);
    itemGraphic = new TabItem(folder, SWT.NULL);
    itemPieces  = new TabItem(folder, SWT.NULL);
    itemFiles   = new TabItem(folder, SWT.NULL);

    viewGeneral = new GeneralView(manager);
    viewDetails = new PeersView(manager);
    viewGraphic = new PeersGraphicView(manager);
    viewPieces = new PiecesView(manager);
    viewFiles = new FilesView(manager);
    
    Messages.setLanguageText(itemGeneral, viewGeneral.getData());
    Messages.setLanguageText(itemDetails, viewDetails.getData());
    Messages.setLanguageText(itemGraphic, viewGraphic.getData());
    Messages.setLanguageText(itemPieces, viewPieces.getData());
    Messages.setLanguageText(itemFiles, viewFiles.getData());
    
    TabItem items[] = {itemGeneral};
    folder.setSelection(items);
    viewGeneral.initialize(folder);
    itemGeneral.setControl(viewGeneral.getComposite());
    viewDetails.initialize(folder);
    itemDetails.setControl(viewDetails.getComposite());
    viewGraphic.initialize(folder);
    itemGraphic.setControl(viewGraphic.getComposite());
    viewPieces.initialize(folder);
    itemPieces.setControl(viewPieces.getComposite());
    viewFiles.initialize(folder);
    itemFiles.setControl(viewFiles.getComposite());

    
    folder.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        refresh();
      }
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });
    
    
    refresh();
    viewGeneral.getComposite().layout(true);

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;

    try {
      switch (folder.getSelectionIndex()) {
        case 0 :
          if (viewGeneral != null && !itemGeneral.isDisposed())
            viewGeneral.refresh();
          break;
        case 1 :
        if (viewDetails != null && !itemDetails.isDisposed())
          viewDetails.refresh();
          break;
        case 2 :
          if (viewGraphic != null && !itemGraphic.isDisposed())
            viewGraphic.refresh();
            break;  
        case 3 :
        //case 2 :
        if (viewPieces != null && !itemPieces.isDisposed())
          viewPieces.refresh();
          break;
        case 4 :
        //case 3 :
        if (viewFiles != null && !itemFiles.isDisposed())
          viewFiles.refresh();
          break;
      }
    } catch (Exception e) {
    	Debug.printStackTrace( e );
    }
  }
  
  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("run"))
      return true;
    if(itemKey.equals("start"))
      return ManagerUtils.isStartable(manager);
    if(itemKey.equals("stop"))
      return ManagerUtils.isStopable(manager);
    if(itemKey.equals("host"))
      return true;
    if(itemKey.equals("publish"))
      return true;
    if(itemKey.equals("remove"))
      return true;
    return false;
  }
  
  public void itemActivated(String itemKey) {
	  if(itemKey.equals("run")) {
	    ManagerUtils.run(manager);
	    return;
	  }
	  if(itemKey.equals("start")) {
	    ManagerUtils.queue(manager,folder);
	    return;
	  }
	  if(itemKey.equals("stop")) {
	    ManagerUtils.stop(manager,folder);
	    return;
	  }
	  if(itemKey.equals("host")) {
	    ManagerUtils.host(azureus_core, manager,folder);
	    MainWindow.getWindow().showMyTracker();
	    return;
	  }
	  if(itemKey.equals("publish")) {
	    ManagerUtils.publish(azureus_core, manager,folder);
	    MainWindow.getWindow().showMyTracker();
	    return;
	  }
	  if(itemKey.equals("remove")) {
	  
        
        if( COConfigurationManager.getBooleanParameter( "confirm_torrent_removal" ) ) {
          MessageBox mb = new MessageBox(folder.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
          mb.setText(MessageText.getString("deletedata.title"));
          mb.setMessage(MessageText.getString("MyTorrentsView.confirm_torrent_removal") + manager.getDisplayName() );
          if( mb.open() == SWT.NO ) {
            return;
          }
        }
        
       	new AEThread( "asyncStop", true )
			{
        		public void
				runSupport()
        		{
        			try{
        		        
				        manager.stopIt( DownloadManager.STATE_STOPPED, false, false );
				        
				        manager.getGlobalManager().removeDownloadManager( manager );
					  		
        			}catch( GlobalManagerDownloadRemovalVetoException e ){
					  		
        				Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", e );
					}
        		}
			}.start();
	  }
  }
  
  
  public void downloadComplete(DownloadManager manager) {   
  }

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
  }

  public void stateChanged(DownloadManager manager, int state) {
    if(folder == null || folder.isDisposed())
      return;    
    Display display = folder.getDisplay();
    if(display == null || display.isDisposed())
      return;
    display.asyncExec(new AERunnable() {
	    public void runSupport() {
	      MainWindow.getWindow().refreshIconBar();  
	    }
    });    
  }

  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  }
}
