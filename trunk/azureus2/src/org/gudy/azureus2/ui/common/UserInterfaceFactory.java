/*
 * UserInterfaceFactory.java
 *
 * Created on 9. Oktober 2003, 00:33
 */

package org.gudy.azureus2.ui.common;

/**
 *
 * @author  Tobias Minich
 */
public class UserInterfaceFactory {
  
  /** Creates a new instance of UserInterfaceFactory */
  public static IUserInterface getUI(String ui) {
    IUserInterface cui = null;
    String uiclass = "org.gudy.azureus2.ui."+ui+".UI";
    try {
      cui = (IUserInterface) Class.forName(uiclass).newInstance();
    } catch (ClassNotFoundException e) {
      throw new Error("Could not find class: "+uiclass);
    } catch (InstantiationException e) {
      throw new Error("Could not instantiate User Interface: "+ uiclass);
    } catch (IllegalAccessException e) {
      throw new Error("Could not access User Interface: "+ uiclass);
    }
    return cui;
  }
      
  
}
