/*
 * File    : ChangeSelectionActionPerformer.java
 * Created : 10 oct. 2003 15:38:53
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
 
package org.gudy.azureus2.ui.swt.views.config;

import org.eclipse.swt.widgets.Control;

/**
 * @author Olivier
 * 
 */
public class ChangeSelectionActionPerformer implements IAdditionalActionPerformer{

  boolean selected = false;

  Control[] controls;
  
  public ChangeSelectionActionPerformer(Control[] controls) {
    this.controls = controls;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#performAction()
   */
  public void performAction() {
    if(controls == null)
      return;
    for(int i = 0 ; i < controls.length ; i++) {
      controls[i].setEnabled(selected);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#setIntValue(int)
   */
  public void setIntValue(int value) {    
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#setSelected(boolean)
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#setStringValue(java.lang.String)
   */
  public void setStringValue(String value) {    
  }

}
