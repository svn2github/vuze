/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core3.config.impl;

import java.io.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

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
  
  private Map propertiesMap;
  
  private Vector	listeners = new Vector();
  private Hashtable parameterListeners = new Hashtable();
  
  private ConfigurationManager() {
  	load();
  }
  
  private ConfigurationManager(Map data) {
  	propertiesMap	= data;
  }
  
  public synchronized static ConfigurationManager getInstance() {
  	if (config == null)
  		config = new ConfigurationManager();
  	return config;
  }
  
  public synchronized static ConfigurationManager getInstance(Map data) {
  	if (config == null)
  		config = new ConfigurationManager(data);
  	return config;
  }
  
  public void load(String filename) {
    BufferedInputStream bin = null;
    
    try {
      //open the file
      File file = new File( SystemProperties.getUserPath() + filename );
      
      //make sure the file exists and isn't zero-length
      if ( file.length() <= 1L ) {
        //if so, try using the backup file
        file = new File( SystemProperties.getUserPath() + filename + ".bak" );
        if ( file.length() <= 1L ) {
          throw new FileNotFoundException();
        }
      }
      
      bin = new BufferedInputStream( new FileInputStream(file), 8192 );
        
      try{
      	propertiesMap = BDecoder.decode(bin);
      }
      catch( IOException e ){
      	// Occurs when file is there but b0rked
      	propertiesMap = new HashMap();
      }

    }
    catch (FileNotFoundException e) {
    	propertiesMap = new HashMap();
    }
    finally {
    	try {
    		if (bin != null)
    			bin.close();
    	} catch (Exception e) {
    	}
    }
  }
  
  public void load() {
    load("azureus.config");
  }
  
  public void save(String filename) {
    //open a file stream
    BufferedOutputStream bos = null;
    try {
    	//re-encode the data
    	
    	byte[] torrentData = BEncoder.encode(propertiesMap);
      
      File file = new File( SystemProperties.getUserPath() + filename );
      
    	//backup
      if ( file.length() > 1L ) {
        File bakfile = new File( file + ".bak" );
        if ( bakfile.exists() ) bakfile.delete();
        file.renameTo( bakfile );
      }
    	
      bos = new BufferedOutputStream( new FileOutputStream( file, false ), 8192 );
    	
    	//write the data out
    	bos.write(torrentData);
      bos.flush();
      
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (bos != null)
          bos.close();
      } catch (Exception e) {
      }
    }
    
    synchronized( this  ){
    	
    	for (int i=0;i<listeners.size();i++){
    		COConfigurationListener l = (COConfigurationListener)listeners.elementAt(i);
    		
    		if (l != null) {
    			l.configurationSaved();
    		}
    		else Debug.out("COConfigurationListener is null");
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
  
  public void setParameter(String parameter, boolean value) {
    setParameter(parameter, value ? 1 : 0);
  }
  
  private Long getIntParameterRaw(String parameter) {
    try {
      return (Long) propertiesMap.get(parameter);
    } catch (Exception e) {
      e.printStackTrace();
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
  
  public void setParameter(String parameter, int defaultValue) {
    Long newValue = new Long(defaultValue);
    Long oldValue = (Long) propertiesMap.put(parameter, newValue);
    notifyParameterListenersIfChanged(parameter, newValue, oldValue);
  }
  
  public void setParameter(String parameter, byte[] defaultValue) {
    byte[] oldValue = (byte[]) propertiesMap.put(parameter, defaultValue);
    notifyParameterListenersIfChanged(parameter, defaultValue, oldValue);
   }
  
  public void setParameter(String parameter, String defaultValue) {
    setParameter(parameter, defaultValue.getBytes());
  }
  
  // Sets a parameter back to its default
  public void setParameter(String parameter) throws ConfigurationParameterNotFoundException {
    ConfigurationDefaults def = ConfigurationDefaults.getInstance();
    try {
      setParameter(parameter, def.getIntParameter(parameter));
    } catch (Exception e) {
      setParameter(parameter, def.getStringParameter(parameter));
    }
  }
  
  private void notifyParameterListenersIfChanged(String parameter, Long newValue, Long oldValue) {
    if(oldValue == null || 0 != newValue.compareTo(oldValue))
      notifyParameterListeners(parameter);
  }

  private void notifyParameterListenersIfChanged(String parameter, byte[] newValue, byte[] oldValue) {
    if(oldValue == null || Arrays.equals(newValue, oldValue))
      notifyParameterListeners(parameter);
  }
    
  private void notifyParameterListeners(String parameter) {
    Vector parameterListener = (Vector) parameterListeners.get(parameter);
    if(parameterListener != null) {
      for (Iterator iter = parameterListener.iterator(); iter.hasNext();) {
        ParameterListener listener = (ParameterListener) iter.next();
        
        if(listener != null) {
          listener.parameterChanged(parameter);
        }
        else Debug.out("ParameterListener is null");
      }
    }
    //else Debug.out("parameterListener Vector is null for: " + parameter);
  }

  public synchronized void addParameterListener(String parameter, ParameterListener listener){
    if(parameter == null || listener == null)
      return;
    Vector parameterListener = (Vector) parameterListeners.get(parameter);
    if(parameterListener == null) {
      parameterListeners.put(parameter, parameterListener = new Vector());
    }
    if(!parameterListener.contains(listener))
      parameterListener.add(listener); 
  }

  public synchronized void removeParameterListener(String parameter, ParameterListener listener){
    if(parameter == null || listener == null)
      return;
    Vector parameterListener = (Vector) parameterListeners.get(parameter);
    if(parameterListener != null) {
    	parameterListener.remove(listener);
    }
  }

  public synchronized void addListener(COConfigurationListener listener) {
    listeners.addElement(listener);
  }

  public synchronized void removeListener(COConfigurationListener listener) {
    listeners.removeElement(listener);
  }
}
