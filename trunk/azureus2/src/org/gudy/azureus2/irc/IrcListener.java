/*
 * Created on 6 sept. 2003
 *
 */
package org.gudy.azureus2.irc;

/**
 * @author Olivier
 * 
 */
public interface IrcListener {

  public void messageReceived(String sender,String message);
  public void systemMessage(String message);
  public void clientEntered(String client);
  public void clientExited(String client);
  public void action(String sender,String action);
}
