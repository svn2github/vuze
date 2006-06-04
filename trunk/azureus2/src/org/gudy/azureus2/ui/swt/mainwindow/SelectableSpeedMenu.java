/**
 * Created on 04-Jun-2006
 * Created by Allan Crooks
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;

public class SelectableSpeedMenu {

	public static void generateMenuItems(final Menu parent, final MainWindow main_window, boolean up_menu) {
        final MenuItem[] oldItems = parent.getItems();
        for(int i = 0; i < oldItems.length; i++)
        {
            oldItems[i].dispose();
        }

        final String configKey = 
        	up_menu?
        		TransferSpeedValidator.getActiveUploadParameter( main_window.getGlobalManager()):
        		"Max Download Speed KBs";
               	 
        final int speedPartitions = 12;

        int maxBandwidth = COConfigurationManager.getIntParameter(configKey);
        final boolean unlim = (maxBandwidth == 0);
        if(maxBandwidth == 0 && !up_menu )
        {
            maxBandwidth = 275;
        }
        
        boolean	auto = false;
        
        if ( up_menu ){	   
        	
            final String configAutoKey = 
            		TransferSpeedValidator.getActiveAutoUploadParameter( main_window.getGlobalManager());
     
	        auto = COConfigurationManager.getBooleanParameter( configAutoKey );
	        
	        	// auto
	        final MenuItem auto_item = new MenuItem(parent,SWT.CHECK);
	        auto_item.setText(MessageText.getString("ConfigView.auto"));
	        auto_item.addListener(SWT.Selection,new Listener() {
	          public void handleEvent(Event e) {
	            COConfigurationManager.setParameter(configAutoKey,auto_item.getSelection());
	            COConfigurationManager.save();
	          }
	        });
	        
	        if(auto)auto_item.setSelection(true);
	        auto_item.setEnabled(TransferSpeedValidator.isAutoUploadAvailable(main_window.getAzureusCore()));

	        new MenuItem(parent,SWT.SEPARATOR);
        }
        
        MenuItem item = new MenuItem(parent, SWT.RADIO);
        item.setText(MessageText.getString("MyTorrentsView.menu.setSpeed.unlimited"));
        item.setData("maxkb", new Integer(0));
        item.setSelection(unlim && !auto);
        item.addListener(SWT.Selection, getLimitMenuItemListener(up_menu, parent, main_window, configKey));
        
        Integer[] speed_limits = null;
        
        final String config_prefix = "config.ui.speed.partitions.manual." + ((up_menu) ? "upload": "download") + ".";
        if (COConfigurationManager.getBooleanParameter(config_prefix  + "enabled", false)) {
        	speed_limits = parseSpeedPartitionString(COConfigurationManager.getStringParameter(config_prefix + "values", ""));
        }

        if (speed_limits == null) {
        	java.util.List l = new java.util.ArrayList(); 
	        int delta = 0;
	        for (int i = 0; i < speedPartitions; i++) {
	            final int[] valuePair;
	              if (delta == 0)
	                valuePair = new int[] { maxBandwidth };
	              else
	                valuePair = new int[] { maxBandwidth - delta, maxBandwidth + delta };
	
	              for (int j = 0; j < valuePair.length; j++) {
	            	  if (j==0) {l.add(0, new Integer(valuePair[j]));}
	            	  else {l.add(new Integer(valuePair[j]));}
	              }
	
	              delta += (delta >= 50) ? 50 : (delta >= 10) ? 10 : (delta >= 5) ? 5 : (delta >= 2) ? 3 : 1;
	        }
	        speed_limits = (Integer[])l.toArray(new Integer[l.size()]);
        }
        
        for (int i=0; i<speed_limits.length; i++) {
        	Integer i_value = speed_limits[i]; 
        	int value = i_value.intValue();
        	if (value < 5) {continue;} // Don't allow the user to easily select slow speeds.
            item = new MenuItem(parent, SWT.RADIO);
            item.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec(value * 1024, true));
            item.setData("maxkb", i_value);
            item.addListener(SWT.Selection, getLimitMenuItemListener(up_menu, parent, main_window, configKey));
            item.setSelection(!unlim && value == maxBandwidth && !auto);
        }
    }

	  private static java.util.Map parseSpeedPartitionStringCache = new java.util.HashMap();
	  private synchronized static Integer[] parseSpeedPartitionString(String s) {
		  Integer[] result = (Integer[])parseSpeedPartitionStringCache.get(s);
		  if (result == null) {
			  try {result = parseSpeedPartitionString0(s);}
			  catch (NumberFormatException nfe) {result = new Integer[0];}
			  parseSpeedPartitionStringCache.put(s, result);
		  }
		  if (result.length == 0) {return null;}
		  else {return result;}
	  }
	  
	  private static Integer[] parseSpeedPartitionString0(String s) {
		  java.util.StringTokenizer tokeniser = new java.util.StringTokenizer(s.trim(), ",");
		  java.util.TreeSet values = new java.util.TreeSet(); // Filters duplicates out and orders the values.
		  while (tokeniser.hasMoreTokens()) {
			  values.add(new Integer(Integer.parseInt(tokeniser.nextToken().trim())));
		  }
		  return (Integer[])values.toArray(new Integer[values.size()]);
	  }
	  
	    /**
	     * Gets the selection listener of a upload or download limit menu item (including unlimited)
	     * @param parent The parent menu
	     * @param configKey The configuration key
	     * @return The selection listener
	     */
	   private static final Listener getLimitMenuItemListener(final boolean up_menu, final Menu parent, final MainWindow main_window,  final String configKey)
	   {
	       return new Listener() {
	           public void handleEvent(Event event) {
	               final MenuItem[] items = parent.getItems();
	               for(int i = 0; i < items.length; i++) {
	                    if(items[i] == event.widget)
	                    {
	                        items[i].setSelection(true);
	                        final int cValue = ((Integer)new TransferSpeedValidator(configKey, items[i].getData("maxkb")).getValue()).intValue();
	                        COConfigurationManager.setParameter(configKey, cValue);
	                        
	                        if ( up_menu ){
	                            
	                        	String configAutoKey = 
	                        		TransferSpeedValidator.getActiveAutoUploadParameter( main_window.getGlobalManager());
	             
	                        	COConfigurationManager.setParameter( configAutoKey, false );
	                        }
	                        
	                        COConfigurationManager.save();
	                    }
	                    else {
	                        items[i].setSelection(false);
	                    }
	                }
	           }
	       };
	   }
	
}
 