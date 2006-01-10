/**
 * Created on 10-Jan-2006
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
package org.gudy.azureus2.pluginsimpl.remote;

import org.gudy.azureus2.plugins.PluginInterface;

public class GenericRPPluginInterface extends RPPluginInterface {

    public static RPPluginInterface create(PluginInterface _delegate) {
        GenericRPPluginInterface res = (GenericRPPluginInterface)_lookupLocal( _delegate);

        if (res == null) {
            res = new GenericRPPluginInterface(_delegate);
        }

        return res;
    }

    protected GenericRPPluginInterface(PluginInterface _delegate) {
        super(_delegate);
    }

    public RPReply _process(RPRequest request) {
        return GenericRPObject.processGenerically(this, request);
    }

    public String _getName() {
        return "PluginInterface";
    }

    public Class _getPluginClass() {
        return PluginInterface.class;
    }

}