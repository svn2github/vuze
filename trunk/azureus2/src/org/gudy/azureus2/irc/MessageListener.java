/*
 * Created on 6 sept. 2003
 *
 */
package org.gudy.azureus2.irc;

/**
 * @author Olivier
 * 
 */
public interface MessageListener {

  public void messageReceived(String sender,String message);
  public void systemMessage(String message);
  
}
