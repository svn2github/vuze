/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class ManagerView extends AbstractIView {

  DownloadManager manager;
  TabFolder folder;
  TabItem itemGeneral;
  TabItem itemDetails;
  TabItem itemPieces;
  TabItem itemFiles;
  /*
  CTabFolder folder;
  CTabItem itemGeneral;
  CTabItem itemDetails;
  CTabItem itemPieces;
  CTabItem itemFiles;
  */
  IView viewGeneral;
  IView viewDetails;
  IView viewPieces;
  IView viewFiles;

  public ManagerView(DownloadManager manager) {
    this.manager = manager;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().removeManagerView(manager);
    if (viewGeneral != null)
      viewGeneral.delete();
    if (viewDetails != null)
      viewDetails.delete();
    if (viewPieces != null)
      viewPieces.delete();
    if (viewFiles != null)
      viewFiles.delete();
    if (folder != null)
      folder.dispose();
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
    return manager.getName();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    int completed = manager.getStats().getCompleted();
    return (completed / 10) + "." + (completed % 10) + "% : " + manager.getName();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
  	if (folder == null) {
      folder = new TabFolder(composite, SWT.LEFT);
      folder.setBackground(MainWindow.getWindow().getBackground());
    	//folder = new CTabFolder(composite, SWT.TOP | SWT.FLAT);
    	//folder.setSelectionBackground(new Color[] { MainWindow.white }, new int[0]);
  	}
    itemGeneral = new TabItem(folder, SWT.NULL);
    itemDetails = new TabItem(folder, SWT.NULL);
    itemPieces  = new TabItem(folder, SWT.NULL);
    itemFiles   = new TabItem(folder, SWT.NULL);
    /*
    itemGeneral = new CTabItem(folder, SWT.NULL);
    itemDetails = new CTabItem(folder, SWT.NULL);
    itemPieces = new CTabItem(folder, SWT.NULL);
    itemFiles = new CTabItem(folder, SWT.NULL);*/
    viewGeneral = new GeneralView(manager);
    viewGeneral.initialize(folder);
    viewDetails = new PeersView(manager);
    viewDetails.initialize(folder);
    viewPieces = new PiecesView(manager);
    viewPieces.initialize(folder);
    viewFiles = new FilesView(manager);
    viewFiles.initialize(folder);
    Messages.setLanguageText(itemGeneral, viewGeneral.getData());
    itemGeneral.setControl(viewGeneral.getComposite());
    Messages.setLanguageText(itemDetails, viewDetails.getData());
    itemDetails.setControl(viewDetails.getComposite());
    Messages.setLanguageText(itemPieces, viewPieces.getData());
    itemPieces.setControl(viewPieces.getComposite());
    Messages.setLanguageText(itemFiles, viewFiles.getData());
    itemFiles.setControl(viewFiles.getComposite());
    TabItem items[] = {itemGeneral};
    folder.setSelection(items);
    manager.addListener((PiecesView)viewPieces);
    manager.addListener((PeersView)viewDetails);
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
    }
  }

}
