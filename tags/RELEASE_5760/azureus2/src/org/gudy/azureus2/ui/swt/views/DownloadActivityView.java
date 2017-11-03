/*
 * Created on 2 juil. 2003
 *
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

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.components.graphics.MultiPlotGraphic;
import org.gudy.azureus2.ui.swt.components.graphics.ValueFormater;
import org.gudy.azureus2.ui.swt.components.graphics.ValueSource;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListenerEx;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventImpl;

import com.aelitis.azureus.core.util.GeneralUtils;
import com.aelitis.azureus.core.util.average.MovingImmediateAverage;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;


/**
 * aka "Speed" sub view
 */
public class 
DownloadActivityView 
	implements UISWTViewCoreEventListenerEx, UIPluginViewToolBarListener
{
	public static final String MSGID_PREFIX = "DownloadActivityView";

	private static Color[]	colors = { 
		Colors.fadedGreen, Colors.fadedGreen, 
		Colors.blues[Colors.BLUES_DARKEST], Colors.blues[Colors.BLUES_DARKEST], 
		Colors.light_grey };
		
	private UISWTView 				swtView;
	private boolean					legend_at_bottom;
	private Composite				panel;
	private MultiPlotGraphic 		mpg;
	
	private DownloadManager 		manager;

	private Composite parent;
	
	public 
	DownloadActivityView()
	{
	}
	
	public boolean
	isCloneable()
	{
		return( true );
	}

	public UISWTViewCoreEventListener
	getClone()
	{
		return( new DownloadActivityView());
	}
	
	private String 
	getFullTitle() 
	{
		return( MessageText.getString(MSGID_PREFIX + ".title.full" ));
	}
	  
	public void 
	initialize(
		Composite parent )
	{
	    this.parent = parent;
			panel = new Composite(parent,SWT.NULL);
	    panel.setLayout(new GridLayout(legend_at_bottom?1:2, false));
	    fillPanel();
	}
	
	public void fillPanel() {
		Utils.disposeComposite(panel, false);

	    GridData gridData;

	    ValueFormater formatter =
	    	new ValueFormater() 
	    	{
	        	public String 
	        	format(
	        		int value) 
	        	{
	        		return DisplayFormatters.formatByteCountToKiBEtcPerSec( value );
	        	}
	    	};
	      
	    
	    final ValueSourceImpl[] sources = {
	    	new ValueSourceImpl( "Up", 0, colors, true, false, false )
	    	{
	    		public int
	    		getValue()
	    		{
	    			DownloadManager dm = manager;
	    			
	    			if ( dm == null ){
	    				
	    				return( 0 );
	    			}
	    			
	    			DownloadManagerStats stats = manager.getStats();
	    			
	    			return((int)(stats.getDataSendRate()));
	    		}
	    	},
	    	new ValueSourceImpl( "Up Smooth", 1, colors, true, false, true )
	    	{
	    		public int
	    		getValue()
	    		{
	    			DownloadManager dm = manager;
	    			
	    			if ( dm == null ){
	    				
	    				return( 0 );
	    			}
	    			
	    			DownloadManagerStats stats = manager.getStats();
	    			
	    			return((int)(stats.getSmoothedDataSendRate()));
	    		}
	    	},
	    	new ValueSourceImpl( "Down", 2, colors, false, false, false )
	    	{
	    		public int
	    		getValue()
	    		{
	    			DownloadManager dm = manager;
	    			
	    			if ( dm == null ){
	    				
	    				return( 0 );
	    			}
	    			
	    			DownloadManagerStats stats = manager.getStats();
	    			
	    			return((int)(stats.getDataReceiveRate()));
	    		}
	    	},
	    	new ValueSourceImpl( "Down Smooth", 3, colors, false, false, true )
	    	{
	    		public int
	    		getValue()
	    		{
	    			DownloadManager dm = manager;
	    			
	    			if ( dm == null ){
	    				
	    				return( 0 );
	    			}
	    			
	    			DownloadManagerStats stats = manager.getStats();
	    			
	    			return((int)(stats.getSmoothedDataReceiveRate()));
	    		}
	    	},
	    	new ValueSourceImpl( "Swarm Peer Average", 4, colors, false, true, false )
	    	{
	    		public int
	    		getValue()
	    		{
	    			DownloadManager dm = manager;
	    			
	    			if ( dm == null ){
	    				
	    				return( 0 );
	    			}
	    				    			
	    			return((int)(manager.getStats().getTotalAveragePerPeer()));
	    		}
	    	}
	    };
	    
		final MultiPlotGraphic f_mpg = mpg = MultiPlotGraphic.getInstance( sources, formatter );
	    
	    
		String[] color_configs = new String[] {
				"DownloadActivityView.legend.up",
				"DownloadActivityView.legend.up_smooth",
				"DownloadActivityView.legend.down",
				"DownloadActivityView.legend.down_smooth",
				"DownloadActivityView.legend.peeraverage",
			};

		Legend.LegendListener legend_listener = 
			new Legend.LegendListener()
			{
				private int	hover_index = -1;
				
				public void 
				hoverChange(
					boolean 	entry, 
					int 		index ) 
				{
					if ( hover_index != -1 ){
						
						sources[hover_index].setHover( false );
					}
					
					if ( entry ){
						
						hover_index = index;
						
						sources[index].setHover( true );
					}
										
					f_mpg.refresh( true );
				}
				
				public void
				visibilityChange(
					boolean	visible,
					int		index )
				{
					sources[index].setVisible( visible );

					f_mpg.refresh( true );
				}
			};
			
		
		if ( !legend_at_bottom ){
				
			gridData = new GridData( GridData.FILL_VERTICAL );
			gridData.verticalAlignment = SWT.CENTER;
			
			Legend.createLegendComposite(panel, colors, color_configs, null, gridData, false, legend_listener );
		}

	    Composite gSpeed = new Composite(panel,SWT.NULL);
	    gridData = new GridData(GridData.FILL_BOTH);
	    gSpeed.setLayoutData(gridData);    
	    gSpeed.setLayout(new GridLayout());
	     
	    if ( legend_at_bottom ){
	    	
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			
			Legend.createLegendComposite(panel, colors, color_configs, null, gridData, true, legend_listener );

	    }
	    
	    Canvas speedCanvas = new Canvas(gSpeed,SWT.NO_BACKGROUND);
	    gridData = new GridData(GridData.FILL_BOTH);
	    speedCanvas.setLayoutData(gridData);

		mpg.initialize( speedCanvas, false );
	}
	
	private void
	refresh(
		boolean	force )
	{
		if (mpg == null) {
			return;
		}
		mpg.refresh( force );
	}
	
	public Composite 
	getComposite() 
	{
		return( panel );
	}
	
	private boolean comp_focused;
	private Object focus_pending_ds;

	private void
	setFocused( boolean foc )
	{
		if ( foc ){

			comp_focused = true;

			dataSourceChanged( focus_pending_ds );

		}else{

			focus_pending_ds = manager;

			dataSourceChanged( null );

			comp_focused = false;
		}
	}
	
	public void 
	dataSourceChanged(
		Object newDataSource ) 
	{
		if ( !comp_focused ){
			focus_pending_ds = newDataSource;
			return;
		}
		  
		DownloadManager newManager = ViewUtils.getDownloadManagerFromDataSource( newDataSource );
	
		if (newManager == manager) {
			return;
		}

		manager = newManager;
		
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (panel == null || panel.isDisposed()) {
					return;
				}
				Utils.disposeComposite(panel, false);
			  if (manager != null){
			  	fillPanel();
			  	parent.layout(true, true);
			  } else {
			  	ViewUtils.setViewRequiresOneDownload(panel);
			  }
			}
		});
		
		if ( manager == null ){
			
			mpg.setActive( false );
			
			mpg.reset( new int[5][0] );
		
		}else{
		
			DownloadManagerStats stats = manager.getStats();
			
			stats.setRecentHistoryRetention( true );
			
			int[][] _history = stats.getRecentHistory();
			
				// reconstitute the smoothed values to the best of our ability (good enough unless we decide we want
				// to throw more memory at remembering this more accurately...)
			
			int[] send_history = _history[0];
			int[] recv_history = _history[1];
			
			int	history_secs = send_history.length;
			
			int[] smoothed_send = new int[history_secs];
			int[] smoothed_recv = new int[history_secs];
		
			MovingImmediateAverage	send_average = GeneralUtils.getSmoothAverage();
			MovingImmediateAverage	recv_average = GeneralUtils.getSmoothAverage();
			
			int smooth_interval = GeneralUtils.getSmoothUpdateInterval();
			
			int	current_smooth_send = 0;
			int	current_smooth_recv = 0;
			int	pending_smooth_send = 0;
			int	pending_smooth_recv = 0;
			
			for ( int i=0;i<history_secs;i++){
				pending_smooth_send += send_history[i];
				pending_smooth_recv += recv_history[i];
				
				if ( i % smooth_interval == 0 ){
					current_smooth_send = (int)(send_average.update( pending_smooth_send )/smooth_interval);
					current_smooth_recv = (int)(recv_average.update( pending_smooth_recv )/smooth_interval);
					
					pending_smooth_send = 0;
					pending_smooth_recv = 0;
				}
				smoothed_send[i] = current_smooth_send;
				smoothed_recv[i] = current_smooth_recv;
			}
			
			int[][] history = {send_history, smoothed_send, recv_history, smoothed_recv, _history[2] };
			
			mpg.reset( history );
			
			mpg.setActive( true );
		}
	}
	
	public void 
	delete()
	{
		 Utils.disposeComposite( panel );
		 
		 if ( mpg != null ){
		 
			 mpg.dispose();
		 }
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
	    switch( event.getType()){
		    case UISWTViewEvent.TYPE_CREATE:{
		    	swtView = event.getView();
		    	
		    	swtView.setTitle(getFullTitle());
		    	
		    	swtView.setToolBarListener(this);
		    	
		    	if ( event instanceof UISWTViewEventImpl ){

		    		String parent = ((UISWTViewEventImpl)event).getParentID();

		    		legend_at_bottom = parent != null && parent.equals( UISWTInstance.VIEW_TORRENT_DETAILS );
		    	}
		    	 
		    	break;
		    }
		    case UISWTViewEvent.TYPE_DESTROY:{
		    	
		    	delete();
		    	
		    	break;
		    }
		    case UISWTViewEvent.TYPE_INITIALIZE:{
		    	
		    	initialize((Composite)event.getData());
		    	
		    	break;
		    }
		    case UISWTViewEvent.TYPE_REFRESH:{
		     
		    	refresh( false );
		    	
		        break;
		    }
		    case UISWTViewEvent.TYPE_LANGUAGEUPDATE:{
		    	Messages.updateLanguageForControl(getComposite());
		    	
		    	swtView.setTitle(getFullTitle());
		    	
		    	break;
		    }
		    case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:{
		    	
		    	dataSourceChanged(event.getData());
		    	
		    	break;
		    }
	    	case UISWTViewEvent.TYPE_FOCUSGAINED:{
	    		
	    		String id = "DMDetails_DownloadGraph";

			    setFocused( true );	// do this here to pick up corrent manager before rest of code

	    		if ( manager != null ){

	    			if ( manager.getTorrent() != null ){

	    				id += "." + manager.getInternalName();

	    			}else{

	    				id += ":" + manager.getSize();
	    			}

	    			SelectedContentManager.changeCurrentlySelectedContent(id,
								new SelectedContent[] {
									new SelectedContent(manager)
						});
					} else {
						SelectedContentManager.changeCurrentlySelectedContent(id, null);
					}

	    		refresh( true );
	    					    
			    break;
	    	}
		    case UISWTViewEvent.TYPE_FOCUSLOST:{
		    	
		    	setFocused( false );
		    	
		    	SelectedContentManager.clearCurrentlySelectedContent();

		    	break;
		    }
	    }
	    
	    return( true );
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	 */
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		return false; // default handler will handle it
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		Map<String, Long> states = TorrentUtil.calculateToolbarStates(
				SelectedContentManager.getCurrentlySelectedContent(), null);
		list.putAll(states);
	}
	
	private abstract static class
	ValueSourceImpl
		implements ValueSource
	{	
		private String			name;
		private int				index;
		private Color[]			colours;
		private boolean			is_up;
		private boolean			trimmable;
		
		private boolean			is_hover;
		private boolean			is_invisible;
		private boolean			is_dotted;
		
		private
		ValueSourceImpl(
			String					_name,
			int						_index,
			Color[]					_colours,
			boolean					_is_up,
			boolean					_trimmable,
			boolean					_is_dotted )
		{
			name			= _name;
			index			= _index;
			colours			= _colours;
			is_up			= _is_up;
			trimmable		= _trimmable;
			is_dotted		= _is_dotted;
		}
			
		public String
		getName()
		{
			return( name );
		}
		
		public Color 
		getLineColor() 
		{
			return( colours[index] );
		}
		
		public boolean
		isTrimmable()
		{
			return( trimmable );
		}
		
		private void
		setHover(
			boolean	h )
		{
			is_hover = h;
		}
		
		private void
		setVisible(
			boolean	visible )
		{
			is_invisible = !visible;
		}
		
		public int 
		getStyle() 
		{
			if ( is_invisible ){
				
				return( STYLE_INVISIBLE );
			}
			
			int	style = is_up?STYLE_UP:STYLE_DOWN;
			
			if ( is_hover ){
				
				style |= STYLE_BOLD; 
			}
			
			if ( is_dotted ){
				
				style |= STYLE_HIDE_LABEL;
			}
			
			return( style );
		}
		
		public int 
		getAlpha() 
		{
			return( is_dotted?128:255 );
		}
	}
}
