/*
 * Created on Apr 1, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
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


package org.gudy.azureus2.ui.swt;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;


public class 
BrowserWrapperFake
	extends BrowserWrapper
{
	private Composite		parent;
	
	private Composite		browser;
	private Label			link_label;
	private Label			description_label;
	
	private String 			url;
	private String			description;
	
	private List<LocationListener>		location_listeners 	= new ArrayList<LocationListener>();
	private List<ProgressListener>		progress_listeners 	= new ArrayList<ProgressListener>();
	private List<TitleListener>			title_listeners 	= new ArrayList<TitleListener>();
	
	protected
	BrowserWrapperFake(
		Composite		_parent,
		int				style )
	{
		parent	= _parent;
		
		browser = new Composite( parent, SWT.NULL );
		browser.setBackground( Colors.white );
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		browser.setLayout(layout);
		
		Label label = new Label(browser, SWT.WRAP);
		Messages.setLanguageText(label, "browser.internal.disabled.info");
		label.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ));
		label.setBackground( Colors.white );

		Composite details = new Composite( browser, SWT.BORDER );
		layout = new GridLayout();
		layout.numColumns = 2;
		details.setLayout(layout);
		details.setLayoutData( new GridData( GridData.FILL_BOTH ));
		details.setBackground( Colors.white );
		
			// url
		
		label = new Label(details, SWT.NULL );
		label.setText( "URL" );
		label.setLayoutData( new GridData());
		label.setBackground( Colors.white );
		
		
		link_label = new Label(details, SWT.NULL);
		link_label.setText( MessageText.getString( "ConfigView.label.please.visit.here" ));
		
		link_label.setCursor(link_label.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		link_label.setForeground(Colors.blue);
		link_label.setBackground( Colors.white );
		
		link_label.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				Utils.launch( url );
			}
			public void mouseUp(MouseEvent e) {
				
				if ( e.button == 1 && e.stateMask != SWT.CONTROL){
				
					Utils.launch( url );
				}
			}
		});
		
		GridData grid_data = new GridData(GridData.FILL_HORIZONTAL);
		grid_data.horizontalIndent = 10;
		link_label.setLayoutData(grid_data);
		
		ClipboardCopy.addCopyToClipMenu( 
			link_label,
			new ClipboardCopy.copyToClipProvider() {
				
				public String getText() {
					return( url );
				}
			});
		
			// desc
		
		label = new Label(details, SWT.NULL );
		Messages.setLanguageText(label, "columnChooser.columndescription" );
		label.setLayoutData( new GridData());
		label.setBackground( Colors.white );

		description_label = new Label(details, SWT.NULL );
		description_label.setText( "" );
		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		grid_data.horizontalIndent = 10;
		description_label.setLayoutData( grid_data );
		description_label.setBackground( Colors.white );
	}
	
	public boolean
	isFake()
	{
		return( true );
	}
	
	public Composite
	getControl()
	{
		return( browser );
	}
	
	public void
	setBrowser(
		WindowEvent		event )
	{
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
		return( false );
	}
	
	public boolean
	isBackEnabled()
	{
		return( false );
	}
	
	public String
	getUrl()
	{
		return( url );
	}
	
	public void
	setUrl(
		final String		_url )
	{
		url		= _url;
		
		Utils.execSWTThread(
			new Runnable()
			{				
				public void 
				run() 
				{
					String url_str = _url;
					
					int	pos = url_str.indexOf( '?' );
					
					if ( pos != -1 ){
						
						url_str = url_str.substring( 0, pos );
					}
					
					link_label.setText( url_str );

					browser.layout();
					
					for ( LocationListener l: location_listeners ){
						
						try{
							LocationEvent event = new LocationEvent( browser );
							
							event.top 		= true;
							event.location 	= _url;
							
							l.changed( event );
							
						}catch( Throwable e){
							
							Debug.out( e );
						}
					}
					
					for ( ProgressListener l: progress_listeners ){
						
						try{
							ProgressEvent event = new ProgressEvent( browser );
							
							l.completed( event );
							
						}catch( Throwable e){
							
							Debug.out( e );
						}
					}
					
					for ( TitleListener l: title_listeners ){
						
						try{
							TitleEvent event = new TitleEvent( browser );
							
							event.title = "Browser Disabled";
							
							l.changed( event );
							
						}catch( Throwable e){
							
							Debug.out( e );
						}
					}
				}
			});
	}
	
	public void
	setText(
		String		text )
	{
		description	= text;
				
		Utils.execSWTThread(
			new Runnable()
			{				
				public void 
				run() 
				{					
					description_label.setText( description );

					browser.layout();
				}
			});
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
	}
	
	public void
	refresh()
	{
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
		location_listeners.add( l );
	}
	
	public void
	removeLocationListener(
		LocationListener		l )
	{
		location_listeners.remove( l );
	}
	
	public void
	addTitleListener(
		TitleListener		l )
	{
		title_listeners.add( l );
	}
	
	public void
	addProgressListener(
		ProgressListener		l )
	{
		progress_listeners.add( l );
	}
	
	public void
	removeProgressListener(
		ProgressListener		l )
	{
		progress_listeners.remove( l );
	}
	
	public void
	addOpenWindowListener(
		OpenWindowListener		l )
	{
	}
	
	public void
	addCloseWindowListener(
		CloseWindowListener		l )
	{
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
	}
	
	public void
	removeStatusTextListener(
		StatusTextListener		l )
	{
	}
	
	public BrowserFunction
	addBrowserFunction(
		String						name,
		final BrowserFunction		bf )
	{	
		return( new BrowserFunctionFake( bf ));
	}
	
	public static class
	BrowserFunctionFake
		extends BrowserFunction
	{
		private final BrowserFunction	bf;
		
		private boolean	disposed;
		
		private 
		BrowserFunctionFake(
			BrowserFunction	_bf )
		{
			bf		= _bf;
			
			bf.bind( this );
		}
		
		public Object 
		function(
			Object[] arguments )
		{
			return( bf.function( arguments ));
		}

		public boolean
		isDisposed()
		{
			return( disposed );
		}
		
		public void
		dispose()
		{
			disposed	= true;
		}
	}
}
