/*
 * Created on 7 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.update;

import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateManager;
import org.gudy.azureus2.plugins.update.UpdateManagerListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateMonitor implements UpdateManagerListener {

  UpdateWindow window;
  
  public UpdateMonitor() {
    window = new UpdateWindow();
    PluginInitializer.getDefaultInterface().getUpdateManager().addListener(this);
  }
  
  public void updateAdded(UpdateManager manager, Update update) {
   window.addUpdate(update); 
  }  
}
