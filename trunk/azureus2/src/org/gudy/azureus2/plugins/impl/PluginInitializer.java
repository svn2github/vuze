/*
 * File    : PluginInitializer.java
 * Created : 2 nov. 2003 18:59:17
 * By      : Olivier 
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
 
package org.gudy.azureus2.plugins.impl;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.swt.SplashWindow;
import org.gudy.azureus2.plugins.Plugin;

/**
 * @author Olivier
 * 
 */
public class PluginInitializer {

  URLClassLoader classLoader;
  private GlobalManager gm;
  private SplashWindow splash;
  
  public PluginInitializer(GlobalManager gm,SplashWindow splash) {
    this.gm = gm;
    this.splash = splash;
  }
  
  public void initializePlugins() {
    File pluginDirectory = FileUtil.getApplicationFile(System.getProperty("file.separator") + "plugins");
    if(!pluginDirectory.exists()) return;
    if(!pluginDirectory.isDirectory()) return;
    File[] pluginsDirectory = pluginDirectory.listFiles();
    for(int i = 0 ; i < pluginsDirectory.length ; i++) {
      if(splash != null) {        
        splash.setCurrentTask(MessageText.getString("splash.plugin") + pluginsDirectory[i].getName());
      }
      initializePluginFromDir(pluginsDirectory[i]);
      if(splash != null) {
        splash.setPercentDone(50 + (50*(i +1)) / pluginsDirectory.length);        
      }
    }
  }
  
  private void initializePluginFromDir(File directory) {
    classLoader = null;
    if(!directory.exists()) return;
    if(!directory.isDirectory()) return;
    String pluginName = directory.getName();
    File[] pluginContents = directory.listFiles();
    for(int i = 0 ; i < pluginContents.length ; i++) {
      addFileToClassPath(pluginContents[i]);
    }
    Properties props = null;
    try {
      props = new Properties();
      try {
        URL url = classLoader.findResource("plugin.properties");
        props.load(url.openStream());
      } catch (Exception e) {
            System.out.println("Can't read plugin.properties from plug-in " + pluginName + " : file may be missing.");
            return;    
      }

      Class c = classLoader.loadClass((String)props.get("plugin.class"));
      Plugin plugin = (Plugin) c.newInstance();
      MessageText.integratePluginMessages((String)props.get("plugin.langfile"));
      plugin.initialize(new PluginInterfaceImpl(directory.getName(),props,directory.getAbsolutePath()));
    } catch(Exception e) {
      e.printStackTrace();
      System.out.println("Error while loading class :" + ((String)props.get("plugin.class")));      
    }
  }
  
  private void addFileToClassPath(File f) {
    if(!f.exists()) return;
    if(f.isDirectory()) return;
    if(!f.getName().endsWith(".jar")) return;
    try {
      URL[] urls = {f.toURL()};
      if(classLoader == null) {
        classLoader = new URLClassLoader(urls);
      } else {
        classLoader = new URLClassLoader(urls,classLoader);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
