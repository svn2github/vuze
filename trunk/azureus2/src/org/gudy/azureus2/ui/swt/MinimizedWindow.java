/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
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

  private Color[] blues;

  private int xPressed, yPressed;
  private boolean moving;
  
  private int hSize;
  
  private Label splashFile;
  private Label splashPercent;
  private Label splashDown;
  private Label splashUp;
  
  private DownloadManager manager;
  private Display display;

  public MinimizedWindow(DownloadManager manager, Shell main) {
    this.display = main.getDisplay();
    this.manager = manager;
    
    blues = new Color[5];
    blues[4] = new Color(display, new RGB(0, 128, 255));
    blues[3] = new Color(display, new RGB(64, 160, 255));
    blues[2] = new Color(display, new RGB(128, 192, 255));
    blues[1] = new Color(display, new RGB(192, 224, 255));
    blues[0] = new Color(display, new RGB(255, 255, 255));
    //   The splash Screen setup
    splash = new Shell(main, SWT.ON_TOP);
    lDrag = new Label(splash, SWT.NULL);
    lDrag.setImage(ImageRepository.getImage("dragger"));
    lDrag.pack();
    int hSizeImage = lDrag.getSize().y;
    int xSize = lDrag.getSize().x + 3;
    lDrag.setLocation(0, 0);

    final Display _display = display;
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
          splash.setLocation(currentLoc.x - dX, currentLoc.y - dY);
          //System.out.println("Position : " + xPressed + " , " + yPressed);
        }
      }
    };

    splash.setBackground(blues[0]);
    splash.setForeground(blues[4]);
    splash.addMouseListener(mListener);
    splash.addMouseMoveListener(mMoveListener);
    lDrag.addMouseListener(mListener);
    lDrag.addMouseMoveListener(mMoveListener);

    Label l1 = new Label(splash, SWT.NONE);
    l1.setBackground(blues[0]);
    l1.setForeground(blues[4]);
    l1.setText("Name:");
    l1.addMouseListener(mListener);
    l1.addMouseMoveListener(mMoveListener);
    l1.pack();
    l1.setLocation(xSize, 0);
    xSize += l1.getSize().x + 3;

    int hSizeText = l1.getSize().y;
    hSize = hSizeText > hSizeImage ? hSizeText : hSizeImage;

    splashFile = new Label(splash, SWT.NONE);
    splashFile.setBackground(blues[0]);
    splashFile.setText("");
    splashFile.addMouseListener(mListener);
    splashFile.addMouseMoveListener(mMoveListener);
    splashFile.setSize(250, hSize);
    splashFile.setLocation(xSize, 0);
    xSize += 250 + 3;

    Label l2 = new Label(splash, SWT.NONE);
    l2.setBackground(blues[0]);
    l2.setForeground(blues[4]);
    l2.setText("C:");
    l2.addMouseListener(mListener);
    l2.addMouseMoveListener(mMoveListener);
    l2.pack();
    l2.setLocation(xSize, 0);
    xSize += l2.getSize().x + 3;

    splashPercent = new Label(splash, SWT.NONE);
    splashPercent.setBackground(blues[0]);
    splashPercent.setText("");
    splashPercent.addMouseListener(mListener);
    splashPercent.addMouseMoveListener(mMoveListener);
    splashPercent.setSize(45, hSize);
    splashPercent.setLocation(xSize, 0);
    xSize += 45 + 3;

    Label l3 = new Label(splash, SWT.NONE);
    l3.setBackground(blues[0]);
    l3.setForeground(blues[4]);
    l3.setText("D:");
    l3.addMouseListener(mListener);
    l3.addMouseMoveListener(mMoveListener);
    l3.pack();
    l3.setLocation(xSize, 0);
    xSize += l3.getSize().x + 3;

    splashDown = new Label(splash, SWT.NONE);
    splashDown.setBackground(blues[0]);
    splashDown.setText("");
    splashDown.addMouseListener(mListener);
    splashDown.addMouseMoveListener(mMoveListener);
    splashDown.setSize(65, hSize);
    splashDown.setLocation(xSize, 0);
    xSize += 65 + 3;

    Label l4 = new Label(splash, SWT.NONE);
    l4.setBackground(blues[0]);
    l4.setForeground(blues[4]);
    l4.setText("U:");
    l4.addMouseListener(mListener);
    l4.addMouseMoveListener(mMoveListener);
    l4.pack();
    l4.setLocation(xSize, 0);
    xSize += l4.getSize().x + 3;

    splashUp = new Label(splash, SWT.NONE);
    splashUp.setBackground(blues[0]);
    splashUp.setText("");
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
    splash.setVisible(true);
  }

  public void close() {
    if (blues != null) {
      for (int i = 0; i < blues.length; i++) {
        if (blues[i] != null && !blues[i].isDisposed())
          blues[i].dispose();
      }
    }
    splash.dispose();    
  }
  
  public void refresh() {
   if(splash.isDisposed())
    return;
   splashFile.setText(manager.getName());
   int percent = manager.getCompleted();
   splashPercent.setText((percent/10) + "." + (percent%10) + " %");
   splashDown.setText(manager.getDownloadSpeed());
   splashUp.setText(manager.getUploadSpeed());
  }  
  
  public void setVisible(boolean visible) {
    splash.setVisible(visible);
  }
}
