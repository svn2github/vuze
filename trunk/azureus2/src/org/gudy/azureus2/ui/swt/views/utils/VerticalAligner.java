/*
 * File    : VerticalAlignment.java
 * Created : 22 dec. 2003
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
package org.gudy.azureus2.ui.swt.views.utils;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;

/**
 * @author Olivier
 *
 */
public class VerticalAligner implements ParameterListener {
  public static final String parameterName = "Vertical Alignement";
  
  private int verticalAlignement;
  private static VerticalAligner instance;
  
  public synchronized static VerticalAligner getInstance() {
    if(instance == null) instance = new VerticalAligner();
    return instance;
  }
  
  private VerticalAligner() {
    if(System.getProperty("os.name").equals("Linux") && SWT.getPlatform().equals("gtk")) {
    	verticalAlignement = COConfigurationManager.getIntParameter(parameterName,28);
    	COConfigurationManager.addParameterListener(parameterName,this);
    } else {
    	verticalAlignement = 0;
    }
  }
  
	public void parameterChanged(String parameterName) {
    if(parameterName != null && parameterName.equals(VerticalAligner.parameterName)) {
    	verticalAlignement = COConfigurationManager.getIntParameter(parameterName,0);
    }
	}
  
  public int getVerticalAlignement() {
   return  verticalAlignement; 
  }
  
  public void setVerticalAlignement(int alignement) {
    this.verticalAlignement = alignement;
  }
  
  public static int getAlignement() {
   return - getInstance().getVerticalAlignement();
  }      
   
}
