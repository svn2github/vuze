/*
 * File    : ColorParameter.java
 * Created : 12 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;


/**
 * @author Olivier
 *
 */
public class ColorParameter implements IParameter, ParameterListener {

  private Button colorChooser;
  protected String sParamName;
  private Image img;

  public ColorParameter(final Composite composite,
                        final String name,
                        int r, int g, int b) {
    sParamName = name;
    colorChooser = new Button(composite,SWT.PUSH);
    final int rV = COConfigurationManager.getIntParameter(name+".red",r);
    final int gV = COConfigurationManager.getIntParameter(name+".green",g);
    final int bV = COConfigurationManager.getIntParameter(name+".blue",b);
    updateButtonColor(composite.getDisplay(), rV, gV, bV);

    COConfigurationManager.addParameterListener(sParamName, this);
    
    colorChooser.addListener(SWT.Dispose, new Listener() {
      public void handleEvent(Event e) {
        COConfigurationManager.removeParameterListener(sParamName, ColorParameter.this);
      }
    });

    colorChooser.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        ColorDialog cd = new ColorDialog(composite.getShell());
        cd.setRGB(new RGB(rV,gV,bV));
        RGB newColor = cd.open();
        if (newColor == null)
          return;
        newColorChosen();
        COConfigurationManager.setRGBParameter(name, rV,gV,bV );
      }
    });
    
  }
  
  private void updateButtonColor(final Display display, final int rV, final int gV, final int bV) {
    Image oldImg = img;
    Color color = new Color(display,rV,gV,bV);
    img = new Image(display,25,10);
    GC gc = new GC(img);
    gc.setBackground(color);
    gc.fillRectangle(0,0,25,10);
    gc.dispose();
    color.dispose();
    colorChooser.setImage(img);
    if(oldImg != null && ! oldImg.isDisposed())
      oldImg.dispose();
  }

  public Control getControl() {
    return colorChooser;
  }

  public void setLayoutData(Object layoutData) {
    colorChooser.setLayoutData(layoutData);
  }

  public void parameterChanged(String parameterName) {
    final int rV = COConfigurationManager.getIntParameter(sParamName+".red");
    final int gV = COConfigurationManager.getIntParameter(sParamName+".green");
    final int bV = COConfigurationManager.getIntParameter(sParamName+".blue");
    updateButtonColor(colorChooser.getDisplay(), rV, gV, bV);
  }
  
  public void newColorChosen() {
    // subclasses can write their own code
  }
}
