/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core;

import java.util.*;
import java.io.*;

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
      fin = new FileInputStream(this.getApplicationPath() + filename);
      bin = new BufferedInputStream(fin);
      propertiesMap = BDecoder.decode(bin);
    } catch (FileNotFoundException e) {
      //create the file!
      try {
        //create the file
        new File(this.getApplicationPath() + filename).createNewFile();
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
      fos = new FileOutputStream(this.getApplicationPath() + filename);
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

  public void setParameter(String parameter, boolean value) {
    int intValue = value ? 1 : 0;
    propertiesMap.put(parameter, new Long(intValue));
  }

  private Long getIntParameter(String parameter) {
    try {
      return (Long) propertiesMap.get(parameter);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public int getIntParameter(String parameter, int defaultValue) {
    Long tempValue = this.getIntParameter(parameter);
    return tempValue != null ? tempValue.intValue() : defaultValue;
  }

  private byte[] getByteParameter(String parameter) {
    return (byte[]) propertiesMap.get(parameter);
  }

  public byte[] getByteParameter(String parameter, byte[] defaultValue) {
    byte[] tempValue = this.getByteParameter(parameter);
    return tempValue != null ? tempValue : defaultValue;
  }

  private String getStringParameter(String parameter, byte[] defaultValue) {
    try {
      return new String((byte[])this.getByteParameter(parameter, defaultValue));
    } catch (Exception e) {
      //e.printStackTrace();
      return null;
    }
  }

  public String getStringParameter(String parameter, String defaultValue) {
    String tempValue = this.getStringParameter(parameter, (byte[]) null);
    return tempValue != null ? tempValue : defaultValue;
  }

  public void setParameter(String parameter, int defaultValue) {
    propertiesMap.put(parameter, new Long(defaultValue));
  }

  public void setParameter(String parameter, byte[] defaultValue) {
    propertiesMap.put(parameter, defaultValue);
  }

  public void setParameter(String parameter, String defaultValue) {
    this.setParameter(parameter, defaultValue.getBytes());
  }

  //TODO:: Move this to a FileManager class?
  private String getApplicationPath() {
    return System.getProperty("user.dir") + System.getProperty("file.separator");
  }
}
