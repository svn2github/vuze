/* Written and copyright 2001 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 * Title:        jHTTPp2: Java HTTP Filter Proxy
 * Description: An OpenSource HTTP Proxy
 * Copyright:    Copyright (c) 2001 Benjamin Kohl
 * @author Benjamin Kohl
 */
package org.gudy.azureus2.server;

public class OnURLAction implements java.io.Serializable
{

  private String customerrortext, desc, httppath, newlocation;
  private boolean log,block,customtext,http_rq,anotherlocation;
  public OnURLAction(String desc)
  {
    this.desc=desc;
  }
  public void denyAccess(String customerrortext)
  {
    this.block=true;
    this.customtext=true;
    this.customerrortext=customerrortext;
  }
  public void denyAccess()
  {
    block=true;
  }
  public void logAccess()
  {
    log=true;
  }
  public void anotherLocation(String newlocation)
  {
    this.anotherlocation=true;
    this.newlocation=newlocation;
  }

  public boolean onAccesssDeny()
  {
    return block;
  }
  public boolean onAccessLog()
  {
    return log;
  }
  public boolean onAccessDenyWithCustomText()
  {
    return  customtext;
  }
  public boolean onAccessSendHTTPRequest()
  {
    return http_rq;
  }
  public boolean onAccessRedirect()
  {
    return this.anotherlocation;
  }
  public String newLocation()
  {
    return this.newlocation;
  }
  public void setHTTPAction(boolean http_rq, String httppath)
  {
    this.http_rq=http_rq;
    this.httppath=httppath;
  }
  public String getCustomErrorText()
  {
    return customerrortext;
  }
  public String getDescription()
  {
    return desc;
  }
  public String toString()
  {
    return desc;
  }

}