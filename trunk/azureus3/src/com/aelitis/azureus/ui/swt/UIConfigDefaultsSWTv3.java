/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt;

import java.io.File;

import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;

/**
 * @author TuxPaper
 * @created Nov 3, 2006
 *
 */
public class UIConfigDefaultsSWTv3
{
	public static void initialize() {
		ConfigurationManager config = ConfigurationManager.getInstance();
      
      if( System.getProperty("FORCE_PROGRESSIVE", "" ).length() > 0 ) {  //TODO HACK FOR DEMO PURPOSES ONLY!
         config.setParameter( "Prioritize First Piece", true );   
         config.save();
      }
      
		if (config.isNewInstall()) {
			config.setParameter("Auto Upload Speed Enabled", true);
			config.setParameter("Wizard Completed", true);
			config.setParameter("Use default data dir", true);
			config.setParameter("Add URL Silently", true);
			config.setParameter("add_torrents_silently", true);

			config.setParameter("Status Area Show SR", false);
			config.setParameter("Status Area Show NAT", false);
			config.setParameter("Status Area Show IPF", false);         
         
			config.setParameter("window.maximized", true);

			String userPath = SystemProperties.getUserPath();
			File f = new File(userPath, "data");
			if (FileUtil.mkdirs(f)) {
				config.setParameter("Default save path", f.getAbsolutePath());
			}
			config.save();
		}
	}
}
