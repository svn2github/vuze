/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.ILoggerListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Olivier
 * @author James Yeh Additional documentation, log history, and memory usage enhancements
 */
public class ConsoleView extends AbstractIView {

  private Display display;
  private StyledText consoleText;
  private static Color[] colors;
  private static ConsoleView instance;

  private static final int PREFERRED_LINES = 256;
  private static final int MAX_LINES = 1024 + PREFERRED_LINES;
  private static final int COLORS_NUM = 4;

  // private static final List logHistory;
  private static final SimpleDateFormat dateFormatter;
  private static final FieldPosition formatPos;

  private static final AEMonitor classMon = new AEMonitor("ConsoleView:S");

  static {
      // logHistory = new LinkedList();
      dateFormatter = new SimpleDateFormat("[h:mm:ss]  ");
      formatPos = new FieldPosition(0);
  }

  /**
   * Preinitializes the ConsoleView logging handler. This method must be called before a ConsoleView instance is created.
   */
  public static void preInitialize() {
      LGLogger.setListener(new LogEventHandler());
  }

  /**
   * <p>Sets the singleton ConsoleView instance</p>
   * <p>If records exist in the session log history, it is logged to the text view</p>
   * @param view ConsoleView instance
   * @throws IllegalStateException If this method is called when the singleton instance has already been set
   */
  private static void setInstance(ConsoleView view) throws IllegalStateException {
      try
      {
          classMon.enter();

          if(ConsoleView.instance != null)
              throw new IllegalStateException("Only one ConsoleView is allowed");
          ConsoleView.instance = view;

          // latent initialization
          if(colors == null)
          {
              colors = new Color[COLORS_NUM];
              colors[0] = Colors.blues[3];
              colors[1] = Colors.blues[2];
              colors[2] = Colors.blues[1];
              colors[3] = Colors.red_ConsoleView;
          }

          // prefill history text
		  /*
          Iterator iter = logHistory.iterator();
          for(int i = 0; i < logHistory.size(); i++)
          {
              ConsoleView.instance.doLog((LogInfo)iter.next(), true);
          }

          if(logHistory.size() > 0)
          {
              ConsoleView.instance.display.asyncExec(new AERunnable()
              {
                  public void runSupport()
                  {
                      if(ConsoleView.instance != null && ConsoleView.instance.consoleText != null && !ConsoleView.instance.consoleText.isDisposed())
                        ConsoleView.instance.consoleText.setTopIndex(ConsoleView.instance.consoleText.getLineCount() - 1);
                  }
              });
          }
          */
		  
          // reset state
          ConsoleView.instance.consoleText.addDisposeListener(new DisposeListener() {
              public void widgetDisposed(DisposeEvent event)
              {
                  ConsoleView.instance = null;
              }
          });
      }
      finally
      {
          classMon.exit();
      }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    display = composite.getDisplay();
    consoleText = new StyledText(composite, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
    setInstance(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return consoleText;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {}

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
  	MainWindow.getWindow().getShell().setFocus();
    MainWindow.getWindow().setConsole(null);
    consoleText.dispose();    
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("ConsoleView.title.full"); //$NON-NLS-1$
  }

  private void doLog(final int color,final String text,final Date timestamp, final boolean supressScrolling) {
    if(display == null || display.isDisposed())
      return;
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        if (consoleText == null || consoleText.isDisposed())
          return;
        ScrollBar sb = consoleText.getVerticalBar();

        //System.out.println(sb.getSelection()+ "/" + (sb.getMaximum() - sb.getThumb()));
        boolean autoScroll = !supressScrolling && (sb.getSelection() == (sb.getMaximum() - sb.getThumb()));
        int nbLines = consoleText.getLineCount();
        if (nbLines > MAX_LINES)
          consoleText.replaceTextRange(0, consoleText.getOffsetAtLine(PREFERRED_LINES), ""); //$NON-NLS-1$

        StringBuffer buf = new StringBuffer();
        dateFormatter.format(timestamp, buf, formatPos);
        buf.append(text).append('\n');

        consoleText.append(String.valueOf(buf));
        nbLines = consoleText.getLineCount();
        consoleText.setLineBackground(nbLines - 2, 1, colors[color]);
        if (autoScroll)  consoleText.setSelection(consoleText.getText().length());
      }
    });
  }

  /**
   * <p>Appends the log information to the end of the history list</p>
   * <p>The size of the history list is kept at MAX_LINES maximum, inclusive. If the size exceeds MAX_LINES,
   * the first element in the list is removed before the append happens.</p>
   * @param info Log info
   */
  /*
  private static void appendLogHistory(LogInfo info)
  {
      try
      {
          classMon.enter();

          if(logHistory.size() > MAX_LINES - 1)
              logHistory.remove(0);

          logHistory.add(info);
      }
      finally
      {
          classMon.exit();
      }
  }
  */
  
  /**
   * Event handler used to remember the logging history of a session and logs it on screen when the view is opened
   */
  private static class LogEventHandler implements ILoggerListener
  {
      /**
       * {@inheritDoc}
       */
      public void log(int componentId, int event, int color, String text)
      {
          if (color >= 0 && color < COLORS_NUM)
          {
             //  appendLogHistory(info);

              if(ConsoleView.instance != null && ConsoleView.instance.display != null && !ConsoleView.instance.display.isDisposed())
              {
		          // LogInfo info = new LogInfo(color, text, new Date());
					 
                  ConsoleView.instance.doLog(color,text,new Date(), false);
              }
          }
      }
  }

  /**
   * Basic immutable log info model to store color code, timestamp, and the message contents
   */
  /*
  private static class LogInfo
  {
      private final int color;
      private final String text;
      private final Date timestamp;

  
      public LogInfo(int color, String text, Date timestamp)
      {
          this.color = color;
          this.text = text;
          this.timestamp = timestamp;
      }
  }
*/
}
