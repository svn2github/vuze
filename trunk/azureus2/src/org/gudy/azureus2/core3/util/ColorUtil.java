/*
 * Created on Dec 18, 2003
 * Created by nolar
 *
 */
package org.gudy.azureus2.core3.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.COConfigurationListener;



/**
 * This class holds and maintains user color preferences.
 */
public class ColorUtil implements COConfigurationListener {
 
  private Display display;
  
  
  public Color black;
  public Color blue;
  public Color grey;
  public Color red;
  public Color white;
  public Color background;
  public Color red_ConsoleView;
  public Color red_ManagerItem;
  
  public Color seeding;
  public Color stopped_complete;
  public Color stopped_incomplete;
  public Color error;
  public Color downloading;
  public Color waiting_ready;
  
  public Color[] gradient = new Color[5];
 

  
  /**
   * Create a ColorUtil for the given display to hold and maintain user color prefs.
   */
  public ColorUtil(Display display) {
    this.display = display;
    initializeColors();
    COConfigurationManager.addListener(this);
  }

  
  private void initializeColors() {
    int r, g, b;
    
    r = COConfigurationManager.getIntParameter("Color gradient.red",255);
    g = COConfigurationManager.getIntParameter("Color gradient.green",150);
    b = COConfigurationManager.getIntParameter("Color gradient.blue",1);
    for(int i = 0; i < 5; i++) {
      dispose(gradient[i]);
      gradient[i] = new Color(display,r+((255-r)*(4-i))/4,g+((255-g)*(4-i))/4,b+((255-b)*(4-i))/4);
    }
    
    
    dispose(black);
    black = new Color(display, new RGB(0, 0, 0));
    
    dispose(blue);
    blue = new Color(display, new RGB(0, 0, 170));
    
    dispose(grey);
    grey = new Color(display, new RGB(170, 170, 170));
    
    dispose(red);
    red = new Color(display, new RGB(255, 0, 0));
    
    dispose(white);
    white = new Color(display, new RGB(255, 255, 255));
    
    dispose(background);
    background = new Color(display , new RGB(248,248,248));
    
    dispose(red_ConsoleView);
    red_ConsoleView = new Color(display, new RGB(255, 192, 192));
    
    dispose(red_ManagerItem);
    red_ManagerItem = new Color(display, new RGB(255, 68, 68));
    
    
    r = COConfigurationManager.getIntParameter("Color seeding.red",0);
    g = COConfigurationManager.getIntParameter("Color seeding.green",110);
    b = COConfigurationManager.getIntParameter("Color seeding.blue",0);
    dispose(seeding);
    seeding = new Color(display, new RGB(r, g, b));
    
    r = COConfigurationManager.getIntParameter("Color stopped_complete.red",192);
    g = COConfigurationManager.getIntParameter("Color stopped_complete.green",192);
    b = COConfigurationManager.getIntParameter("Color stopped_complete.blue",0);
    dispose(stopped_complete);
    stopped_complete = new Color(display, new RGB(r, g, b));
    
    r = COConfigurationManager.getIntParameter("Color stopped_incomplete.red",170);
    g = COConfigurationManager.getIntParameter("Color stopped_incomplete.green",170);
    b = COConfigurationManager.getIntParameter("Color stopped_incomplete.blue",170);
    dispose(stopped_incomplete);
    stopped_incomplete = new Color(display, new RGB(r, g, b));
    
    r = COConfigurationManager.getIntParameter("Color error.red",255);
    g = COConfigurationManager.getIntParameter("Color error.green",68);
    b = COConfigurationManager.getIntParameter("Color error.blue",68);
    dispose(error);
    error = new Color(display, new RGB(r, g, b));
    
    r = COConfigurationManager.getIntParameter("Color downloading.red",0);
    g = COConfigurationManager.getIntParameter("Color downloading.green",0);
    b = COConfigurationManager.getIntParameter("Color downloading.blue",0);
    dispose(downloading);
    downloading = new Color(display, new RGB(r, g, b));
    
    r = COConfigurationManager.getIntParameter("Color waiting_ready.red",255);
    g = COConfigurationManager.getIntParameter("Color waiting_ready.green",150);
    b = COConfigurationManager.getIntParameter("Color waiting_ready.blue",1);
    dispose(waiting_ready);
    waiting_ready = new Color(display, new RGB(r, g, b));
       
  }
  
  
  public void configurationSaved() {
    initializeColors();
  }
  
  private void dispose(Color to_dispose) {
    if(to_dispose != null && !to_dispose.isDisposed()) {
        to_dispose.dispose();
    }
  }
  
  /**
   * 
   */
  public void disposeAll() {
    
  }
  

}
