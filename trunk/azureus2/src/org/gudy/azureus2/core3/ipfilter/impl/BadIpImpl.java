/*
 * Created on 05-Jul-2004
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

package org.gudy.azureus2.core3.ipfilter.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.ipfilter.*;

public class 
BadIpImpl
	implements BadIp
{
	protected final String		ip;
	protected int			warning_count;
	protected long			last_time;
	
	protected
	BadIpImpl(
		String		_ip )
	{
		ip		= _ip;
	}
	
	protected int
	incrementWarnings()
	{
		last_time	= SystemTime.getCurrentTime();
		
		return( ++warning_count );
	}
	
	public String
	getIp()
	{
		return( ip );
	}
	
	public int
	getNumberOfWarnings()
	{
		return( warning_count );
	}
	
	public long
	getLastTime()
	{
		return( last_time );
	}
}
