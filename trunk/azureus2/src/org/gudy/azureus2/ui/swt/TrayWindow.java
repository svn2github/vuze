/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core.*;
import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.*;

/**
 * @author Olivier
 * 
 */
public class TrayWindow implements IComponentListener {

  GlobalManager globalManager;
  List managers;

  MainWindow main;
  Display display;
  Shell minimized;
  Label label;
  private Menu menu;

  private Rectangle screen;

  private int xPressed;
  private int yPressed;
  private boolean moving;

  public TrayWindow(MainWindow _main) {
    this.managers = new ArrayList();
    this.main = _main;
    this.display = main.getDisplay();
    minimized = new Shell(main.getShell(), SWT.ON_TOP);
    minimized.setText("Azureus"); //$NON-NLS-1$
    label = new Label(minimized, SWT.NULL);
    label.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    label.setSize(16, 16);
    minimized.setSize(18, 18);
    screen = display.getClientArea();
    //System.out.println(screen);
    minimized.setLocation(screen.width - 18, screen.height - 18);
    minimized.setVisible(false);
    //minimized.open();    

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

      public void mouseDoubleClick(MouseEvent e) {
        restore();
      }

    };
    MouseMoveListener mMoveListener = new MouseMoveListener() {
      public void mouseMove(MouseEvent e) {
        if (moving) {
          int dX = xPressed - e.x;
          int dY = yPressed - e.y;
          Point currentLoc = minimized.getLocation();
          int x = currentLoc.x - dX;
          int y = currentLoc.y - dY;
          if (x < 10)
            x = 0;
          if (x > screen.width - 28)
            x = screen.width - 18;
          if (y < 10)
            y = 0;
          if (y > screen.height - 28)
            y = screen.height - 18;
          minimized.setLocation(x, y);
        }
      }
    };

    label.addMouseListener(mListener);
    label.addMouseMoveListener(mMoveListener);

    menu = new Menu(minimized, SWT.CASCADE);
    label.setMenu(menu);

    MenuItem file_show = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(file_show, "TrayWindow.menu.show"); //$NON-NLS-1$
    menu.setDefaultItem(file_show);
    file_show.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        restore();
      }
    });

    main.addCloseDownloadBarsToMenu(menu);

    new MenuItem(menu, SWT.SEPARATOR);

    MenuItem file_exit = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(file_exit, "TrayWindow.menu.exit"); //$NON-NLS-1$
    file_exit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        main.dispose();
      }
    });

    globalManager = main.getGlobalManager();
    globalManager.addListener(this);
  }

  public void setVisible(boolean visible) {
    minimized.setVisible(visible);
    if (visible) {
      //minimized.setFocus();
      //minimized.setActive();
    }
    else {
      moving = false;
    }
  }

  public void dispose() {
    minimized.dispose();
  }

  public void restore() {
    if (!COConfigurationManager.getBooleanParameter("Password enabled", false)) {
      minimized.setVisible(false);
      main.setVisible(true);
      moving = false;
    }
    else {
      PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
    }    
  }

  public void refresh() {
    if (minimized.isDisposed() || !minimized.isVisible())
      return;
    StringBuffer toolTip = new StringBuffer();
    String separator = ""; //$NON-NLS-1$
    synchronized (managers) {
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager) managers.get(i);
        String name = manager.getName();
        String completed = (manager.getCompleted() / 10) + "." + (manager.getCompleted() % 10) + "%"; //$NON-NLS-1$ //$NON-NLS-2$
        toolTip.append(separator);
        toolTip.append(name);
        toolTip.append(" -- C: ");
        toolTip.append(completed);
        toolTip.append(", D : ");
        toolTip.append(manager.getDownloadSpeed());
        toolTip.append(", U : ");
        toolTip.append(manager.getUploadSpeed());
        separator = "\n"; //$NON-NLS-1$
      }
    }
    //label.setToolTipText(toolTip.toString());
    //minimized.moveAbove(null);
  }
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object)
   */
  public void objectAdded(Object created) {
    if (!(created instanceof DownloadManager))
      return;
    synchronized (managers) {
      managers.add(created);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectRemoved(java.lang.Object)
   */
  public void objectRemoved(Object removed) {
    synchronized (managers) {
      managers.remove(removed);
    }
  }

  public void updateLanguage() {
    MainWindow.updateMenuText(menu);
  }

  /**
   * @param moving
   */
  public void setMoving(boolean moving) {
    this.moving = moving;
  }

}
