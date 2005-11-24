/**
 * 
 */
package org.gudy.azureus2.core3.logging;

/**
 * @author TuxPaper
 * @since 2.3.0.7
 */
public interface ILogEventListener
{
  /** A LogEvent has been generated.
   * 
   * @param event The newly generated LogEvent
   */
  public void log(LogEvent event);
}
