/*
 * Created on Sep 4, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.ui.swt.views.table;

import java.util.List;

import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;

import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;

public interface 
TableViewSWTFiltered<T> 
{
	public void
	initialize(
		Composite		comp );
	
	public void
	filterChanged();
	
	public void
	setRowDefaultHeight(
		int				height );
	
	public void
	setHeaderVisible(
		boolean			visible );
	
	public void 
	addDataSources(
		T[] 			dataSources );

	public void 
	changeDataSources(
		T[] 			dataSources );

	public void 
	removeDataSources(
		T[] 			dataSources );

	public void
	removeAllDataSources();

	public List<T> 
	getSelectedDataSources();

	public void
	refresh(
		boolean 	force_sort );

	public void 
	addKeyListener(
		KeyListener 	listener );
	
	public void 
	addLifeCycleListener(
		TableLifeCycleListener listener );

	public void 
	addSelectionListener(
		TableSelectionListener 	listener, 
		boolean 				trigger );

	public void 
	addMenuFillListener(
		TableViewSWTMenuFillListener 	listener );
	
	public void
	delete();
	
	public interface
	Filter<S>
	{
		public boolean
		isVisible(
			S		data_source );
	}
}
