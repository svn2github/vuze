/*
 * Created on Oct 2, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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


package com.aelitis.azureus.ui.swt.browser;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class 
BrowserWrapper 
{
	private Browser		browser;
	
	public
	BrowserWrapper(
		Composite		composite,
		int				style )
	{
		browser = new Browser( composite, style );
	}
	
	public 
	BrowserWrapper(
		Browser		_browser )
	{
		browser = _browser;
	}
	
	public Browser
	getBrowser()
	{
		return( browser );
	}
	
	public void
	setVisible(
		boolean		visible )
	{
		browser.setVisible( visible );
	}
	
	public boolean
	isVisible()
	{
		return( browser.isVisible());
	}
	
	public boolean
	isDisposed()
	{
		return( browser.isDisposed());
	}
	
	public void
	dispose()
	{
		browser.dispose();
	}
	
	public boolean
	execute(
		String		str )
	{
		//System.out.println( "execute: " + str );
		
		return( browser.execute( str ));
	}
	
	public boolean
	isBackEnabled()
	{
		return( browser.isBackEnabled());
	}
	
	public String
	getUrl()
	{
		return( browser.getUrl());
	}
	
	public void
	setUrl(
		String		url )
	{
		browser.setUrl( url );
	}
	
	public void
	setText(
		String		text )
	{
		browser.setText( text );
	}
	
	public void
	setData(
		String		key,
		Object		value )
	{
		browser.setData(key, value);
	}

	public Object
	getData(
		String	key )
	{
		return( browser.getData( key ));
	}
	
	public void
	back()
	{
		browser.back();
	}
	
	public void
	refresh()
	{
		browser.refresh();
	}
	
	public void
	update()
	{
		browser.update();
	}
	
	public Shell
	getShell()
	{
		return( browser.getShell());
	}
	
	public Display
	getDisplay()
	{
		return( browser.getDisplay());
	}
	
	public Composite
	getParent()
	{
		return( browser.getParent());
	}
	
	public Object
	getLayoutData()
	{
		return( browser.getLayoutData());
	}
	
	public void
	setLayoutData(
		Object	data )
	{
		browser.setLayoutData( data );
	}
	
	public void
	setFocus()
	{
		browser.setFocus();
	}
	
	public void
	addListener(
		int			type,
		Listener	l )
	{
		browser.addListener( type, l );
	}
	
	public void
	addLocationListener(
		LocationListener		l )
	{
		browser.addLocationListener( l );
	}
	
	public void
	removeLocationListener(
		LocationListener		l )
	{
		browser.removeLocationListener( l );
	}
	
	public void
	addTitleListener(
		TitleListener		l )
	{
		browser.addTitleListener( l );
	}
	
	public void
	addProgressListener(
		ProgressListener		l )
	{
		browser.addProgressListener( l );
	}
	
	public void
	removeProgressListener(
		ProgressListener		l )
	{
		browser.removeProgressListener( l );
	}
	
	public void
	addOpenWindowListener(
		OpenWindowListener		l )
	{
		browser.addOpenWindowListener( l );
	}
	
	public void
	addCloseWindowListener(
		CloseWindowListener		l )
	{
		browser.addCloseWindowListener( l );
	}
	
	public void
	addDisposeListener(
		DisposeListener		l )
	{
		browser.addDisposeListener( l );
	}
	
	public void
	removeDisposeListener(
		DisposeListener		l )
	{
		browser.removeDisposeListener( l );
	}
	
	public void
	addStatusTextListener(
		StatusTextListener		l )
	{
		browser.addStatusTextListener( l );
	}
	
	public void
	removeStatusTextListener(
		StatusTextListener		l )
	{
		browser.removeStatusTextListener( l );
	}
}
