/*
 * File    : RPPluginConfig.java
 * Created : 17-Feb-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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

package org.gudy.azureus2.pluginsremote;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.*;

public class 
RPPluginConfig
	extends		RPObject
	implements 	PluginConfig
{
	protected transient PluginConfig		delegate;

	public static PluginConfig
	create(
		PluginConfig		_delegate )
	{
		RPPluginConfig	res =(RPPluginConfig)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPPluginConfig( _delegate );
		}
			
		return( res );
	}
	
	protected
	RPPluginConfig(
		PluginConfig		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (PluginConfig)_delegate;
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	public void
	_setRemote(
		RPRequestDispatcher		_dispatcher )
	{
		super._setRemote( _dispatcher );
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		Object[] params = (Object[])request.getParams();
		
		if ( method.equals( "getPluginIntParameter")){
			
			return( new RPReply( new Integer( delegate.getPluginIntParameter((String)params[0],((Integer)params[1]).intValue()))));
			
		}else if ( method.equals( "setPluginParameter[int]")){
				
			delegate.setPluginParameter((String)params[0],((Integer)params[1]).intValue());
				
		}else if ( method.equals( "save")){
			
			try{ 
				delegate.save();
				
				return( null );
				
			}catch( PluginException e ){
				
				return( new RPReply( e ));
			}
		}			
	
			
		throw( new RPException( "Unknown method: " + method ));
	}

	// ***************************************************

	  public int getIntParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(0);
	  }

	  public int getIntParameter(String key, int default_value)
	  {
	  	notSupported();
	  	
	  	return(0);
	  }
		
	  public void
	  setIntParameter(
		  	String	key, 
			int		value )
	  {
	  	notSupported();
	  }
	  
	  public String getStringParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(null);
	  }
	  
	  public boolean getBooleanParameter(String key)
	  {	
	  	notSupported();
	  	
	  	return(false);
	  }
	  
	  public boolean getBooleanParameter(String key, boolean _default )
	  {
	  	notSupported();
	  	
	  	return( false );
	  }
	  
	  public int getPluginIntParameter(String key)
	  {	
	  	notSupported();
	  	
	  	return(0);
	  }
	  
	  public int getPluginIntParameter(String key,int defaultValue)
	  {
		Integer	res = (Integer)_dispatcher.dispatch( new RPRequest( this, "getPluginIntParameter", new Object[]{key,new Integer(defaultValue)} )).getResponse();
		
		return( res.intValue());
	  }
	  
	  public String getPluginStringParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(null);
	  }
	  
	  public String getPluginStringParameter(String key,String defaultValue)
	  {
	  	notSupported();
	  	
	  	return(null);
	  }
	  
	  public boolean getPluginBooleanParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(false);
	  }
	  
	  public boolean getPluginBooleanParameter(String key,boolean defaultValue)
	  {
	  	notSupported();
	  	
	  	return(false);
	  }
	    
	  public void setPluginParameter(String key,int value)
	  {
		_dispatcher.dispatch( new RPRequest( this, "setPluginParameter[int]", new Object[]{key,new Integer(value)} ));
	  }
	  
	  public void setPluginParameter(String key,String value)
	  {
	  	
	  	notSupported();
	  }
	  
	  public void setPluginParameter(String key,boolean value)
	  {  	
	  	notSupported();
	  }
	  
	  public void
	  save()
	  	throws PluginException
	  {
	  	try{
	  		_dispatcher.dispatch( new RPRequest( this, "save", null)).getResponse();
	  		
		}catch( RPException e ){
			
			Throwable cause = e.getCause();
			
			if ( cause instanceof PluginException ){
				
				throw((PluginException)cause);
			}
			
			throw( e );
		}
	  }
}
