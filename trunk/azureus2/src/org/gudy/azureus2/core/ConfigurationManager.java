/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.*;

/**
 * A singleton used to store configuration into a bencoded file.
 *
 * @author TdC_VgA
 *
 */
public class ConfigurationManager {
  
  private static ConfigurationManager config;
  
  private Map propertiesMap;
  
  private ConfigurationManager() {
    load();
  }
  
  public synchronized static ConfigurationManager getInstance() {
    if (config == null)
      config = new ConfigurationManager();
    return config;
  }
  
  public void load(String filename) {
    FileInputStream fin = null;
    BufferedInputStream bin = null;
    try {
      //open the file
      fin = new FileInputStream(getApplicationPath() + filename);
      bin = new BufferedInputStream(fin);
      propertiesMap = BDecoder.decode(bin);
      if (propertiesMap == null)
        // Occurs when file is there but zero size (or b0rked?)
        propertiesMap = new HashMap();
    } catch (FileNotFoundException e) {
      //create the file!
      try {
        File newConfigFile = new File(getApplicationPath() + filename);
        if (System.getProperty("os.name").equals("Linux"))
          newConfigFile.getParentFile().mkdir();
        newConfigFile.createNewFile();
        //create an instance of properties map
        propertiesMap = new HashMap();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } finally {
      try {
        if (fin != null)
          fin.close();
      } catch (Exception e) {
      }
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
    //re-encode the data
    byte[] torrentData = BEncoder.encode(propertiesMap);
    //open a file stream
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(getApplicationPath() + filename);
      //write the data out
      fos.write(torrentData);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (fos != null)
          fos.close();
      } catch (Exception e) {
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
      e.printStackTrace();
      result = def.def_boolean;
    }
    return result == 0 ? false : true;
  }
  
  public void setParameter(String parameter, boolean value) {
    int intValue = value ? 1 : 0;
    propertiesMap.put(parameter, new Long(intValue));
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
      e.printStackTrace();
      result = def.def_int;
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
      //e.printStackTrace();
      return null;
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
      e.printStackTrace();
      result = def.def_String;
    }
    return result;
  }
  
  public String getDirectoryParameter(String parameter) throws IOException {
    ConfigurationDefaults def = ConfigurationDefaults.getInstance();
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
    propertiesMap.put(parameter, new Long(defaultValue));
  }
  
  public void setParameter(String parameter, byte[] defaultValue) {
    propertiesMap.put(parameter, defaultValue);
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
  
  //TODO:: Move this to a FileManager class?
  public static String getApplicationPath() {
    if (System.getProperty("os.name").equals("Linux")) {
      return System.getProperty("user.home") + System.getProperty("file.separator") + ".azureus" + System.getProperty("file.separator");
    } else {
      return System.getProperty("user.dir") + System.getProperty("file.separator");
    }
  }
}
