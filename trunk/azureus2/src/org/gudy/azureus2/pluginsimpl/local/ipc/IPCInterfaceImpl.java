/*
 * Created on 02-May-2006
 * Created by Damokles
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.ipc;

import java.lang.reflect.Method;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

/**
 * @author Damokles
 *
 */

public class IPCInterfaceImpl implements IPCInterface {

	Plugin target;


	public IPCInterfaceImpl ( Plugin target ) {
		this.target = target;
	}

	public Object invoke( String methodName, Object[] params )
	throws IPCException {

		try {
			Class[] paramTypes = new Class[params.length];
			for (int i=0;i<params.length;i++) {
				if (params[i] instanceof Boolean) {
					paramTypes[i] = boolean.class;
				} else if (params[i] instanceof Integer) {
					paramTypes[i] = int.class;
				} else if (params[i] instanceof Long) {
					paramTypes[i] = long.class;
				} else if (params[i] instanceof Float) {
					paramTypes[i] = float.class;
				} else if (params[i] instanceof Double) {
					paramTypes[i] = double.class;
				} else if (params[i] instanceof Byte) {
					paramTypes[i] = byte.class;
				} else if (params[i] instanceof Character) {
					paramTypes[i] = char.class;
				} else if (params[i] instanceof Short) {
					paramTypes[i] = short.class;
				} else
					paramTypes[i] = params[i].getClass();
			}
			Method mtd = target.getClass().getMethod(methodName,paramTypes);
			return mtd.invoke(target, params);
		} catch (Exception e) {
			throw new IPCException(e);
		}
	}
}
