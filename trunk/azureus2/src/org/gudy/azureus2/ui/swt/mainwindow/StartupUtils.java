/*
 * Created on May 1, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.Locale;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;

/**
 * 
 */
public class StartupUtils {
  
  /*
   * Setup Locales during initialization
   */
  public static void setLocale() {
    //  Setup Locales
    Locale[] locales = MessageText.getLocales();
    String savedLocaleString = COConfigurationManager.getStringParameter("locale", Locale.getDefault().toString()); //$NON-NLS-1$
    Locale savedLocale;
    String[] savedLocaleStrings = savedLocaleString.split("_", 3);
    if (savedLocaleStrings.length > 0 && savedLocaleStrings[0].length() == 2) {
      if (savedLocaleStrings.length == 3) {
        savedLocale = new Locale(savedLocaleStrings[0], savedLocaleStrings[1], savedLocaleStrings[2]);
      } else if (savedLocaleStrings.length == 2 && savedLocaleStrings[1].length() == 2) {
        savedLocale = new Locale(savedLocaleStrings[0], savedLocaleStrings[1]);
      } else {
        savedLocale = new Locale(savedLocaleStrings[0]);
      }
    } else {
      if (savedLocaleStrings.length == 3 && 
          savedLocaleStrings[0].length() == 0 && 
          savedLocaleStrings[2].length() > 0) {
        savedLocale = new Locale(savedLocaleStrings[0], 
                                 savedLocaleStrings[1], 
                                 savedLocaleStrings[2]);
      } else {
        savedLocale = Locale.getDefault();
      }
    }
    MessageText.changeLocale(savedLocale);

    ////////////////////////////////////////////////////
  }
  
}
