package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.custom.SashForm;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.config.COConfigurationManager;


public class MyTorrentsSuperView extends AbstractIView  {
  private GlobalManager globalManager;
  private MyTorrentsView torrentview;
  private MyTorrentsView seedingview;
  private SashForm form;

  public MyTorrentsSuperView(GlobalManager globalManager) {
    this.globalManager = globalManager;
  }

  public Composite getComposite() {
    return form;
  }
  
  public void delete() {
    // this doesn't always get called :(
    // XXX need a better place
    COConfigurationManager.setParameter("MyTorrents.SplitAt", form.getWeights()[0]);
    MainWindow.getWindow().setMytorrents(null);
    super.delete();
  }

  public void initialize(Composite composite0) {
    if (form != null) {      
      return;
    }

    GridData gridData;
    form = new SashForm(composite0,SWT.VERTICAL);
    gridData = new GridData(GridData.FILL_BOTH); 
    form.setLayoutData(gridData);

    Composite child1 = new Composite(form,SWT.NULL);
    child1.setLayout(new FillLayout());
    torrentview = new MyTorrentsView(globalManager, false);
    torrentview.initialize(child1);

    Composite child2 = new Composite(form,SWT.NULL);
    child2.setLayout(new FillLayout());
    seedingview = new MyTorrentsView(globalManager, true);
    seedingview.initialize(child2);
    int weight = COConfigurationManager.getIntParameter("MyTorrents.SplitAt", 30);
    if (weight > 100)
      weight = 100;
    form.setWeights(new int[] {weight,100 - weight});
  }

  public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;

    seedingview.refresh();
    torrentview.refresh();
  }

  public String getFullTitle() {
    return MessageText.getString("MyTorrentsView.mytorrents");
  }
  
  // XXX: Is there an easier way to find out what has the focus?
  private IView getCurrentView() {
    // wrap in a try, since the controls may be disposed
    try {
      if (torrentview.getTable().isFocusControl())
        return torrentview;
      else if (seedingview.getTable().isFocusControl())
        return seedingview;
    } catch (Exception ignore) {/*ignore*/}

    return null;
  }

  public boolean isEnabled(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      return currentView.isEnabled(itemKey);
    else
      return false;
  }
  
  public void itemActivated(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      currentView.itemActivated(itemKey);    
  }

}