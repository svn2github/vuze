/*
 * Created on 6 sept. 2003
 *
 */
package org.gudy.azureus2.irc;

import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

/**
 * @author Olivier
 * 
 */
public class IrcClient extends PircBot {

  private String srvName;
  private String channel;

  private IrcListener listener;
  private String userName;

  public IrcClient(IrcListener _listener){    
    this.srvName = ConfigurationManager.getInstance().getStringParameter("Irc Server", "irc.freenode.net");
    this.channel = ConfigurationManager.getInstance().getStringParameter("Irc Channel", "#azureus-users");
    this.userName = ConfigurationManager.getInstance().getStringParameter("Irc Login", "");    
    this.setName(userName);
    this.listener = _listener;
    if(userName.equals("")) {
        listener.systemMessage(MessageText.getString("IrcClient.noNick"));
        return;
    }
    setLogin("Azureus");
    Thread t = new Thread() {
      /* (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      public void run() {
        try {                  
          listener.systemMessage("");  
          listener.systemMessage(MessageText.getString("IrcClient.copyright"));
          listener.systemMessage("");
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
    super.quitServer("Azureus " + MainWindow.VERSION);
    try {
      super.dispose();
    } catch(Exception e) {
      
    }
  }
  
  public void sendMessage(String message) {
    super.sendMessage(channel,message);
    listener.messageReceived(userName,message);
  }
  
  
  protected void onJoin(String channel, String sender, String login, String hostname) {
    listener.systemMessage(sender + " " + MessageText.getString("IrcClient.hasjoined"));
    listener.clientEntered(sender);
  }
  
  protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
    listener.systemMessage(kickerNick + " " + MessageText.getString("IrcClient.haskicked") + " " + recipientNick + " (" + reason + ").");
    listener.clientExited(recipientNick);
  }
  
  protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
    listener.systemMessage(sourceNick + " " + MessageText.getString("IrcClient.hasleft") + " (" + reason + ").");
    listener.clientExited(sourceNick);
  }
  
  protected void onPart(String channel, String sender, String login, String hostname)  {
    listener.systemMessage(sender + " " + MessageText.getString("IrcClient.hasleft"));
    listener.clientExited(sender);
  }
  
  
  protected void onNickChange(String oldNick, String login, String hostname, String newNick) 
  {
    listener.systemMessage(oldNick + " " + MessageText.getString("IrcClient.nowknown") + " " + newNick);
    listener.clientExited(oldNick);
    listener.clientEntered(newNick);
  }
  
  protected void onAction(String sender, String login, String hostname, String target, String action) 
  {
    listener.action(sender,action);
  }
  
  /**
   * @return
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @param userName
   */
  public void setUserName(String userName) {
    this.userName = userName;
    super.changeNick(userName);
  }
  
  public void sendAction(String action) {
    super.sendAction(channel,action);
  }
  
  protected void onUserList(String channel, User[] users) {
    if(! this.channel.equals(channel))
       return;
    for(int i = 0 ; i < users.length ;i++) {
      listener.clientEntered(users[i].getNick());
    }    
  }
  
  protected void onPrivateMessage(String sender, String login, String hostname, String message)
  {
    listener.privateMessage(sender,message);
  }
  
  protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice)
  {
    listener.notice(sourceNick,notice);
  } 
  
  protected void onServerResponse(int code, String response)  {
    //No such nick name:
    if(code == 401)
      listener.systemMessage(response);
  }
  
  protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
    listener.systemMessage(MessageText.getString("IrcClient.topicforchannel") + " " + channel + " : " + topic);
  }
  
  protected void onDisconnect() {
    listener.systemMessage(MessageText.getString("IrcClient.disconnected") + " " + srvName);
  }

  
  
  /**
   * @return
   */
  public String getChannel() {
    return channel;
  }

  /**
   * @return
   */
  public String getSrvName() {
    return srvName;
  }
  
  public void changeChannel(String channel) {
    partChannel(this.channel);    
    User[] users = super.getUsers(this.channel);
    for(int i=0 ; i< users.length ; i++) {
      listener.clientExited(users[i].getNick());
    }
    this.channel = channel;
    joinChannel(this.channel);
  }

}
