/*  Connection.java -- classes and functions to handle client connections
 
    Copyright (C) 2000 - 2001 Jan De Luyck & Kris Van Hulle
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.server;

import java.net.*;
import java.io.*;
import java.util.*;

import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.ConfigurationManager;

public class Connection {
  private InputHandler input;
  private OutputHandler output;
  
  private String httpMethod;
  private String httpURI;
  private String httpURIPath;
  private HashMap httpURIVars = new HashMap();
  
  private String httpVersion;
  private int httpMajor;
  private int httpMinor;
  
  private String protocol;
  
  private Map httpHeaders = new HashMap();
  
  private InetAddress remoteIP;
  
  private GlobalManager gm;
  private MainServer server;
  
  Connection(InputHandler c_input, OutputHandler c_output, InetAddress sock_remoteIP, GlobalManager _gm, MainServer _server)
  /* class constructor */
  {
    this.remoteIP = sock_remoteIP;
    this.input = c_input;
    this.output = c_output;
    
    this.gm = _gm;
    this.server = _server;
  }
  
  public String connectionType() {
    String conType = (String) httpHeaders.get("connection");
    conType = conType.toLowerCase().trim();
    if (conType == null) conType = "close";
    
    return conType;
  }
  
  /****************************************************************************************************
   ****************************************************************************************************
   ****************************************************************************************************/
  
  public void processRequest() throws UnknownProtocolException, NoDataReceivedException,
  ObsoleteHTTPBrowserException, RFCViolationException
        /*	processes incoming requests from clients.
                This function is called first on connection of a new thread.
                Extracts the various parts from the messages that are gotten from the client.
         */
  {
    this.server.putSysMessage(SLevel.INFO, "Going in for next request handling...");
    
    String aRequest = "";
    
    /* get the line, but get rid of the (possibly) empty line */
    try {
      do {
        aRequest = input.readLine();
      } while ((aRequest.length() == 0));
    }
    catch (NullPointerException e) {
      this.server.putSysMessage(SLevel.WARNING, "readLine:" +  e);
      aRequest = "";
    }
    
        /* now we must 'process' what we've gotten... If it is a normal request, it would normally
           start with <method> <file> HTTP/X.Y
           X -> HttpMajor, Y -> HttpMinor (version levels)
           There are some things that you normally have to keep in mind concerning http protocol
           versions: I've read that version 0.9 doesn't support several things, amongst which the
           Status codes (500 - server made a booboo e.g.)
         */
    
    if (aRequest.length() == 0)
      throw new NoDataReceivedException();
    
    StringTokenizer requestInTokens = new StringTokenizer(aRequest);
    httpMethod = requestInTokens.nextToken();
    
    if (requestInTokens.hasMoreTokens() == true)
      try {
        httpURI = URLDecoder.decode(requestInTokens.nextToken(), "US-ASCII");
      }
      catch (UnsupportedEncodingException e) {
        httpURI = "/";
      }
    else
      httpURI = "/";
    
        /*remove last '/' (as many as there are) from the URI
          not strictly necessary, and i guess it can be done it a way that looks 'better'
              but it's friggin' late and i don't feel like searching too long on it :-) */
    while (httpURI.length() > 1 && httpURI.charAt(httpURI.length() - 1) == '/')
      httpURI = httpURI.substring(0,httpURI.length() - 1);
    
    /* check if there are any variables passed, we need to split them off */
    if (httpURI.indexOf('?') != -1) {
      httpURIPath = httpURI.substring(0, httpURI.indexOf('?'));
      String httpURIVarsTemp = httpURI.substring(httpURI.indexOf('?') + 1);
      StringTokenizer URIVars = new StringTokenizer(httpURIVarsTemp, "&");
      while (URIVars.hasMoreTokens()) {
        String URIVar = URIVars.nextToken();
        try {
          httpURIVars.put(URLDecoder.decode(URIVar.substring(0, URIVar.indexOf('=')),"UTF-8"), URLDecoder.decode(URIVar.substring(URIVar.indexOf('=')+1),"UTF-8"));
        } catch (Exception e) {}
      }
    }
    else
      httpURIPath = httpURI;
    
    if (requestInTokens.hasMoreTokens() == true)
      httpVersion = requestInTokens.nextToken();
    else
      httpVersion = "HTTP/0.9";
    
    StringTokenizer versionInTokens = new StringTokenizer(httpVersion,"/.");
    protocol = versionInTokens.nextToken();
    
    if (protocol.equals("Secure-HTTP") == true) {
      // do secure http processing here
      this.server.putSysMessage(SLevel.INFO,"SECURE HTTP REQUEST");
    }
    else {
      if (protocol.equals("HTTP") != true) {
                    /* throw an error in the ring
                           we can only handle http requests, we're not going to turn this thing into a
                            multispacial multiphasic multidimensional server toolkit :-) */
        output.outputError(601,protocol);
        throw new UnknownProtocolException(protocol);
      }
      else {
        /* get HTTP version */
        httpMajor = Integer.parseInt(versionInTokens.nextToken());
        httpMinor = Integer.parseInt(versionInTokens.nextToken());
        
        if (httpMajor < 1)	/* oops... Version is lower than 1.0... Tell OutputHandler not
                                               to send any headers!*/
        {
          output.sendHeaders(false);
        }
        
        String header = input.readLine();
        String name = null;
        String value = "";
        
        this.server.putSysMessage(SLevel.HTTP,aRequest);
        
        while (header != null && header.length() > 0) {
                                    /*process the rest of the headers
                                HTTP/1.1 requires a 'host' header to be sent, check on this one */
          
          int positionOfColon = header.indexOf(":");
          /*convert header-key to lowercase to prevent problems*/
          if (positionOfColon != -1)
            httpHeaders.put(header.substring(0,positionOfColon).trim().toLowerCase(), header.substring(positionOfColon + 1).trim());
          
          this.server.putSysMessage(SLevel.HTTP,header);
          header = input.readLine();
        }
        
        char[] buf;
        
        if (httpHeaders.containsKey("content-length")) {
          int length = Integer.decode((String) httpHeaders.get("content-length")).intValue();
          buf = new char[length];
          input.read(buf, 0, length);
          String postvars = new String(buf);
          StringTokenizer URIVars = new StringTokenizer(postvars, "&");
          while (URIVars.hasMoreTokens()) {
            String URIVar = URIVars.nextToken();
            try {
              httpURIVars.put(URLDecoder.decode(URIVar.substring(0, URIVar.indexOf('=')),"UTF-8"), URLDecoder.decode(URIVar.substring(URIVar.indexOf('=')+1),"UTF-8"));
            } catch (Exception e) {}
          }
        }
        
        output.setHttpHeaders(httpHeaders);
        
        /* if all headers are passed us, we check if the 'host' has been sent, as requred in the HTTP/1.1 specs */
        if ((httpMajor > 0) && (httpMinor > 0) && (httpHeaders.containsKey("host") == false)) {
          output.outputError(400,"");
          throw new RFCViolationException("RFC2068");
        }
        
        /* put Connection: close header in if it isn't there */
        if (httpHeaders.containsKey("connection") == false)
          httpHeaders.put("connection","close");
      }
    }
  }
  
  /*********************************************************************************************************
   *********************************************************************************************************
   *********************************************************************************************************/
  
  
  
  /* Servicer part - actually services the connection */
  
  public void doMagicStuff()
        /* This procedure gets and outputs the thing(s) requested by the client.
                If someone wants to implement other methods, it is in this function that you're needed :-)
         */
  {
    this.server.putSysMessage(SLevel.INFO,"Doing Magic Stuff :-)");
    
    this.server.putSysMessage(SLevel.INFO, "Method Requested is: " + httpMethod);
    
    HashMap methodList = new HashMap();
    methodList.put("GET","0");
    methodList.put("HEAD","1");
    methodList.put("POST","2");
    methodList.put("TRACE","3");
    
    //        String test = methodList.get(httpMethod).toString();
    
    if (methodList.get(httpMethod) != null) {
      /* look if we can find the file requested */
      //            FileFinder findFile = new FileFinder(httpURIPath);
      int methodID = Integer.parseInt(methodList.get(httpMethod).toString());
      switch(methodID) {
        case 0: /* START OF GET METHOD */
        case 1: /* START OF HEAD METHOD */
        case 2: /* START OF POST METHOD */ {
          if (methodID == 1)
            output.sendBody(false);    /* for HEAD method, we don't send body's out) */
          if (this.server.allowedURLs.contains(httpURIPath)) {
            this.server.putSysMessage(SLevel.INFO,"Requested "+httpURIPath);
            output.Process(httpURIVars);
            if (httpURIPath.equals("/favicon.ico"))
              output.ProcessAndOutputFile(new File("org/gudy/azureus2/ui/icons/azureus.ico"), httpMethod, httpURIVars, remoteIP);
            else
              output.ProcessAndOutput(httpURIPath, httpMethod, httpURIVars, remoteIP);
          }
          else {
            this.server.putSysMessage(SLevel.INFO,"Requested file was NOT found.");
            output.outputError(404,httpURI);
          }
          //                    switch (findFile.status()) {
          //                        case -1:    /* nothing found */ {
          //                            this.server.putSysMessage(0,"Requested file was NOT found.");
          //                            output.outputError(404,httpURI);
          //                            break;
          //                        }
          //                        case 0:     /* must send dirlisting */ {
          //                            this.server.putSysMessage(0,"Index file not found, but dir listing sent.");
          //                            output.outputDirectoryListing(httpURIPath);
          //                            break;
          //                        }
          //                        case 1:     /* must transmit file */ {
          //                            this.server.putSysMessage(0,"Wanted file found: " + findFile.theFile().getName());
          //                            output.ProcessAndOutputFile(findFile.theFile(), httpMethod, httpURIVars, remoteIP);
          //                            break;
          //                        }
          //                    }
          break;
        } /* END OF HEAD METHOD */
        /* END OF GET METHOD */
        
        
        case 3: /* START OF TRACE METHOD */ {
                    /* trace: respond with 200 OK, and send all the headers back that we've received
                              in the entity body */
          output.outputTrace(httpURI);
          break;
        } /* END OF TRACE METHOD */
      }
    }
    else {
      /* it is a method that is not implemented. Error on it. */
      output.outputError(501, httpMethod);
    }
  }
}