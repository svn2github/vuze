/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core.DownloadManager;

/**
 * @author Olivier
 * 
 */
public class ManagerView implements IView {

  DownloadManager manager;
  CTabFolder folder;
  IView viewGeneral;
  IView viewDetails;
  IView viewPieces;

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
    int completed = manager.getCompleted();
    return (completed / 10) + "." + (completed % 10) + "% : " + manager.getName();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    folder = new CTabFolder(composite, SWT.TOP | SWT.FLAT);
    folder.setSelectionBackground(new Color[] { MainWindow.white }, new int[0]);
    CTabItem itemGeneral = new CTabItem(folder, SWT.NULL);
    CTabItem itemDetails = new CTabItem(folder, SWT.NULL);
    CTabItem itemPieces = new CTabItem(folder, SWT.NULL);
    viewGeneral = new GeneralView(manager);
    viewGeneral.initialize(folder);
    viewDetails = new PeersView(manager);
    viewDetails.initialize(folder);
    viewPieces = new PiecesView(manager);
    viewPieces.initialize(folder);
    itemGeneral.setText(viewGeneral.getShortTitle());
    itemGeneral.setControl(viewGeneral.getComposite());
    itemDetails.setText(viewDetails.getShortTitle());
    itemDetails.setControl(viewDetails.getComposite());
    itemPieces.setText(viewPieces.getShortTitle());
    itemPieces.setControl(viewPieces.getComposite());
    folder.setSelection(itemGeneral);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    int selected = folder.getSelectionIndex();
    switch (selected) {
      case 0 :
        viewGeneral.refresh();
        break;
      case 1 :
        viewDetails.refresh();
        break;
      case 2 :
        viewPieces.refresh();
        break;
    }
  }

}
