/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.ui.swt.views;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * @author TuxPaper
 * @created Mar 6, 2015
 *
 */
public class MyTorrentsSubView
	extends MyTorrentsView
{

	public MyTorrentsSubView() {
		super("MyTorrentsSubView", false);
		neverShowCatOrTagButtons = true;
		isEmptyListOnNullDS = true;
		AzureusCore _azureus_core = AzureusCoreFactory.getSingleton();
		init(_azureus_core, "MyTorrentsSubView", Download.class,
				TableColumnCreator.createCompleteDM("MyTorrentsSubView"));
	}

}
