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
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;

/** Workaround Eclipse Bug Bug 42416
 *   "[Platform Inconsistency] GC(Table) has wrong origin"
 *
 */
public class VerticalAligner implements ParameterListener {
  private static VerticalAligner instance;
  private boolean bFixGTKBug;
  
  public synchronized static VerticalAligner getInstance() {
    if(instance == null) instance = new VerticalAligner();
    return instance;
  }
  
  private VerticalAligner() {
  	parameterChanged("");
  	COConfigurationManager.addParameterListener("SWT_bGTKTableBug",this);
  }
  
	public void parameterChanged(String parameterName) {
	  // some people switch from motif to gtk & back again, so make this
	  // only apply to GTK, even if it was enabled prior
  	bFixGTKBug = COConfigurationManager.getBooleanParameter("SWT_bGTKTableBug") &&
  	             System.getProperty("os.name").equals("Linux") && SWT.getPlatform().equals("gtk");
	}
  
  public static int getTableAdjustVerticalBy(Table t) {
   return getInstance().COgetTableAdjustVerticalBy(t);
  }      

  public int COgetTableAdjustVerticalBy(Table t) {
    if (!bFixGTKBug || t == null || t.isDisposed())
      return 0;
   return -t.getHeaderHeight(); 
  }      
   
}
