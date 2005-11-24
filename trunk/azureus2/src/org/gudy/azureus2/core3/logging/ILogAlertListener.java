/**
 * 
 */
package org.gudy.azureus2.core3.logging;

/**
 * @author TuxPaper
 * @since 2.3.0.7
 */
public interface ILogAlertListener
{
  /** An alert has been generated
   * 
   * @param alert LogAlert that was generated
   */
  public void alertRaised(LogAlert alert);
}
