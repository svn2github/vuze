/*
 * Created on 05-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIException;

import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTAWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;

import com.aelitis.azureus.core.AzureusCore;

public class 
UISWTInstanceImpl
	implements UISWTInstance
{
	public 
	UISWTInstanceImpl(
		AzureusCore		core )
	{
		try{
			core.getPluginManager().getDefaultPluginInterface().getUIManager().attachUI( this );
			
		}catch( UIException e ){
			
			Debug.printStackTrace(e);
		}
	}
  
	public Display 
	getDisplay() 
	{
		return SWTThread.getInstance().getDisplay();
	}
  
	public UISWTGraphic 
	createGraphic(
		Image img) 
	{
		return new UISWTGraphicImpl(img);
	}
  

	public void 
	addView(
		final UISWTPluginView view, 
		boolean bAutoOpen )
	{
	  	try{
		    final MainWindow window = MainWindow.getWindow();
		    if(window != null) {
		      window.getMenu().addPluginView(view);
		      if (bAutoOpen) {
	          window.getDisplay().asyncExec(new AERunnable(){
	            public void runSupport() {
	    	        window.openPluginView(view);
	            }
	          });
		      }
		    }
	  	}catch( Throwable e ){
	  		// SWT not available prolly
	  	}
	} 
  
	public void
	addView(
		final UISWTAWTPluginView	view,
		boolean						auto_open )
	{
		addView(
			new UISWTPluginView()
			{
				Composite		composite;
				Component		component;
				
				boolean	first_paint = true;
				
				public String
				getPluginViewName()
				{
					return( view.getPluginViewName());
				}
				
				public String 
				getFullTitle() 
				{
					return( view.getPluginViewName());
				}
				 
				public void 
				initialize(
					Composite _composite )
				{
					first_paint	= true;
					
					composite	= _composite;
					
					Composite frame_composite = new Composite(composite, SWT.EMBEDDED);
	
					GridData data = new GridData(GridData.FILL_BOTH);
					
					frame_composite.setLayoutData(data);
	
					Frame	f = SWT_AWT.new_Frame(frame_composite);
	
					BorderLayout	layout = 
						new BorderLayout()
						{
							public void 
							layoutContainer(Container parent)
							{
								try{
									super.layoutContainer( parent );
								
								}finally{
									if ( first_paint ){
										
										first_paint	= false;
											
										view.open( component );
									}
								}
							}
						};
					
					Panel	pan = new Panel( layout );
	
					f.add( pan );
							
					component	= view.create();
					
					pan.add( component, BorderLayout.CENTER );
				}
				
				public Composite 
				getComposite()
				{
					return( composite );
				}
				
				public void 
				delete() 
				{
					super.delete();
					
					view.delete( component );
				}
			},
			auto_open );
	}
	
	public void
	detach()
	
		throws UIException
	{
		throw( new UIException( "not supported" ));
	}
}
