/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core3.config.impl;

import java.io.*;
import java.io.IOException;
import java.util.*;


import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;

/**
 * A singleton used to store configuration into a bencoded file.
 *
 * @author TdC_VgA
 *
 */
public class ConfigurationManager {
  
  private static ConfigurationManager config;
  private static AEMonitor				class_mon	= new AEMonitor( "ConfigMan:class" );
  
  private Map propertiesMap;
  
  private List		listeners 			= new ArrayList();
  private Hashtable parameterListeners 	= new Hashtable();
  
  private AEMonitor	this_mon	= new AEMonitor( "ConfigMan");
  
  private ConfigurationManager() {
  	load();
  }
  
  private ConfigurationManager(Map data) {
  	propertiesMap	= data;
  }
  
  public static ConfigurationManager getInstance() {
  	try{
  		class_mon.enter();
  	
	  	if (config == null)
	  		config = new ConfigurationManager();
	  	return config;
  	}finally{
  		class_mon.exit();
  	}
  }
  
  public static ConfigurationManager getInstance(Map data) {
  	try{
  		class_mon.enter();

	  	if (config == null)
	  		config = new ConfigurationManager(data);
	  	return config;
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public void load(String filename) 
  {
  	propertiesMap = FileUtil.readResilientConfigFile( filename );

  }
  
  public void load() {
    load("azureus.config");
  }
  
  public void save(String filename) 
  {
  	FileUtil.writeResilientConfigFile( filename, propertiesMap );
    
  	List	listeners_copy;
  	
    try{
    	this_mon.enter();
    
    	listeners_copy = new ArrayList( listeners );
    	
    }finally{
    	
    	this_mon.exit();
    }
    
	for (int i=0;i<listeners_copy.size();i++){
		
		COConfigurationListener l = (COConfigurationListener)listeners_copy.get(i);
		
		if (l != null){
			
			try{
				l.configurationSaved();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}else{
			
			Debug.out("COConfigurationListener is null");
		}
	}
  }
  
  public void save() {
    save("azureus.config");
  }
  
  public boolean getBooleanParameter(String parameter, boolean defaultValue) {
    int defaultInt = defaultValue ? 1 : 0;
    int result = getIntParameter(parameter, defaultInt);
    return result == 0 ? false : true;
  }
  
  public boolean getBooleanParameter(String parameter) {
    ConfigurationDefaults def = ConfigurationDefaults.getInstance();
    int result;
    try {
      result = getIntParameter(parameter, def.getIntParameter(parameter));
    } catch (ConfigurationParameterNotFoundException e) {
      result = getIntParameter(parameter, def.def_boolean);
    }
    return result == 0 ? false : true;
  }
  
  public boolean setParameter(String parameter, boolean value) {
    return setParameter(parameter, value ? 1 : 0);
  }
  
  private Long getIntParameterRaw(String parameter) {
    try {
      return (Long) propertiesMap.get(parameter);
    } catch (Exception e) {
    	Debug.printStackTrace( e );
      return null;
    }
  }
  
  public int getIntParameter(String parameter, int defaultValue) {
    Long tempValue = getIntParameterRaw(parameter);
    return tempValue != null ? tempValue.intValue() : defaultValue;
  }
  
  public int getIntParameter(String parameter) {
  	ConfigurationDefaults def = ConfigurationDefaults.getInstance();
  	int result;
    try {
      result = getIntParameter(parameter, def.getIntParameter(parameter));
    } catch (ConfigurationParameterNotFoundException e) {
      result = getIntParameter(parameter, def.def_int);
    }
    return result;
  }
  
  private byte[] getByteParameterRaw(String parameter) {
    return (byte[]) propertiesMap.get(parameter);
  }
  
  public byte[] getByteParameter(String parameter, byte[] defaultValue) {
    byte[] tempValue = getByteParameterRaw(parameter);
    return tempValue != null ? tempValue : defaultValue;
  }
  
  private String getStringParameter(String parameter, byte[] defaultValue) {
    try {
      return new String(getByteParameter(parameter, defaultValue));
    } catch (Exception e) {
      byte[] bytesReturn = getByteParameter(parameter, null);
      if (bytesReturn == null)
        return null;
      return new String(bytesReturn);
    }
  }
  
  public String getStringParameter(String parameter, String defaultValue) {
    String tempValue = getStringParameter(parameter, (byte[]) null);
    return tempValue != null ? tempValue : defaultValue;
  }
  
  public String getStringParameter(String parameter) {
    ConfigurationDefaults def = ConfigurationDefaults.getInstance();
    String result;
    try {
      result = getStringParameter(parameter, def.getStringParameter(parameter));
    } catch (ConfigurationParameterNotFoundException e) {
      result = getStringParameter(parameter, def.def_String);
    }
    return result;
  }
  
  public StringList getStringListParameter(String parameter) {
  	try {  		
  		List rawList = (List) propertiesMap.get(parameter);
  		if(rawList == null)
  			return new StringListImpl();  		
  		return new StringListImpl(rawList);  	
  	} catch(Exception e) {
  		Debug.printStackTrace(e);
  		return new StringListImpl();
  	}
  }
  
  public boolean setParameter(String parameter,StringList value) {
  	try {
  		propertiesMap.put(parameter,new ArrayList(((StringListImpl)value).getList()));
  	} catch(Exception e) {
  		Debug.printStackTrace(e);
  		return false;
  	}
  	return true;
  }
   
  public String getDirectoryParameter(String parameter) throws IOException {
    String dir = getStringParameter(parameter);
    File temp = new File(dir);
    if (!temp.exists())
      temp.mkdirs();
    else if (!temp.isDirectory()) {
      throw new IOException(
      "Configuration error. This is not a directory: " + dir);
    }
    return dir;
  }
  
  public float getFloatParameter(String parameter) {
    ConfigurationDefaults def = ConfigurationDefaults.getInstance();
    try {
      Object o = propertiesMap.get(parameter);
      if (o instanceof Number) {
        return ((Number)o).floatValue();
      }
      
      String s = getStringParameter(parameter);
      
      if (!s.equals(def.def_String))
        return Float.parseFloat(s);
    } catch (Exception e) {
    	Debug.printStackTrace( e );
    }
    
    try {
      return def.getFloatParameter(parameter);
    } catch (Exception e2) {
      return def.def_float;
    }
  }

  public boolean setParameter(String parameter, float defaultValue) {
    String newValue = String.valueOf(defaultValue);
    return setParameter(parameter, newValue.getBytes());
  }

  public boolean setParameter(String parameter, int defaultValue) {
    Long newValue = new Long(defaultValue);
    Long oldValue = (Long) propertiesMap.put(parameter, newValue);
    return notifyParameterListenersIfChanged(parameter, newValue, oldValue);
  }
  
  public boolean setParameter(String parameter, byte[] defaultValue) {
    byte[] oldValue = (byte[]) propertiesMap.put(parameter, defaultValue);
    return notifyParameterListenersIfChanged(parameter, defaultValue, oldValue);
   }
  
  public boolean setParameter(String parameter, String defaultValue) {
    return setParameter(parameter, defaultValue.getBytes());
  }

	public boolean setRGBParameter(String parameter, int red, int green, int blue) {
    boolean bAnyChanged = false;
    bAnyChanged |= setParameter(parameter + ".red", red);
    bAnyChanged |= setParameter(parameter + ".green", green);
    bAnyChanged |= setParameter(parameter + ".blue", blue);
    if (bAnyChanged)
      notifyParameterListeners(parameter);

    return bAnyChanged;
	}
  
  // Sets a parameter back to its default
  public boolean setParameter(String parameter) throws ConfigurationParameterNotFoundException {
    ConfigurationDefaults def = ConfigurationDefaults.getInstance();
    try {
      return setParameter(parameter, def.getIntParameter(parameter));
    } catch (Exception e) {
      return setParameter(parameter, def.getStringParameter(parameter));
    }
  }
  
  private boolean  notifyParameterListenersIfChanged(String parameter, Long newValue, Long oldValue) {
    if(oldValue == null || 0 != newValue.compareTo(oldValue)) {
      notifyParameterListeners(parameter);
      return true;
    }
    return false;
  }

  private boolean notifyParameterListenersIfChanged(String parameter, byte[] newValue, byte[] oldValue) {
    if(oldValue == null || !Arrays.equals(newValue, oldValue)) {
      notifyParameterListeners(parameter);
      return true;
    }
    return false;
  }
    
  private void notifyParameterListeners(String parameter) {
    Vector parameterListener = (Vector) parameterListeners.get(parameter);
    if(parameterListener != null) {
    	try{
    		for (int i=0;i<parameterListener.size();i++){
        
    			ParameterListener	listener = (ParameterListener)parameterListener.get(i);
    			
    			if(listener != null) {
    				listener.parameterChanged(parameter);
    			}
    		}
    	}catch( Throwable e ){
    		
    			// we're not synchronized so possible but unlikely error here
    		
    		Debug.printStackTrace( e );
      }
    }
   }

  public void addParameterListener(String parameter, ParameterListener listener){
  	try{
  		this_mon.enter();
  	
	    if(parameter == null || listener == null)
	      return;
	    Vector parameterListener = (Vector) parameterListeners.get(parameter);
	    if(parameterListener == null) {
	      parameterListeners.put(parameter, parameterListener = new Vector());
	    }
	    if(!parameterListener.contains(listener))
	      parameterListener.add(listener); 
  	}finally{
  		this_mon.exit();
  	}
  }

  public void removeParameterListener(String parameter, ParameterListener listener){
  	try{
  		this_mon.enter();
 
	    if(parameter == null || listener == null)
	      return;
	    Vector parameterListener = (Vector) parameterListeners.get(parameter);
	    if(parameterListener != null) {
	    	parameterListener.remove(listener);
	    }
  	}finally{
  		this_mon.exit();
  	}
  }

  public void addListener(COConfigurationListener listener) {
  	try{
  		this_mon.enter();

  		listeners.add(listener);
  		
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void removeListener(COConfigurationListener listener) {
  	try{
  		this_mon.enter();
  	
  		listeners.remove(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }
}
