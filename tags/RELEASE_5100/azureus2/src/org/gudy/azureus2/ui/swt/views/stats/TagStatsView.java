/*
 * Created on May 1, 2013
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

package org.gudy.azureus2.ui.swt.views.stats;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;


import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.components.graphics.MultiPlotGraphic;
import org.gudy.azureus2.ui.swt.components.graphics.ValueFormater;
import org.gudy.azureus2.ui.swt.components.graphics.ValueSource;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.ui.swt.utils.ColorCache;


public class TagStatsView 
	extends TagTypeAdapter
	implements UISWTViewCoreEventListener, TagManagerListener
{	
	public static final String MSGID_PREFIX = "TagStatsView";
  
	private Composite 			panel;
	private Group	 			legend_panel;
	private ScrolledComposite	legend_panel_sc;
  
	private Composite			speed_panel;
	
	private UISWTView swtView;  
  
	private MultiPlotGraphic 		mpg;
	
	
	public 
	TagStatsView() 
	{
	}
  
 
	public void 
	periodicUpdate() 
	{

	}
  
	private void 
	initialize(
		Composite composite) 
	{
	    panel = new Composite(composite,SWT.NULL);	    
	    panel.setLayout(new GridLayout(2, false));
	    
	    legend_panel_sc = new ScrolledComposite(panel, SWT.V_SCROLL );
	    legend_panel_sc.setExpandHorizontal(true);
	    legend_panel_sc.setExpandVertical(true);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		legend_panel_sc.setLayout(layout);
		GridData gridData = new GridData(GridData.FILL_VERTICAL );
		legend_panel_sc.setLayoutData(gridData);
		
		legend_panel = new Group( legend_panel_sc, SWT.NULL );
		legend_panel.setText( MessageText.getString( "label.tags" ));
		
		legend_panel.setLayout(new GridLayout());

		legend_panel_sc.setContent(legend_panel);
		legend_panel_sc.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				legend_panel_sc.setMinSize(legend_panel.computeSize(SWT.DEFAULT, SWT.DEFAULT ));
			}
		});
	    
		speed_panel = new Composite( panel, SWT.NULL );
		speed_panel.setLayout(new GridLayout());
		gridData = new GridData(GridData.FILL_BOTH );
		speed_panel.setLayoutData(gridData);
		
	    build();
    
	    TagManager tm = TagManagerFactory.getTagManager();
	    
	    tm.addTagManagerListener( this, false );
	    
	    for ( TagType tt: tm.getTagTypes()){
	    	
	    	tt.addTagTypeListener( this, false );
	    }
	    
		panel.addListener( 
			SWT.Activate,
			new Listener()
			{
				public void 
				handleEvent(
					Event event )
				{
					refresh(true);
				}
			});
	}
  
	private void
	build()
	{
		if ( legend_panel == null || legend_panel.isDisposed()){
			
			return;
		}
		
		for ( Control c: legend_panel.getChildren()){
			
			c.dispose();
		}
		
		List<String>	configs 		= new ArrayList<String>();
		List<String>	texts			= new ArrayList<String>();	
		List<Color>		colors			= new ArrayList<Color>();
		
		TagManager tm = TagManagerFactory.getTagManager();
		
		List<TagType> tag_types = tm.getTagTypes();
		
		tag_types	= TagUIUtils.sortTagTypes( tag_types );
		
		List<TagFeatureRateLimit>	visible_tags = new ArrayList<TagFeatureRateLimit>();
				
		for ( TagType tag_type: tag_types ){
						
			if ( tag_type.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )){
				
				List<Tag> tags = tag_type.getTags();
				
				tags = TagUIUtils.sortTags( tags );
				
				for ( Tag tag: tags ){
					
					if ( !tag.isVisible()){
						
						continue;
					}
					
					TagFeatureRateLimit rl = (TagFeatureRateLimit)tag;

					if ( !rl.supportsTagRates()){
						
						continue;
					}
					
					String	config_key = "TagStatsView.cc." + tag_type.getTagType() + "." + tag.getTagID();
					
					configs.add( config_key );
					
					texts.add( tag.getTagName( true ));
					
					Color tt_colour;
					
					int[]	rgb = tag.getColor();
					
					if ( rgb == null ){
					
						tt_colour = Colors.blues[ Colors.BLUES_DARKEST];
             
					}else{
						
						tt_colour = ColorCache.getColor( legend_panel.getDisplay(), rgb );
					}
					
					colors.add( tt_colour );
					
					visible_tags.add( rl );
				}
			}
		}
		
		
		final Color[]		color_array = colors.toArray( new Color[ colors.size()]);
		final String[]		text_array = texts.toArray( new String[ texts.size()]);

	    final List<ValueSourceImpl>	sources = new ArrayList<ValueSourceImpl>();
	    	    
		List<int[]>	history_records 	= new ArrayList<int[]>();
		int			history_record_max	= 0;
		
	    for ( int i=0;i<visible_tags.size();i++ ){
	    	
	    	final TagFeatureRateLimit tag = visible_tags.get(i);

			tag.setRecentHistoryRetention( true );
			
			int[][] history = tag.getRecentHistory();
			
			history_record_max = Math.max( history[0].length, history_record_max );
			
			history_records.add( history[0] );
			history_records.add( history[1] );
	    	
	    	sources.add( new ValueSourceImpl( tag, text_array[i], i, color_array, true ));
	    	sources.add( new ValueSourceImpl( tag, text_array[i], i, color_array, false ));
	    };
		
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
	      

		
		if ( mpg != null ){
			
			mpg.dispose();
		}

		final MultiPlotGraphic f_mpg = mpg = MultiPlotGraphic.getInstance( sources.toArray( new ValueSource[ sources.size()]), formatter );

		int[][] history = new int[history_records.size()][];
		
		for ( int i=0;i<history.length;i++){
			int[] 	hist 		= history_records.get(i);
			int		hist_len 	= hist.length;
			
			if ( hist_len == history_record_max ){
				history[i] = hist;
			}else{
				int[] temp = new int[history_record_max];
				System.arraycopy( hist, 0, temp, history_record_max-hist_len, hist_len );
				history[i] = temp;
			}
		}
		
		mpg.reset( history );
		
	    GridData gridData;
	    		
		if ( color_array.length > 0 ){
		
			gridData = new GridData( GridData.FILL_VERTICAL );
			gridData.verticalAlignment = SWT.CENTER;

			Legend.createLegendComposite(
				legend_panel, 
				color_array, 
				configs.toArray( new String[ configs.size()]), 
				text_array,
				gridData, 
				false,
				new Legend.LegendListener()
				{
					private int	hover_index = -1;
					
					public void 
					hoverChange(
						boolean 	entry, 
						int 		index ) 
					{
						if ( hover_index != -1 ){
							
							for ( int i=hover_index*2;i<hover_index*2+2;i++){
								
								sources.get(i).setHover( false );
							}
						}
						
						if ( entry ){
							
							hover_index = index;
							
							for ( int i=hover_index*2;i<hover_index*2+2;i++){
								
								sources.get(i).setHover( true );
							}
						}
											
						f_mpg.refresh( true );
					}
					
					public void
					visibilityChange(
						boolean	visible,
						int		index )
					{
						for ( int i=index*2;i<index*2+2;i++){
							
							sources.get(i).setVisible( visible );
						}
						
						f_mpg.refresh( true );
					}
				});
		}else{

			gridData = new GridData( GridData.FILL_HORIZONTAL );
			gridData.verticalAlignment = SWT.TOP;

			Label lab = new Label( legend_panel, SWT.NULL );
			lab.setText( MessageText.getString( "tag.stats.none.defined" ));
			
			lab.setLayoutData( gridData );
		}
		
		legend_panel_sc.setMinSize(legend_panel.computeSize(SWT.DEFAULT, SWT.DEFAULT ));
		
			// speed
		
		for ( Control c: speed_panel.getChildren()){
			
			c.dispose();
		}
				
	    Canvas speed_canvas = new Canvas( speed_panel,SWT.NO_BACKGROUND);
	    gridData = new GridData(GridData.FILL_BOTH);
	    speed_canvas.setLayoutData(gridData);


		mpg.initialize( speed_canvas, true );
		
	    panel.layout( true, true );
	    
	}
	
	private void
	rebuild()
	{
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					build();
				}
			});
	}
	
	private void
	rebuild(
		TagType		tag_type,
		boolean		added )
	{
		if ( panel == null || panel.isDisposed()){
			
			tag_type.getTagManager().removeTagManagerListener( this );
		
			return;
		}
		
		if ( added ){
			
			tag_type.addTagTypeListener( this, false );
		}
		
		rebuild();
	}
	
	public void
	tagTypeAdded(
		TagManager		manager,
		TagType			tag_type )
	{
		rebuild( tag_type, true );
	}
	
	public void
	tagTypeRemoved(
		TagManager		manager,
		TagType			tag_type )
	{		
		rebuild( tag_type, false );
	}
	
	private void
	rebuild(
		Tag			tag )
	{
		if ( panel == null || panel.isDisposed()){
			
			TagType tt = tag.getTagType();
			
			tt.removeTagTypeListener( this );
			
			tt.getTagManager().removeTagManagerListener( this );
			
			return;
		}
		
		rebuild();
	}
	
	public void
	tagAdded(
		Tag			tag )
	{
		rebuild( tag );
	}
	
	public void
	tagRemoved(
		Tag			tag )
	{
		rebuild( tag );
	}
	
	private void 
	delete() 
	{    
	    Utils.disposeComposite( panel );
	    
	    TagManager tm = TagManagerFactory.getTagManager();
	    
	    tm.removeTagManagerListener( this );
	    
	    for ( TagType tt: tm.getTagTypes()){
	    	
	    	tt.removeTagTypeListener( this );
	    }
	    
	    if ( mpg != null ){
	    	
	    	mpg.dispose();
	    }
	}


  
	private void 
	refresh(
		boolean force) 
	{
		mpg.refresh( force );
	}  
  
	public boolean 
	eventOccurred(
		UISWTViewEvent event) 
	{
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = event.getView();
				swtView.setTitle(MessageText.getString(MSGID_PREFIX + ".title.full"));
				break;
	
			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;
	
			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite)event.getData());
				break;
	
			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				Messages.updateLanguageForControl( panel );
				break;
	
			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				break;
	
			case UISWTViewEvent.TYPE_FOCUSGAINED:
				refresh(true);
				break;
	
			case UISWTViewEvent.TYPE_REFRESH:
				refresh(false);
				break;
	
			case StatsView.EVENT_PERIODIC_UPDATE:
				periodicUpdate();
				break;
		}

		return true;
	}
	
	private class
	ValueSourceImpl
		implements ValueSource
	{	
		TagFeatureRateLimit		tag;
		String					name;
		int						index;
		Color[]					colours;
		boolean					is_up;
		
		private boolean			is_hover;
		private boolean			is_invisible;
		
		private
		ValueSourceImpl(
			TagFeatureRateLimit		_tag,
			String					_name,
			int						_index,
			Color[]					_colours,
			boolean					_is_up )
		{
			tag		= _tag;
			name	= _name;
			index	= _index;
			colours	= _colours;
			is_up	= _is_up;
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
			return( false );
		}
		
		private void
		setHover(
			boolean	h )
		{
			is_hover = h;
		}
		
		public int 
		getAlpha() 
		{
			return( 255 );
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
			
			return( style );
		}
		
		public int
		getValue()
		{
			int rate = is_up?tag.getTagCurrentUploadRate():tag.getTagCurrentDownloadRate();
			
			if ( rate < 0 ){
				
				rate = 0;
			}
			
			return( rate );
		}
	}
}
