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


package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.*;

import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTFiltered;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;

import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;

public class 
TableViewSWTFilteredImpl<T> 
	implements TableViewSWTFiltered<T>
{
	private TableViewSWTImpl<T>			basis;
	private Filter<T>					filter;
	
	private Set<T>	all_data_sources = new HashSet<T>();
	
	private FrequencyLimitedDispatcher	dispatcher = 
		new FrequencyLimitedDispatcher(
			new AERunnable()
			{
				public void
				runSupport()
				{
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								filterChangedSupport();
							}
						});
				}
			},
			300 );
	
	public 
	TableViewSWTFilteredImpl(
		TableViewSWTImpl<T>		_basis,
		Filter<T>				_filter )
	{
		basis	= _basis;
		filter	= _filter;
	}
	
	public void 
	filterChanged() 
	{
		dispatcher.dispatch();
	}
	
	protected void
	filterChangedSupport()
	{
		Set<T>  existing		= new HashSet<T>( basis.getDataSources());
		List<T> list_removes 	= new ArrayList<T>();
		List<T> list_adds 		= new ArrayList<T>();
		
		for( T ds: all_data_sources ){
			
			boolean have_it = existing.contains( ds );
			
			if ( filter.isVisible( ds )){
				
				if ( !have_it ){
					
					list_adds.add( ds );
				}
			}else{
				
				if ( have_it ){
					
					list_removes.add( ds );
				}
			}
		}
		
		basis.removeDataSources((T[])list_removes.toArray());
		
		basis.addDataSources((T[])list_adds.toArray());
		
		basis.processDataSourceQueue();
	}
	
	public void
	initialize(
		Composite		comp )
	{
		basis.initialize( comp );
	}
	
	public void
	setRowDefaultHeight(
		int				height )
	{
		basis.setRowDefaultHeight( height );
	}
	
	public void
	setHeaderVisible(
		boolean			visible )
	{
		basis.setHeaderVisible( visible );
	}
	
	public void 
	addDataSources(
		T[] 			data_sources )
	{	
		if ( !basis.isDisposed()){

			all_data_sources.addAll( Arrays.asList( data_sources ));
			
			for (int i=0;i<data_sources.length;i++){
				
				if ( !filter.isVisible( data_sources[i] )){
					
					data_sources[i] = null;
				}
			}
			
			basis.addDataSources( data_sources );
		}
	}
	
	public void 
	changeDataSources(
		T[] 			data_sources )
	{
		if ( !basis.isDisposed()){
			
			for ( T ds: data_sources ){
				
				TableRowCore row = basis.getRow( ds );
				
				if ( row != null ){
			
					row.refresh( true );
				}
			}
		}
	}
	
	public void 
	removeDataSources(
		T[] 			data_sources )
	{
		if ( !basis.isDisposed()){

			all_data_sources.removeAll( Arrays.asList( data_sources ));
			
			basis.removeDataSources( data_sources );
		}
	}
	
	public void
	refresh(
		boolean 	force_sort )
	{
		if ( !basis.isDisposed()){
			
			basis.refreshTable( force_sort );
		}
	}
	
	public void
	removeAllDataSources()
	{
		if ( !basis.isDisposed()){
			
			basis.removeAllTableRows();
		}
	}

	public List<T> 
	getSelectedDataSources()
	{
		return( basis.getSelectedDataSources());
	}

	public void 
	addKeyListener(
		KeyListener 	listener )
	{
		basis.addKeyListener( listener );
	}

	public void 
	addLifeCycleListener(
		TableLifeCycleListener listener )
	{
		basis.addLifeCycleListener( listener );
	}

	public void 
	addSelectionListener(
		TableSelectionListener 	listener, 
		boolean 				trigger )
	{
		basis.addSelectionListener( listener, trigger );
	}

	public void 
	addMenuFillListener(
		TableViewSWTMenuFillListener 	listener )
	{
		basis.addMenuFillListener( listener );
	}
	
	public void
	delete()
	{
		basis.delete();
	}
}
