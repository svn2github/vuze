/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core.DownloadManager;

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

  private int xPressed, yPressed;
  private boolean moving;
  private boolean snapped = false;

  private int hSize;

  private Label splashFile;
  private Label splashPercent;
  private Label splashDown;
  private Label splashUp;

  private DownloadManager manager;

  public MinimizedWindow(DownloadManager manager, Shell main) {
    this.manager = manager;
    this.stucked = null;

    //   The splash Screen setup
    splash = new Shell(main, SWT.ON_TOP);
    this.screen = main.getDisplay().getClientArea();
    lDrag = new Label(splash, SWT.NULL);
    lDrag.setImage(ImageRepository.getImage("dragger")); //$NON-NLS-1$
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

    splash.setBackground(MainWindow.blues[0]);
    splash.setForeground(MainWindow.blues[4]);
    splash.addMouseListener(mListener);
    splash.addMouseMoveListener(mMoveListener);
    lDrag.addMouseListener(mListener);
    lDrag.addMouseMoveListener(mMoveListener);

    Label l1 = new Label(splash, SWT.NONE);
    l1.setBackground(MainWindow.blues[0]);
    l1.setForeground(MainWindow.blues[4]);
    Messages.setLanguageText(l1, "MinimizedWindow.name"); //$NON-NLS-1$
    l1.addMouseListener(mListener);
    l1.addMouseMoveListener(mMoveListener);
    l1.pack();
    l1.setLocation(xSize, 0);
    xSize += l1.getSize().x + 3;

    int hSizeText = l1.getSize().y;
    hSize = hSizeText > hSizeImage ? hSizeText : hSizeImage;

    splashFile = new Label(splash, SWT.NONE);
    splashFile.setBackground(MainWindow.blues[0]);
    splashFile.setText(""); //$NON-NLS-1$
    splashFile.addMouseListener(mListener);
    splashFile.addMouseMoveListener(mMoveListener);
    splashFile.setSize(250, hSize);
    splashFile.setLocation(xSize, 0);
    xSize += 250 + 3;

    Label l2 = new Label(splash, SWT.NONE);
    l2.setBackground(MainWindow.blues[0]);
    l2.setForeground(MainWindow.blues[4]);
    l2.setText("C:");
    l2.addMouseListener(mListener);
    l2.addMouseMoveListener(mMoveListener);
    l2.pack();
    l2.setLocation(xSize, 0);
    xSize += l2.getSize().x + 3;

    splashPercent = new Label(splash, SWT.NONE);
    splashPercent.setBackground(MainWindow.blues[0]);
    splashPercent.setText(""); //$NON-NLS-1$
    splashPercent.addMouseListener(mListener);
    splashPercent.addMouseMoveListener(mMoveListener);
    splashPercent.setSize(45, hSize);
    splashPercent.setLocation(xSize, 0);
    xSize += 45 + 3;

    Label l3 = new Label(splash, SWT.NONE);
    l3.setBackground(MainWindow.blues[0]);
    l3.setForeground(MainWindow.blues[4]);
    l3.setText("D:");
    l3.addMouseListener(mListener);
    l3.addMouseMoveListener(mMoveListener);
    l3.pack();
    l3.setLocation(xSize, 0);
    xSize += l3.getSize().x + 3;

    splashDown = new Label(splash, SWT.NONE);
    splashDown.setBackground(MainWindow.blues[0]);
    splashDown.setText(""); //$NON-NLS-1$
    splashDown.addMouseListener(mListener);
    splashDown.addMouseMoveListener(mMoveListener);
    splashDown.setSize(65, hSize);
    splashDown.setLocation(xSize, 0);
    xSize += 65 + 3;

    Label l4 = new Label(splash, SWT.NONE);
    l4.setBackground(MainWindow.blues[0]);
    l4.setForeground(MainWindow.blues[4]);
    l4.setText("U:");
    l4.addMouseListener(mListener);
    l4.addMouseMoveListener(mMoveListener);
    l4.pack();
    l4.setLocation(xSize, 0);
    xSize += l4.getSize().x + 3;

    splashUp = new Label(splash, SWT.NONE);
    splashUp.setBackground(MainWindow.blues[0]);
    splashUp.setText(""); //$NON-NLS-1$
    splashUp.addMouseListener(mListener);
    splashUp.addMouseMoveListener(mMoveListener);
    splashUp.setSize(65, hSize);
    splashUp.setLocation(xSize, 0);
    xSize += 65 + 3;

    splash.addListener(SWT.Deiconify, new Listener() {
      public void handleEvent(Event e) {
        splash.setVisible(true);
        //splash.setMaximized(true);
        splash.setActive();
      }
    });
    splash.setSize(xSize + 3, hSize + 2);
    downloadBars.add(this);
    splash.setVisible(true);
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
    if (currentLoc.x > screen.width - splash.getBounds().width - 10)
      currentLoc.x = screen.width - splash.getBounds().width;
    if (currentLoc.y < 10)
      currentLoc.y = 0;
    if (currentLoc.y > screen.height - splash.getBounds().height - 10)
      currentLoc.y = screen.height - splash.getBounds().height;

    if (downloadBars.size() > 1) {
      for (int i = 0; i < downloadBars.size(); i++) {
        MinimizedWindow downloadBar = (MinimizedWindow) downloadBars.get(i);
        //Stucking to someone else
        if (downloadBar != this && downloadBar.getStucked() == null || downloadBar.getStucked() == this) {
          int x = downloadBar.getShell().getLocation().x;
          int y = downloadBar.getShell().getLocation().y + downloadBar.getShell().getBounds().height;
          if (Math.abs(x - currentLoc.x) < 10 && y - currentLoc.y < 10 & y - currentLoc.y > 0) {
            downloadBar.setStucked(this);
            currentLoc.x = x;
            currentLoc.y = downloadBar.getShell().getLocation().y + downloadBar.getShell().getBounds().height - 1;
          }
        }
        //Un stucking from someone
        if (downloadBar != this && downloadBar.getStucked() == this) {
          int x = downloadBar.getShell().getLocation().x;
          int y = downloadBar.getShell().getLocation().y + downloadBar.getShell().getBounds().height;
          if (Math.abs(x - currentLoc.x) > 10 || Math.abs(y - currentLoc.y) > 10)
            downloadBar.setStucked(null);
        }
      }
    }

    splash.setLocation(currentLoc);
    MinimizedWindow mwCurrent = this;
    while (mwCurrent != null) {
      currentLoc.y += mwCurrent.getShell().getBounds().height - 1;
      MinimizedWindow mwChild = mwCurrent.getStucked();
      if (mwChild != null) {
        Shell s = mwChild.getShell();
        if (s.isDisposed()) {
          mwCurrent.setStucked(null);
          mwCurrent = null;
        }
        else {
          mwCurrent = mwChild;
          mwCurrent.getShell().setLocation(currentLoc);
          
        }
      } else mwCurrent = null;
    }
  }

  public void close() {
    splash.dispose();
    downloadBars.remove(this);
  }

  public void refresh() {
    if (splash.isDisposed())
      return;
    splashFile.setText(manager.getName());
    int percent = manager.getCompleted();
    splashPercent.setText((percent / 10) + "." + (percent % 10) + " %"); //$NON-NLS-1$ //$NON-NLS-2$
    splashDown.setText(manager.getDownloadSpeed());
    splashUp.setText(manager.getUploadSpeed());
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
