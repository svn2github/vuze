/*
 * Created on 2 mai 2004 Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details (
 * see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * AELITIS, SARL au capital de 30,000 euros, 8 Alle Lenotre, La Grille Royale,
 * 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.ui.common.util.HSLColor;
/**
 * @author Olivier Chalouhi
 *  
 */
public class Colors implements ParameterListener {
  public static final int BLUES_LIGHTEST = 0;
  public static final int BLUES_DARKEST = 9;
  public static final int BLUES_MIDLIGHT = (BLUES_DARKEST + 1) / 4;
  public static final int BLUES_MIDDARK = ((BLUES_DARKEST + 1) / 2)
      + BLUES_MIDLIGHT;
  
  public static Color[] blues = new Color[BLUES_DARKEST + 1];
  public static Color colorProgressBar;
  public static Color colorInverse;
  public static Color colorShiftLeft;
  public static Color colorShiftRight;
  public static Color colorError;
  public static Color colorAltRow;
  public static Color colorWarning;
  public static Color black;
  public static Color blue;
  public static Color grey;
  public static Color red;
  public static Color white;
  public static Color background;
  public static Color red_ConsoleView;
  
  private void allocateBlues() {    
    int r = 0;
    int g = 128;
    int b = 255;
    try {
      r = COConfigurationManager.getIntParameter("Color Scheme.red", r);
      g = COConfigurationManager.getIntParameter("Color Scheme.green", g);
      b = COConfigurationManager.getIntParameter("Color Scheme.blue", b);
      HSLColor hslColor = new HSLColor();
      Color colorTables = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
      int tR = colorTables.getRed();
      int tG = colorTables.getGreen();
      int tB = colorTables.getBlue();
      // 0 == window background (white)
      // [blues.length-1] == rgb
      // in between == blend
      for (int i = 0; i < blues.length; i++) {
        Color toBeDisposed = blues[i];
        hslColor.initHSLbyRGB(r, g, b);
        float blendBy = (i == 0) ? 1 : (float) 1.0
            - ((float) i / (float) (blues.length - 1));
        hslColor.blend(tR, tG, tB, blendBy);
        blues[i] = new Color(display, hslColor.getRed(), hslColor.getGreen(),
            hslColor.getBlue());
        if (toBeDisposed != null && !toBeDisposed.isDisposed()) {
          toBeDisposed.dispose();
        }
      }
      Color toBeDisposed = colorInverse;
      hslColor.initHSLbyRGB(r, g, b);
      hslColor.reverseColor();
      colorInverse = new Color(display, hslColor.getRed(), hslColor.getGreen(),
          hslColor.getBlue());
      if (toBeDisposed != null && !toBeDisposed.isDisposed()) {
        toBeDisposed.dispose();
      }
      toBeDisposed = colorShiftRight;
      hslColor.initHSLbyRGB(r, g, b);
      hslColor.setHue(hslColor.getHue() + 20);
      colorShiftRight = new Color(display, hslColor.getRed(), hslColor
          .getGreen(), hslColor.getBlue());
      if (toBeDisposed != null && !toBeDisposed.isDisposed()) {
        toBeDisposed.dispose();
      }
      toBeDisposed = colorShiftLeft;
      hslColor.initHSLbyRGB(r, g, b);
      hslColor.setHue(hslColor.getHue() - 20);
      colorShiftLeft = new Color(display, hslColor.getRed(), hslColor
          .getGreen(), hslColor.getBlue());
      if (toBeDisposed != null && !toBeDisposed.isDisposed()) {
        toBeDisposed.dispose();
      }
    } catch (Exception e) {
      LGLogger.log(LGLogger.ERROR, "Error allocating colors");
      e.printStackTrace();
    }
  }
  
  public void disposeColors() {
    if(display == null || display.isDisposed())
      return;
    
    display.syncExec(new Runnable() {
      public void run() {
        if (Colors.colorProgressBar != null
            && !Colors.colorProgressBar.isDisposed())
          Colors.colorProgressBar.dispose();
        for (int i = 0; i < Colors.blues.length; i++) {
          if (Colors.blues[i] != null && !Colors.blues[i].isDisposed())
            Colors.blues[i].dispose();
        }
        Color[] colorsToDispose = {colorInverse, colorShiftLeft, colorShiftRight,
            colorError, grey, black, blue, red, white, red_ConsoleView,
            colorAltRow, colorWarning};
        for (int i = 0; i < colorsToDispose.length; i++) {
          if (colorsToDispose[i] != null && !colorsToDispose[i].isDisposed()) {
            colorsToDispose[i].dispose();
          }
        }
      }
    });
  }
  
  /**
   * @param background The background to set.
   */
  public void setBackground(Color background) {
    if(Colors.background != null && !Colors.background.isDisposed()) {
      Color old = Colors.background;
      Colors.background = background;
      old.dispose();
    }    
  }
  
  private void allocateColorProgressBar() {
    if(display == null || display.isDisposed())
      return;
    
    display.syncExec(new Runnable() {
      public void run() {
        colorProgressBar = new AllocateColor("progressBar", colorShiftRight.getRGB(), colorProgressBar).getColor();
      }
    });
  }

  private void allocateColorError() {
    if(display == null || display.isDisposed())
      return;
    
    display.syncExec(new Runnable() {
      public void run() {
        colorError = new AllocateColor("error", new RGB(255, 68, 68), colorError).getColor();
      }
    });
  }

  private void allocateColorWarning() {
    if(display == null || display.isDisposed())
      return;
    
    display.syncExec(new Runnable() {
      public void run() {
        Color colorTables = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        HSLColor hslBG = new HSLColor();
        hslBG.initHSLbyRGB(colorTables.getRed(), colorTables.getGreen(), colorTables.getBlue());
        int lum = hslBG.getLuminence();
    
        HSLColor hslColor = new HSLColor();
        hslColor.initRGBbyHSL(25, 200, lum > 127 ? lum - 128 : lum + 92);
        colorWarning = new AllocateColor("warning", 
                                          new RGB(hslColor.getRed(), hslColor.getGreen(), hslColor.getBlue()), 
                                          colorWarning).getColor();
      }
    });
  }

  private void allocateColorAltRow() {
    if(display == null || display.isDisposed())
      return;
    
    display.syncExec(new Runnable() {
      public void run() {
    Color colorTables = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
    HSLColor hslColor = new HSLColor();
    hslColor.initHSLbyRGB(colorTables.getRed(), colorTables.getGreen(), colorTables.getBlue());

    int lum = hslColor.getLuminence();
    if (lum > 127)
      lum -= 10;
    else
      lum += 30; // it's usually harder to see difference in darkness
    hslColor.setLuminence(lum);
    colorAltRow = new AllocateColor("altRow", 
                                    new RGB(hslColor.getRed(), hslColor.getGreen(), hslColor.getBlue()), 
                                    colorAltRow).getColor();
      }
    });    
  }

  /** Allocates a color */
  private class AllocateColor implements Runnable {
    private Color toBeDeleted = null;
    private String sName;
    private RGB rgbDefault;
    private Color newColor;
    
    public AllocateColor(String sName, RGB rgbDefault, Color colorOld) {
      toBeDeleted = colorOld;
      this.sName = sName;
      this.rgbDefault = rgbDefault;
    }
    
    public Color getColor() {
      display.syncExec(this);
      return newColor;
    }

    public void run() {
      if (COConfigurationManager.getBooleanParameter("Colors." + sName + ".override")) {
        newColor = new Color(display,
           COConfigurationManager.getIntParameter("Colors." + sName + ".red", 
                                                  rgbDefault.red),
           COConfigurationManager.getIntParameter("Colors." + sName + ".green",
                                                  rgbDefault.green),
           COConfigurationManager.getIntParameter("Colors." + sName + ".blue",
                                                  rgbDefault.blue));
      } else {
        newColor = new Color(display, rgbDefault);
        // Since the color is not longer overriden, reset back to default
        // so that the user sees the correct color in Config.
        COConfigurationManager.setParameter("Colors." + sName, rgbDefault); 
      }

      if (toBeDeleted != null && !toBeDeleted.isDisposed())
        toBeDeleted.dispose();
    }
  }
  
  private void allocateDynamicColors() {
    if(display == null || display.isDisposed())
      return;
    
    display.syncExec(new Runnable() {
      public void run() {
        allocateBlues();
        allocateColorProgressBar();
      }
    });
  }

  private void allocateNonDynamicColors() {
    allocateColorWarning();
    allocateColorError();
    allocateColorAltRow();
    
    black = new Color(display, new RGB(0, 0, 0));
    blue = new Color(display, new RGB(0, 0, 170));
    grey = new Color(display, new RGB(170, 170, 170));
    red = new Color(display, new RGB(255, 0, 0));
    white = new Color(display, new RGB(255, 255, 255));
    background = new Color(display , new RGB(248,248,248));
    red_ConsoleView = new Color(display, new RGB(255, 192, 192));
  }   
  
  private Display display;
  
  public Colors() {
    display = SWTThread.getInstance().getDisplay();
    allocateDynamicColors();
    allocateNonDynamicColors();
    
    COConfigurationManager.addParameterListener("Color Scheme", this);
    COConfigurationManager.addParameterListener("Colors.progressBar.override", this);
    COConfigurationManager.addParameterListener("Colors.progressBar", this);
    COConfigurationManager.addParameterListener("Colors.error.override", this);
    COConfigurationManager.addParameterListener("Colors.error", this);
    COConfigurationManager.addParameterListener("Colors.warning.override", this);
    COConfigurationManager.addParameterListener("Colors.warning", this);
    COConfigurationManager.addParameterListener("Colors.altRow.override", this);
    COConfigurationManager.addParameterListener("Colors.altRow", this);
  }
  
  public void parameterChanged(String parameterName) {
    if (parameterName.equals("Color Scheme")) {
      allocateDynamicColors();
    }

    if(parameterName.startsWith("Colors.progressBar")) {
      allocateColorProgressBar();      
    }
    if(parameterName.startsWith("Colors.error")) {
      allocateColorError();
    }
    if(parameterName.startsWith("Colors.warning")) {
      allocateColorWarning();
    }
    if(parameterName.startsWith("Colors.altRow")) {
      allocateColorAltRow();
    }
  }
}