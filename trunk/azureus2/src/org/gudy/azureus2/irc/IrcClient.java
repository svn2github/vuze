/*
 * Created on 6 sept. 2003
 *
 */
package org.gudy.azureus2.irc;

import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.MessageText;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;

/**
 * @author Olivier
 * 
 */
public class IrcClient extends PircBot {

  private String srvName;
  private String channel;

  private MessageListener listener;
  private String userName;

  public IrcClient(MessageListener _listener){    
    this.srvName = ConfigurationManager.getInstance().getStringParameter("Irc Server", "irc.freenode.net");
    this.channel = ConfigurationManager.getInstance().getStringParameter("Irc Channel", "#azureus");
    this.userName = ConfigurationManager.getInstance().getStringParameter("Irc Login", "user" + (int) (Math.random() * 100000));
    this.setName(userName);
    this.listener = _listener;
    Thread t = new Thread() {
      /* (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      public void run() {
        try {
          listener.systemMessage(MessageText.getString("IrcClient.connecting") + " " + srvName);
          connect(srvName);
          listener.systemMessage(MessageText.getString("IrcClient.connected") + " " + srvName);
          listener.systemMessage(MessageText.getString("IrcClient.joining") + " " + channel);
          joinChannel(channel);
          listener.systemMessage(MessageText.getString("IrcClient.channel") + " " + channel + " " +  MessageText.getString("IrcClient.joined"));
        }
        catch (Exception e) {
          //e.printStackTrace();
          listener.systemMessage(MessageText.getString("IrcClient.error") + " : " + e.getMessage());
        }
      }
    };
    t.start();
  }
  
  protected void onMessage(String channel, String sender, String login, String hostname, String message) {
    if(listener != null) {
      listener.messageReceived(sender,Colors.removeFormattingAndColors(message));
    }
  }
  
  public void close() {
    //TODO : implement closing ;)
    super.quitServer();
    super.dispose();
  }
  
  public void sendMessage(String message) {
    super.sendMessage(channel,message);
    listener.messageReceived(userName,message);
  }
}
