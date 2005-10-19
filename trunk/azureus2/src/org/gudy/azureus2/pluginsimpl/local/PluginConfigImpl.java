/*
 * File    : PluginConfigImpl.java
 * Created : 10 nov. 2003
 * By      : epall
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

package org.gudy.azureus2.pluginsimpl.local;

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginConfigListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.pluginsimpl.local.config.*;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class 
PluginConfigImpl
	implements PluginConfig 
{

	protected static Map	external_to_internal_key_map = new HashMap();
	
	static{
		
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, 		CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC, 		"Max Upload Speed Seeding KBs" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, 	CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL, 				"Max.Peer.Connections.Total" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT, 			"Max.Peer.Connections.Per.Torrent" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_DOWNLOADS, 						"max downloads" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_ACTIVE, 							"max active torrents" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_ACTIVE_SEEDING, 							"StartStopManager_iMaxActiveTorrentsWhenSeeding" );
		external_to_internal_key_map.put(CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING, "enable.seedingonly.upload.rate");
		external_to_internal_key_map.put(CORE_PARAM_BOOLEAN_MAX_ACTIVE_SEEDING, "StartStopManager_bMaxActiveTorrentsWhenSeedingEnabled");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_SOCKS_PROXY_NO_INWARD_CONNECTION, 	"Proxy.Data.SOCKS.inform" );
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_NEW_SEEDS_START_AT_TOP, 			CORE_PARAM_BOOLEAN_NEW_SEEDS_START_AT_TOP );
		external_to_internal_key_map.put( CORE_PARAM_STRING_LOCAL_BIND_IP, 						"Bind IP" );
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_FRIENDLY_HASH_CHECKING, 			"diskmanager.friendly.hashchecking" );
	}

	private PluginInterface	plugin_interface;
	private String 			key;
  
	public 
	PluginConfigImpl(
		PluginInterface		_plugin_interface,
		String			 	_key ) 
	{
		plugin_interface	= _plugin_interface;
		
		key = _key + ".";
	}

	public String
	getPluginConfigKeyPrefix()
	{
		return( key );
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getStringParameter(java.lang.String)
	 */
	public String getStringParameter(String name) {
		return COConfigurationManager.getStringParameter(mapKeyName(name));
	}

    public String getStringParameter(String name, String _default )
	{
		return COConfigurationManager.getStringParameter(mapKeyName(name), _default);
    }

	public float getFloatParameter(String name) {
		return COConfigurationManager.getFloatParameter(mapKeyName(name));
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getIntParameter(java.lang.String)
	 */
	public int getIntParameter(String name) {
		return COConfigurationManager.getIntParameter(mapKeyName(name));
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getIntParameter(java.lang.String)
	 */
	public int getIntParameter(String name, int default_value) {
		return COConfigurationManager.getIntParameter(mapKeyName(name), default_value);
	}

	public void
	setIntParameter(
	  	String	key, 
		int		value )
	{
		String	target_key = (String)external_to_internal_key_map.get( key );
		
		if ( target_key == null ){
			
			throw( new RuntimeException("Invalid code int parameter (" + key + ")"));
		}
		
		COConfigurationManager.setParameter( target_key, value );
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getBooleanParameter(java.lang.String)
	 */
	public boolean getBooleanParameter(String name) {
		return COConfigurationManager.getBooleanParameter(mapKeyName(name));
	}
	
	public boolean getBooleanParameter(String name, boolean _default) {
		return COConfigurationManager.getBooleanParameter(mapKeyName(name), _default);
	}
	
	public void
	setBooleanParameter(
	  	String		key, 
		boolean		value )
	{
		String	target_key = (String)external_to_internal_key_map.get( key );
		
		if ( target_key == null ){
			
			throw( new RuntimeException("Invalid code int parameter (" + key + ")"));
		}
		
		COConfigurationManager.setParameter( target_key, value );
	}
	
    public byte[] getByteParameter(String name, byte[] _default )
    {
		return COConfigurationManager.getByteParameter(mapKeyName(name), _default);
    }

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginIntParameter(java.lang.String)
	 */
	public int getPluginIntParameter(String key)
	{
		return getIntParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginIntParameter(java.lang.String, int)
	 */
	public int getPluginIntParameter(String key, int defaultValue)
	{
	   	COConfigurationManager.setIntDefault( this.key+key, defaultValue );

		return COConfigurationManager.getIntParameter(this.key+key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginStringParameter(java.lang.String)
	 */
	public String getPluginStringParameter(String key)
	{
		return getStringParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginStringParameter(java.lang.String, int)
	 */
	public String getPluginStringParameter(String key, String defaultValue)
	{
    	COConfigurationManager.setStringDefault( this.key+key, defaultValue );

		return COConfigurationManager.getStringParameter(this.key+key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginBooleanParameter(java.lang.String)
	 */
	public boolean getPluginBooleanParameter(String key)
	{
		return getBooleanParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginBooleanParameter(java.lang.String, int)
	 */
	public boolean getPluginBooleanParameter(String key, boolean defaultValue)
	{
	   	COConfigurationManager.setBooleanDefault( this.key+key, defaultValue );

		return COConfigurationManager.getBooleanParameter(this.key+key, defaultValue);
	}

	public byte[] getPluginByteParameter(String key, byte[] defaultValue )
	{
	   	COConfigurationManager.setByteDefault( this.key+key, defaultValue );

		return COConfigurationManager.getByteParameter(this.key+key, defaultValue);
	}

	 public List
	 getPluginListParameter( String key, List	default_value )
	 {
		return COConfigurationManager.getListParameter(this.key+key, default_value); 
	 }
	 
	 public void
	 setPluginListParameter( String key, List	value )
	 {
		 COConfigurationManager.setParameter(this.key+key, value);
	 }

	 public Map
	 getPluginMapParameter( String key, Map	default_value )
	 {
		return COConfigurationManager.getMapParameter(this.key+key, default_value); 
	 }
	 
	 public void
	 setPluginMapParameter( String key, Map	value )
	 {
		 COConfigurationManager.setParameter(this.key+key, value);
	 }
	 
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, int)
	 */
	public void setPluginParameter(String key, int value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, java.lang.String)
	 */
	public void setPluginParameter(String key, String value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, boolean)
	 */
	public void setPluginParameter(String key, boolean value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}
	
	public void setPluginParameter(String key,byte[] value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}

	public ConfigParameter
	getParameter(
		String		key )
	{
		return( new ConfigParameterImpl( mapKeyName(key)));
	}
	
	public ConfigParameter
	getPluginParameter(
	  	String		key )
	{
		return( new ConfigParameterImpl( this.key+key ));
	}
	
	public void
	save()
	{
		COConfigurationManager.save();
	}
	
	protected String
	mapKeyName(
		String	key )
	{
		String	k = (String)external_to_internal_key_map.get(key);
		
		if ( k != null ){
			
			return( k );
		}
		
		return( key );
	}
	
	public File
	getPluginUserFile(
		String	name )
	{
		
		String	dir = plugin_interface.getUtilities().getAzureusUserDir();
		
		File	file = new File( dir, "plugins" );

		String	p_dir = plugin_interface.getPluginDirectoryName();
		
		if ( p_dir.length() != 0 ){
			
			int	lp = p_dir.lastIndexOf(File.separatorChar);
			
			if ( lp != -1 ){
				
				p_dir = p_dir.substring(lp+1);
			}
			
			file = new File( file, p_dir );
			
		}else{
			
			String	id = plugin_interface.getPluginID();
			
			if ( id.length() > 0 && !id.equals( PluginInitializer.INTERNAL_PLUGIN_ID )){
			
				file = new File( file, id );
				
			}else{
				
				throw( new RuntimeException( "Plugin was not loaded from a directory" ));
			}
		}
	
		
		file.mkdirs();
		
		return( new File( file, name ));
	}
	
	public void
	addListener(
		final PluginConfigListener	l )
	{
		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					l.configSaved();
				}
			});
	}
}
