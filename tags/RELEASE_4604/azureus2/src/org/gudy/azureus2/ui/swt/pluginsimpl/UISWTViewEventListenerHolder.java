/*
 * Created on Oct 19, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

public class 
UISWTViewEventListenerHolder
	implements UISWTViewEventListener
{
	private UISWTViewEventListener		listener;
	private Reference<PluginInterface>	pi;
	
	protected
	UISWTViewEventListenerHolder(
		UISWTViewEventListener			_listener,
		PluginInterface					_pi )
	{
		listener	= _listener;
		
		if ( _pi == null ){
			
			if ( listener instanceof BasicPluginViewImpl ){
				
				_pi = ((BasicPluginViewImpl)listener).getModel().getPluginInterface();
			}
		}
		
		if ( _pi != null ){
					
			pi = new WeakReference<PluginInterface>( _pi );
		}
	}
	
	public boolean
	isLogView()
	{
		return( listener instanceof BasicPluginViewImpl );
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( pi==null?null:pi.get());
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
		return( listener.eventOccurred( event ));
	}
}
