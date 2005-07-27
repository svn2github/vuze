/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.MyTorrentsSuperView;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.eclipse.swt.widgets.ProgressBar;

import java.util.Vector;
/**
 * @author Olivier
 * 
 */
public class MinimizedWindow {

  Shell splash;
  Label lDrag;

  MinimizedWindow stucked;

  private Rectangle screen;

  private static final Vector downloadBars = new Vector();
  private static final ShellManager shellManager = new ShellManager();

  private int xPressed, yPressed;
  private boolean moving;

  private int hSize;

  
  
  private Label splashFile;
  private Label splashDown;
  private Label splashUp;
  private Label splashTime;

  public ProgressBar pb1;
  
  public GC gc;
  
  private DownloadManager manager;

  public MinimizedWindow(DownloadManager _manager, Shell main) {
    
	manager = _manager;
	
    this.stucked = null;

    //   The splash Screen setup
    splash = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(SWT.ON_TOP);
    shellManager.addWindow(splash);
    main.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent event) {
            close();
        }
    });
    
    this.screen = main.getDisplay().getClientArea();
   
    lDrag = new Label(splash, SWT.NULL);
    if(! Constants.isOSX) {
      lDrag.setImage(ImageRepository.getImage("dragger")); //$NON-NLS-1$
    }
    lDrag.pack();
    int hSizeImage = lDrag.getSize().y;
    int xSize = lDrag.getSize().x + 3;
    lDrag.setLocation(0, 0);

    MouseListener mListener = new MouseAdapter() {
      public void mouseDown(MouseEvent e) {
        xPressed = e.x;
        yPressed = e.y;
        moving = true;
        //System.out.println("Position : " + xPressed + " , " + yPressed);          
      }

      public void mouseUp(MouseEvent e) {
        moving = false;
      }

    };
    MouseMoveListener mMoveListener = new MouseMoveListener() {
      public void mouseMove(MouseEvent e) {
        if (moving) {
          int dX = xPressed - e.x;
          int dY = yPressed - e.y;
          //System.out.println("dX,dY : " + dX + " , " + dY);
          Point currentLoc = splash.getLocation();
          currentLoc.x -= dX;
          currentLoc.y -= dY;
          setSnapLocation(currentLoc);
          //System.out.println("Position : " + xPressed + " , " + yPressed);
        }
      }
    };

    splash.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    splash.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
    splash.addMouseListener(mListener);
    splash.addMouseMoveListener(mMoveListener);
    lDrag.addMouseListener(mListener);
    lDrag.addMouseMoveListener(mMoveListener);

    Label l1 = new Label(splash, SWT.NONE);
    l1.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    l1.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
    Messages.setLanguageText(l1, "MinimizedWindow.name"); //$NON-NLS-1$
    l1.addMouseListener(mListener);
    l1.addMouseMoveListener(mMoveListener);
    l1.pack();
    l1.setLocation(xSize, 0);
    xSize += l1.getSize().x + 3;

    int hSizeText = l1.getSize().y;
    hSize = hSizeText > hSizeImage ? hSizeText : hSizeImage;

    splashFile = new Label(splash, SWT.NONE);
    splashFile.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    splashFile.setText(""); //$NON-NLS-1$
    splashFile.addMouseListener(mListener);
    splashFile.addMouseMoveListener(mMoveListener);
    splashFile.setSize(200, hSize);
    splashFile.setLocation(xSize, 0);
    xSize += 200 + 3;
    
    pb1 = new ProgressBar(splash ,SWT.SMOOTH);
    pb1.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    pb1.setForeground(Colors.blues[Colors.BLUES_MIDLIGHT]);
    pb1.setMinimum(0);
    pb1.setMaximum(1000);
    pb1.addMouseListener(mListener);
    pb1.addMouseMoveListener(mMoveListener);
    pb1.setSize(100, hSize);
    pb1.setLocation(xSize, 0);
    xSize += 100 + 5;
    
    Listener pb_listener = new Listener() {
        public void handleEvent(Event event) {
          int perc = manager.getStats().getCompleted();
          Color old = event.gc.getForeground(); 
          event.gc.setForeground(Colors.black);
          
          /*
          FontData[] fd = splashFile.getFont().getFontData();
          
          int	y_offset = ( pb1.getSize().y - fd[0].getHeight() )/2;
          
          if ( y_offset < 0 ){
          	y_offset = 0;
          }
          */
          
          int	char_width = event.gc.getFontMetrics().getAverageCharWidth();
          
          String	percent = DisplayFormatters.formatPercentFromThousands(perc);
          
          event.gc.drawText(percent, ( pb1.getSize().x - percent.length() * char_width )/2, -1, true);
      
          event.gc.setForeground(old);
        }
      };
      
      pb1.addListener(SWT.Paint,pb_listener);
    
    
    Label l3 = new Label(splash, SWT.NONE);
    l3.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    l3.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
    l3.setText(MessageText.getString("ConfigView.download.abbreviated"));
    l3.addMouseListener(mListener);
    l3.addMouseMoveListener(mMoveListener);
    l3.pack();
    l3.setLocation(xSize, 0);
    xSize += l3.getSize().x + 3;

    splashDown = new Label(splash, SWT.NONE);
    splashDown.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    splashDown.setText(""); //$NON-NLS-1$
    splashDown.addMouseListener(mListener);
    splashDown.addMouseMoveListener(mMoveListener);
    splashDown.setSize(65, hSize);
    splashDown.setLocation(xSize, 0);
    xSize += 65 + 3;

    Label l4 = new Label(splash, SWT.NONE);
    l4.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    l4.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
    l4.setText(MessageText.getString("ConfigView.upload.abbreviated"));
    l4.addMouseListener(mListener);
    l4.addMouseMoveListener(mMoveListener);
    l4.pack();
    l4.setLocation(xSize, 0);
    xSize += l4.getSize().x + 3;

    splashUp = new Label(splash, SWT.NONE);
    splashUp.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    splashUp.setText(""); //$NON-NLS-1$
    splashUp.addMouseListener(mListener);
    splashUp.addMouseMoveListener(mMoveListener);
    splashUp.setSize(65, hSize);
    splashUp.setLocation(xSize, 0);
    xSize += 65 + 3;

    Label l5 = new Label(splash, SWT.NONE);
    l5.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    l5.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
    l5.setText(MessageText.getString("MyTorrentsView.eta") + ":");
    l5.addMouseListener(mListener);
    l5.addMouseMoveListener(mMoveListener);
    l5.pack();
    l5.setLocation(xSize, 0);
    xSize += l5.getSize().x + 3;

    splashTime = new Label(splash, SWT.NONE);
    splashTime.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
    splashTime.setText(""); //$NON-NLS-1$
    splashTime.addMouseListener(mListener);
    splashTime.addMouseMoveListener(mMoveListener);
    splashTime.setSize(65, hSize);
    splashTime.setLocation(xSize, 0);
    xSize += 65 + 3;

    
    
    
    splash.addListener(SWT.Deiconify, new Listener() {
      public void handleEvent(Event e) {
        splash.setVisible(true);
        //splash.setMaximized(true);
        splash.setActive();
      }
    });
    splash.setSize(xSize + 3, hSize + 2);
    
    Menu menu = new Menu(splash,SWT.POP_UP);
    MenuItem itemClose = new MenuItem(menu,SWT.NULL);
    itemClose.setText(MessageText.getString("wizard.close"));
    itemClose.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        close();
        MyTorrentsSuperView viewMyTorrents = (MyTorrentsSuperView) Tab.getView(MainWindow.getWindow().getMytorrents().getTabItem());
        if(viewMyTorrents != null) {
          viewMyTorrents.removeDownloadBar(MinimizedWindow.this.manager);
        }
      }
    });
    
    splash.setMenu(menu);
    lDrag.setMenu(menu);
    l1.setMenu(menu);
    l3.setMenu(menu);
    l4.setMenu(menu);
    l5.setMenu(menu);
    pb1.setMenu(menu);
    splashDown.setMenu(menu);
    splashFile.setMenu(menu);
    splashUp.setMenu(menu);
    splashTime.setMenu(menu);
    
    
    downloadBars.add(this);        
    splash.setVisible(true);
    
    
  }

  public static ShellManager getShellManager() {
      return shellManager;
  }

  private void setSnapLocation(Point currentLoc) {
    /*
    if (downloadBars.size() > 1) {
      Rectangle snap = splash.getBounds();
      snap.x -= 10;
      snap.y -= 10;
      snap.height += 20;
      snap.width += 20;
      Point cursor = splash.getDisplay().getCursorLocation();
      if (!(cursor.x < snap.x || cursor.x > snap.x + snap.width || cursor.y < snap.y || cursor.y > snap.y + snap.height)) {
        if(snapped)
          return;
        for (int i = 0; i < downloadBars.size(); i++) {
          Shell downloadBar = (Shell) downloadBars.get(i);
          if (downloadBar != splash && !downloadBar.isDisposed()) {
            Rectangle rectangle = downloadBar.getBounds();
            if (snap.intersects(rectangle)) {
              currentLoc.x = rectangle.x;
              currentLoc.y = currentLoc.y > rectangle.y ? rectangle.y + rectangle.height : rectangle.y - rectangle.height;
              snapped = true;
            }
          }
        }
      } else {
        snapped = false;
      }
    }
    */
    if (currentLoc.x < 10)
      currentLoc.x = 0;
    else if (currentLoc.x > screen.width - splash.getBounds().width - 10)
      currentLoc.x = screen.width - splash.getBounds().width;
    if (currentLoc.y < 10)
      currentLoc.y = 0;
    MinimizedWindow mw = this;
    int height = 0;
    while (mw != null) {
      Shell s = mw.getShell();
      if (s.isDisposed())
        mw = null;
      else {
        height += s.getBounds().height - 1;
        mw = mw.getStucked();
        if (mw == this)
          mw = null;
      }
    }
    if (currentLoc.y > screen.height - height - 10)
      currentLoc.y = screen.height - height;

    if (downloadBars.size() > 1) {
      for (int i = 0; i < downloadBars.size(); i++) {
        MinimizedWindow downloadBar = (MinimizedWindow) downloadBars.get(i);
        Point location = downloadBar.getShell().getLocation();
        // isn't the height always 10?
        // Gudy : No it depends on your system font.
        location.y += downloadBar.getShell().getBounds().height;
        //Stucking to someone else
        if (downloadBar != this && downloadBar.getStucked() == null || downloadBar.getStucked() == this) {
          if (Math.abs(location.x - currentLoc.x) < 10 && location.y - currentLoc.y < 10 & location.y - currentLoc.y > 0) {
            downloadBar.setStucked(this);
            currentLoc.x = location.x;
            currentLoc.y = location.y - 1;
          }
        }
        //Un-stucking from someone
        if (downloadBar != this && downloadBar.getStucked() == this) {
          if (Math.abs(location.x - currentLoc.x) > 10 || Math.abs(location.y - currentLoc.y) > 10)
            downloadBar.setStucked(null);
        }
      }
    }

    splash.setLocation(currentLoc);
    MinimizedWindow mwCurrent = this;
    while (mwCurrent != null) {
      currentLoc.y += mwCurrent.getShell().getBounds().height - 1;
      MinimizedWindow mwChild = mwCurrent.getStucked();
      if (mwChild != null && mwChild != this) {
        Shell s = mwChild.getShell();
        if (s.isDisposed()) {
          mwCurrent.setStucked(null);
          mwCurrent = null;
        }
        else {
          mwCurrent = mwChild;
          mwCurrent.getShell().setLocation(currentLoc);
        }
      }
      else
        mwCurrent = null;
    }
  }

  public void close() {
    if(!splash.isDisposed()) {
      Display display = splash.getDisplay();
      if(display != null && ! display.isDisposed()) {
       display.asyncExec(new AERunnable() {
        public void runSupport() {
          if(!splash.isDisposed()) {
            splash.dispose();
          }
        }
       }); 
      }
    }
    downloadBars.remove(this);
  }


  public void refresh() {
    if (splash.isDisposed())
      return;
    splashFile.setText(manager.getDisplayName());
    final int percent = manager.getStats().getCompleted();
    splashDown.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec(manager.getStats().getDataReceiveRate()));
    splashUp.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec(manager.getStats().getDataSendRate()));
    splashTime.setText(DisplayFormatters.formatETA(manager.getStats().getETA()));
    
    if ( pb1.getSelection() != percent ){
    	
    	pb1.setSelection(percent);
    
    	pb1.redraw();
    }
  }

  public void setVisible(boolean visible) {
    splash.setVisible(visible);
  }

  public Shell getShell() {
    return this.splash;
  }

  public MinimizedWindow getStucked() {
    return this.stucked;
  }

  public void setStucked(MinimizedWindow mw) {
    this.stucked = mw;
  }
}
