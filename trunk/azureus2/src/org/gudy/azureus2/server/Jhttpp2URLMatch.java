/**
 * Title:        jHTTPp2: Java HTTP Filter Proxy
 * Copyright:    Copyright (c) 2001-2003 Benjamin Kohl
 * @author Benjamin Kohl
 * @version 0.2.8
 */
package org.gudy.azureus2.server;

public class Jhttpp2URLMatch implements java.io.Serializable
{
  String match;
  String desc;
  boolean cookies_enabled;
  int actionindex;

  public Jhttpp2URLMatch(String match,boolean cookies_enabled,int actionindex,String description)
  {
    this.match=match;
    this.cookies_enabled=cookies_enabled;
    this.actionindex=actionindex;
    this.desc=description;
  }
  public String getMatch()
  {
    return match;
  }
  public boolean getCookiesEnabled()
  {
    return cookies_enabled;
  }
  public int getActionIndex()
  {
    return actionindex;
  }
  public String getDescription()
  {
    return desc;
  }
  public String toString()
  {
    return "\"" + match + "\" " + desc;
  }
}