/*
 * Created on 6 sept. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.text.Collator;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.irc.IrcClient;
import org.gudy.azureus2.irc.IrcListener;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author Olivier
 * 
 */
public class IrcView extends AbstractIView implements IrcListener {

  Display display;
  Composite cIrc;
  StyledText consoleText;
  List users;
  Label userSumUp;
  Text inputField;
  Color[] colors;

  IrcClient client;
  boolean newMessage;
  
  private String lastPrivate;

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    display = composite.getDisplay();
    cIrc = new Composite(composite,SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = false;
    cIrc.setLayout(layout);
    consoleText = new StyledText(cIrc, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    consoleText.setWordWrap(true);
    GridData gridData = new GridData(GridData.FILL_BOTH | GridData.CENTER);
    gridData.grabExcessHorizontalSpace = true;
    consoleText.setLayoutData(gridData);
    users = new List(cIrc, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL );
    gridData = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_END | GridData.END);
    gridData.widthHint = 120;
    users.setLayoutData(gridData);
    inputField = new Text(cIrc, SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    //gridData.horizontalSpan = 2;
    inputField.setLayoutData(gridData);
    inputField.addKeyListener(new KeyAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
       */
      public void keyReleased(KeyEvent event) {
        if (event.keyCode == 13) {
          String text = inputField.getText();
          inputField.setText("");
          sendMessage(text);
        }
      }
    });
    inputField.setFocus();
    userSumUp = new Label(cIrc, SWT.NONE);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.widthHint = 120;
    userSumUp.setLayoutData(gridData);
    colors = new Color[4];
    colors[0] = MainWindow.blues[3];
    colors[1] = MainWindow.blues[2];
    colors[2] = MainWindow.blues[1];
    colors[3] = MainWindow.red_ConsoleView;
    client = new IrcClient(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return cIrc;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    newMessage = false;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    Thread t = new Thread() {
      public void run() {
        MainWindow.getWindow().setIrc(null);
        client.close();
      }
    };
    t.start();
    Utils.disposeComposite(cIrc);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    String result = MessageText.getString("IrcView.title.short");
    if(client != null) {
      result += " " + client.getChannel() + " on " + client.getSrvName();
    }     
    if(newMessage && (System.currentTimeMillis() / 1000)%2 == 0)
      result += " !";   
    else
      result += "  ";
    return result; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    String result = MessageText.getString("IrcView.title.full") + " ";
    return  result ; //$NON-NLS-1$
  }

  public void messageReceived(String sender, String message) {
    doLog(2, "<" + sender + "> " + message);
    newMessage = true;
  }

  public void systemMessage(String message) {
    doLog(0, message);
  }

  private void doLog(final int _color, final String _text) {
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new Runnable() {
      public void run() {
        if (consoleText == null || consoleText.isDisposed())
          return;
        ScrollBar sb = consoleText.getVerticalBar();

        int nbLines = consoleText.getLineCount();
        if (nbLines > 4096 + 256)
          consoleText.replaceTextRange(0, consoleText.getOffsetAtLine(256), ""); //$NON-NLS-1$
        Calendar now = GregorianCalendar.getInstance();
        String timeStamp = "[" + now.get(Calendar.HOUR_OF_DAY) + ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "]  "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        nbLines = consoleText.getLineCount();
        consoleText.append(timeStamp + _text + "\n"); //$NON-NLS-1$
        consoleText.setLineBackground(nbLines - 1, 1, colors[_color]);        
        consoleText.setTopIndex(nbLines);
      }
    });
  }

  private String format(int n) {
    if (n < 10)
      return "0" + n; //$NON-NLS-1$
    return "" + n; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.irc.IrcListener#action(java.lang.String, java.lang.String)
   */
  public void action(String sender, String action) {
    doLog(1, sender + " " + action);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.irc.IrcListener#clientEntered(java.lang.String)
   */
  public void clientEntered(final String client) {
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new Runnable() {
      /* (non-Javadoc)
       * @see java.lang.Runnable#run()
       */
      public void run() {
        if (users != null && !users.isDisposed()) {
          int index = users.indexOf(client);
          if (index == -1) {
            Collator collator = Collator.getInstance(Locale.getDefault());
            String items[] = users.getItems();
            int i = 0;
            for (; i < items.length; i++) {
              if (collator.compare(client, items[i]) < 0) {
                users.add(client, i);
                break;
              }
            }
            if (i == items.length) {
              users.add(client);
            }
            int nbUsers = users.getItemCount();
            if (userSumUp != null && !userSumUp.isDisposed()) {
              userSumUp.setText(nbUsers + " " + MessageText.getString("IrcView.clientsconnected"));
            }
          }
        }
      }
    });
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.irc.IrcListener#clientExited(java.lang.String)
   */
  public void clientExited(final String client) {
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new Runnable() {
      /* (non-Javadoc)
       * @see java.lang.Runnable#run()
       */
      public void run() {
        if (users != null && !users.isDisposed()) {
          int index = users.indexOf(client);
          if (index != -1) {
            users.remove(index);
          }
          int nbUsers = users.getItemCount();
          if (userSumUp != null && !userSumUp.isDisposed()) {
            userSumUp.setText(nbUsers + " " + MessageText.getString("IrcView.clientsconnected"));
          }
        }
      }
    });

  }

  private void sendMessage(String text) {
    if (text.equals(""))
      return;  
    if (text.startsWith("/")) {
      if(text.equals("/help")) {
        doLog(0,MessageText.getString("IrcView.help"));
      } else if (text.startsWith("/nick ") || text.startsWith("/name ")) {
        String newNick = text.substring(6).trim();
        client.setUserName(newNick);
      } else if (text.startsWith("/me ")) {
        String action = text.substring(4).trim();
        client.sendAction(action);
        action(client.getUserName(), action);
      } else if(text.startsWith("/msg ") || text.startsWith("/to ")) {
        StringTokenizer st = new StringTokenizer(text," ");
        st.nextToken();
        try {
        String target = st.nextToken();
        String message = "";
        while(st.hasMoreElements()) {
          message += st.nextElement() + " ";
        }
        client.sendMessage(target,message);
        doLog(3,MessageText.getString("IrcView.privateto") + " *" + target + "* " + message);
        } catch(Exception e) {
          doLog(0,MessageText.getString("IrcView.errormsg"));
        } 
      } else if(text.startsWith("/r ")) {
        if(lastPrivate != null) {
          String message = text.substring(3).trim();
          client.sendMessage(lastPrivate,message);
          doLog(3,MessageText.getString("IrcView.privateto") + " *" + lastPrivate + "* " + message);
        }
      } 
      else if(text.startsWith("/join "))
        {
          String channel = text.substring(6).trim();
          client.changeChannel(channel);
        }
      
      else {
        systemMessage(MessageText.getString("IrcView.actionnotsupported"));
      }
    }
    else {
      client.sendMessage(text);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.irc.IrcListener#notice(java.lang.String, java.lang.String)
   */
  public void notice(String sender, String message) {
    doLog(3,MessageText.getString("IrcView.noticefrom") + " -" + sender + "- " + message);
    newMessage = true;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.irc.IrcListener#privateMessage(java.lang.String, java.lang.String)
   */
  public void privateMessage(String sender, String message) {
    doLog(3,MessageText.getString("IrcView.privatefrom") + " *" + sender + "* " + message);
    lastPrivate = sender;
    newMessage = true;
  }

}
