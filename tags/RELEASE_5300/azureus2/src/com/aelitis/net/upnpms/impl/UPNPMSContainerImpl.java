/*
 * Created on Dec 20, 2012
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


package com.aelitis.net.upnpms.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

import com.aelitis.net.upnpms.UPNPMSContainer;
import com.aelitis.net.upnpms.UPNPMSItem;
import com.aelitis.net.upnpms.UPNPMSNode;
import com.aelitis.net.upnpms.UPnPMSException;

public class 
UPNPMSContainerImpl 
	implements UPNPMSContainer
{
	private UPNPMSBrowserImpl		browser;
	private String					id;
	private String					title;
	
	private List<UPNPMSNode>		children;
	
	protected
	UPNPMSContainerImpl(
		UPNPMSBrowserImpl	_browser,
		String				_id,
		String				_title )
	{
		browser	= _browser;
		id 		= _id;
		title	= _title;
	}
	
	public String
	getID()
	{
		return( id );
	}
	
	public String
	getTitle()
	{
		return( title );
	}
	
	private void
	populate()
	
		throws  UPnPMSException
	{
		synchronized( this ){
			
			if ( children == null ){
				
				children 	= new ArrayList<UPNPMSNode>();
				
				List<SimpleXMLParserDocumentNode> results = browser.getContainerContents( id );
				
				for ( SimpleXMLParserDocumentNode result: results ){
					
					// result.print();
					
					SimpleXMLParserDocumentNode[] kids = result.getChildren();
					
					for ( SimpleXMLParserDocumentNode kid: kids ){
						
						String name = kid.getName();
						
						if ( name.equalsIgnoreCase( "container" )){
							
							String	id 		= kid.getAttribute( "id" ).getValue();
							String	title	= kid.getChild( "title" ).getValue();
							
							children.add( new UPNPMSContainerImpl( browser, id, title ));
							
						}else if ( name.equalsIgnoreCase( "item" )){
							
							String	id 		= kid.getAttribute( "id" ).getValue();
							String	title	= kid.getChild( "title" ).getValue();
							String	cla		= kid.getChild( "class" ).getValue();
							
							String	item_class;
							
							if ( cla.contains( ".imageItem" )){
								
								item_class = UPNPMSItem.IC_IMAGE;
								
							}else if ( cla.contains( ".audioItem" )){
								
								item_class = UPNPMSItem.IC_AUDIO;
								
							}else if ( cla.contains( ".videoItem" )){
								
								item_class = UPNPMSItem.IC_VIDEO;
								
							}else{
								
								item_class = UPNPMSItem.IC_OTHER;
							}
							
							URL		url 	= null;
							long	size 	= 0;
							
							SimpleXMLParserDocumentNode[] sub = kid.getChildren();
							
							for ( SimpleXMLParserDocumentNode x: sub ){
								
								if ( x.getName().equalsIgnoreCase( "res" )){
									
									SimpleXMLParserDocumentAttribute a_size = x.getAttribute( "size" );

									long	this_size = 0;
									
									if ( a_size != null ){
										
										try{
											this_size = Long.parseLong( a_size.getValue().trim());
											
										}catch( Throwable e ){
										}
									}
									
									SimpleXMLParserDocumentAttribute pi = x.getAttribute( "protocolInfo" );
									
									if ( pi != null ){
										
										String pi_str = pi.getValue().trim();
										
										if ( pi_str.toLowerCase().startsWith( "http-get" )){
											
											try{
												if ( size == 0 || this_size > size ){
												
													url = new URL( x.getValue().trim());
													
													size = this_size;
												}
											}catch( Throwable e ){
											}
										}
									}
									

								}
							}
							if ( url != null ){
							
								children.add( new UPNPMSItemImpl( id, title, item_class, size, url ));
							}
						}
					}
				}
			}
		}
	}
	
	public List<UPNPMSNode>
	getChildren()
	
		throws  UPnPMSException
	{
		populate();
		
		return( children );
	}
}
