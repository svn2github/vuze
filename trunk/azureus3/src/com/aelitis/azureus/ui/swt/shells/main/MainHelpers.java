/*
 * Created on Sep 14, 2012
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


package com.aelitis.azureus.ui.swt.shells.main;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class 
MainHelpers 
{
	private static boolean	done_xfer_bar;
	
	protected static void
	initTransferBar()
	{
		UIFunctionsSWT ui_functions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		
		if ( ui_functions == null ){
			
			return;
		}
		
		synchronized( MainHelpers.class ){
			
			if ( done_xfer_bar ){
				
				return;
			}
			
			done_xfer_bar = true;
		}
		
		if ( COConfigurationManager.getBooleanParameter("Open Transfer Bar On Start")){
			
			ui_functions.showGlobalTransferBar();
		}
	}
}
