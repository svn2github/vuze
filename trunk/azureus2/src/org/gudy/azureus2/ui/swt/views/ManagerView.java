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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.util.Constants;
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

  DownloadManager manager;
  TabFolder folder;
  
  TabItem itemGeneral;
  TabItem itemDetails;
  TabItem itemPieces;
  TabItem itemFiles;

  IView viewGeneral;
  IView viewDetails;
  IView viewPieces;
  IView viewFiles;

  public ManagerView(DownloadManager manager) {
    this.manager = manager;
    manager.addListener(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().removeManagerView(manager);
    manager.removeListener(this);
    
    
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
    return String.valueOf(completed / 10).concat(".").concat(String.valueOf(completed % 10)).concat("% : ").concat(manager.getName());
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
    itemPieces  = new TabItem(folder, SWT.NULL);
    itemFiles   = new TabItem(folder, SWT.NULL);

    viewGeneral = new GeneralView(manager);
    viewDetails = new PeersView(manager);
    viewPieces = new PiecesView(manager);
    viewFiles = new FilesView(manager);
    
    Messages.setLanguageText(itemGeneral, viewGeneral.getData());
    Messages.setLanguageText(itemDetails, viewDetails.getData());
    Messages.setLanguageText(itemPieces, viewPieces.getData());
    Messages.setLanguageText(itemFiles, viewFiles.getData());
    
    TabItem items[] = {itemGeneral};
    folder.setSelection(items);
    viewGeneral.initialize(folder);
    itemGeneral.setControl(viewGeneral.getComposite());
    viewDetails.initialize(folder);
    itemDetails.setControl(viewDetails.getComposite());
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
        if (viewPieces != null && !itemPieces.isDisposed())
          viewPieces.refresh();
          break;
        case 3 :
        if (viewFiles != null && !itemFiles.isDisposed())
          viewFiles.refresh();
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
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
      return ManagerUtils.isRemoveable(manager);
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
	    ManagerUtils.host(manager,folder);
	    MainWindow.getWindow().showMyTracker();
	    return;
	  }
	  if(itemKey.equals("publish")) {
	    ManagerUtils.publish(manager,folder);
	    MainWindow.getWindow().showMyTracker();
	    return;
	  }
	  if(itemKey.equals("remove")) {
	  	try{
	  		ManagerUtils.remove(manager);
	  		
	  	}catch( GlobalManagerDownloadRemovalVetoException e ){
	  		
	  		Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", e );
	  	}
	  	
	    return;
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
    display.asyncExec(new Runnable() {
	    public void run() {
	      MainWindow.getWindow().refreshIconBar();  
	    }
    });    
  }

  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  }
}
