/*
 * Created on Dec 16, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.ui.swt.utils;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;

public class 
SearchSubsUtils 
{
	public static boolean
	addMenu(
		final SearchSubsResultBase	result,
		Menu						menu )
	{		
		final byte[] hash = result.getHash();
		
		if ( hash != null ){
			MenuItem item = new MenuItem(menu, SWT.PUSH);
			item.setText(MessageText.getString("searchsubs.menu.google.hash"));
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					String s = ByteFormatter.encodeString(hash);
					String URL = "https://google.com/search?q=" + UrlUtils.encode(s);
					launchURL(URL);
				};
			});
		}
		
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("searchsubs.menu.gis"));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String s = result.getName();
				s = s.replaceAll("[-_]", " ");
				String URL = "http://images.google.com/images?q=" + UrlUtils.encode(s);
				launchURL(URL);
			}

		});

		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("searchsubs.menu.google"));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String s = result.getName();
				s = s.replaceAll("[-_]", " ");
				String URL = "https://google.com/search?q=" + UrlUtils.encode(s);
				launchURL(URL);
			};
		});

		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("searchsubs.menu.bis"));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String s = result.getName();
				s = s.replaceAll("[-_]", " ");
				String URL = "http://www.bing.com/images/search?q=" + UrlUtils.encode(s);
				launchURL(URL);
			};
		});
		
		return( true );
	}
	
	private static void launchURL(String s) {
		Program program = Program.findProgram(".html");
		if (program != null && program.getName().contains("Chrome")) {
			try {
				Field field = Program.class.getDeclaredField("command");
				field.setAccessible(true);
				String command = (String) field.get(program);
				command = command.replaceAll("%[1lL]", s);
				command = command.replace(" --", "");
				PluginInitializer.getDefaultInterface().getUtilities().createProcess(command + " -incognito");
			} catch (Exception e1) {
				e1.printStackTrace();
				Utils.launch(s);
			}
		} else {
			Utils.launch(s);
		}
	};
	
}
