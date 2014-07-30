/*
 * Created on May 23, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.gudy.azureus2.ui.console.multiuser.persist;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.ui.console.UserProfile;
import org.gudy.azureus2.ui.console.multiuser.UserManagerPersister;
import org.gudy.azureus2.ui.console.multiuser.UserManager.UserManagerConfig;

public class 
UserManagerXMLPersist 
	implements UserManagerPersister
{
		/**
		 * This code abstracted because it relies on java.beans.* which is not always available
		 * e.g. on Android
		 */
	
	public void 
	doSave( 
		OutputStream 	out,
		Map				usersMap )
	{
		UserManagerConfig config = new UserManagerConfig();
		List users = new ArrayList( usersMap.values() );
		config.setUsers(users);
		
		XMLEncoder encoder = new XMLEncoder( new BufferedOutputStream( out ) );
		encoder.writeObject(config);
		encoder.close();
	}
	
	public void 
	doLoad( 
		InputStream 	in,
		Map				usersMap )
	{
		XMLDecoder decoder = new XMLDecoder( in );
		UserManagerConfig managerConfig = (UserManagerConfig)decoder.readObject();
		for (Iterator iter = managerConfig.getUsers().iterator(); iter.hasNext();) {
			UserProfile user = (UserProfile) iter.next();
			usersMap.put(user.getUsername().toLowerCase(), user);
		}
		System.out.println("UserManager: registered " + usersMap.size() + " users");
		decoder.close();
	}
}
