/*
 * Created on Oct 21, 2004
 * Created by Alon Rohter
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

package org.gudy.azureus2.ui.swt.views.configsections;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.Parameter;
import org.gudy.azureus2.ui.swt.config.ParameterChangeListener;
import org.gudy.azureus2.ui.swt.config.StringListParameter;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.StartupUtils;

/**
 *
 */
public class ConfigSectionInterfaceLanguage implements ConfigSectionSWT {
  
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_INTERFACE;
  }

  public String configSectionGetName() {
    return "language";
  }

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    Label label;
    GridLayout layout;
    Composite cMain = new Composite( parent,  SWT.NULL );
    cMain.setLayoutData( new GridData( GridData.FILL_BOTH ) );
    layout = new GridLayout();
    layout.numColumns = 2;
    cMain.setLayout( layout );
    
    label = new Label( cMain, SWT.NULL );
    Messages.setLanguageText( label, "MainWindow.menu.language" );  //old name path, but already translated

    Locale[] locales = MessageText.getLocales();
    
    String[] drop_labels = new String[ locales.length ];
    String[] drop_values = new String[ locales.length ];
    for( int i=0; i < locales.length; i++ ) {
      Locale locale = locales[ i ];
      drop_labels[ i ] = locale.getDisplayName( locale );
      drop_values[ i ] = locale.toString();
    }
    
    final StringListParameter locale_param = new StringListParameter( cMain, "locale", "", drop_labels, drop_values );
    locale_param.addChangeListener( new ParameterChangeListener() {
      public void parameterChanged( Parameter p, boolean caused_internally ) {
        StartupUtils.setLocale();
        MainWindow.getWindow().getMenu().refreshLanguage();
      }
    });
    

    return cMain;
  }

}
