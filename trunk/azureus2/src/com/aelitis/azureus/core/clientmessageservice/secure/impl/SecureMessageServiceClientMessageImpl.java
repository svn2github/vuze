/*
 * Created on 07-Nov-2005
 * Created by Paul Gardner
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.core.clientmessageservice.secure.impl;

import java.util.Map;

import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClientMessage;

public class 
SecureMessageServiceClientMessageImpl
	implements SecureMessageServiceClientMessage
{
	private final SecureMessageServiceClientImpl		service;
	private final Map					request;
	private Map					reply;
	private final Object				client_data;
	private final String				description;
	
	protected
	SecureMessageServiceClientMessageImpl(
		SecureMessageServiceClientImpl	_service,
		Map				_content,
		Object			_data,
		String			_description )
	{
		service			= _service;
		request			= _content;
		client_data		= _data;
		description		= _description;
	}
		
	public Map
	getRequest()
	{
		return( request );
	}
	
	protected void
	setReply(
		Map		_reply )
	{
		reply	= _reply;
	}
	
	public Map
	getReply()
	{
		return( reply );
	}
	
	public Object
	getClientData()
	{
		return( client_data );
	}
	
	public void
	cancel()
	{
		service.cancel( this );
	}
	
	public String
	getString()
	{
		return( description );
	}
}
