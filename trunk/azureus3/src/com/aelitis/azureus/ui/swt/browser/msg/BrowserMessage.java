/*
 * Created on Jun 29, 2006 10:16:26 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.browser.msg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;
import org.json.JSONArray;
import org.json.JSONObject;

import com.aelitis.azureus.util.JSFunctionParametersParser;

/**
 * Holds a message being dispatched to a {@link MessageListener}.
 * 
 * @author dharkness
 * @created Jul 18, 2006
 */
public class BrowserMessage
{
    /** All messages must start with this prefix. */
    public static final String MESSAGE_PREFIX = "AZMSG";

    /** Separates prefix and listener ID from rest of message. */
    public static final String MESSAGE_DELIM = ";";

    /** There were no parameters passed with the message. */
    public static final int NO_PARAM = 0;

    /** Parameters were an encoded JSONObject. */
    public static final int OBJECT_PARAM = 1;

    /** Parameters were an encoded JSONArray. */
    public static final int ARRAY_PARAM = 2;

    /** Parameters were an encoded list. */
    public static final int LIST_PARAM = 3;


    private int    sequence;
    private String listenerId;
    private String operationId;
    private String params;
    
    private int paramType;
    private Object decodedParams;

		private String sFullMessage;
		
		private ArrayList completionListeners = new ArrayList();

		private boolean completed;

		private boolean completeDelayed;

    public BrowserMessage ( String sMsg ) {
        if ( sMsg == null ) {
            throw new IllegalArgumentException("event must be non-null");
        }
        
        this.sFullMessage = sMsg;
        parse();
    }


    static int seqFake = 1;
    /**
     * Parses the full message into its component parts.
     * 
     * @throws IllegalArgumentException 
     *              if the message cannot be parsed
     */
    protected void parse ( ) {
        String text = sFullMessage;
        
        // DJH: StringTokenizer was not used so that listeners
        //      could define their message format
        int delimSeqNum = text.indexOf(MESSAGE_DELIM);
        if ( delimSeqNum == -1 ) {
            throw new IllegalArgumentException("Message has no delimeters: " + text);
        }
        if ( delimSeqNum == text.length() - 1 ) {
            throw new IllegalArgumentException("Message has no sequence number: " + text);
        }
        
        int delimListener = text.indexOf(MESSAGE_DELIM, delimSeqNum + 1);
        if ( delimListener == -1 || delimListener == text.length() - 1 ) {
            throw new IllegalArgumentException("Message has no listener ID: " + text);
        }
        try {
            sequence = Integer.parseInt(text.substring(delimSeqNum + 1, delimListener));
        }
        catch ( NumberFormatException e ) {
        	System.err.println("Plese put the throw back in once souk fixes the seq # bug");
        	sequence = seqFake++;
            //throw new IllegalArgumentException("Message has no sequence number: " + text);
        }
        
        int delimOperation = text.indexOf(MESSAGE_DELIM, delimListener + 1);
        if ( delimOperation == -1 || delimOperation == text.length() - 1 ) {
            // listener ID without operation
            throw new IllegalArgumentException("Message has no operation ID: " + text);
        }
        
        listenerId = text.substring(delimListener + 1, delimOperation);
        
        int delimParams = text.indexOf(MESSAGE_DELIM, delimOperation + 1);
        if ( delimParams == -1 ) {
            // operation without parameters
            operationId = text.substring(delimOperation + 1);
        }
        else if ( delimParams == text.length() - 1 ) {
            // operation without parameters
            operationId = text.substring(delimOperation + 1, delimParams);
            params = null;
            paramType = NO_PARAM;
            decodedParams = null;
        }
        else {
            // operation with parameters
            operationId = text.substring(delimOperation + 1, delimParams);
            params = text.substring(delimParams + 1);
            char leading = params.charAt(0);
            switch ( leading ) {
            case '{':
                paramType = OBJECT_PARAM;
                decodedParams = new JSONObject(params);
                break;

            case '[':
                paramType = ARRAY_PARAM;
                decodedParams = new JSONArray(params);
                break;

            default:
                paramType = LIST_PARAM;
                decodedParams = JSFunctionParametersParser.parse(params);
                break;
            }
        }
    }

/*
    public StatusTextEvent getMessage( ) {
        return event;
    }


    public Browser getBrowser ( ) {
        return (Browser) event.widget;
    }

    public Object getBrowserData ( String key ) {
        return getBrowser().getData(key);
    }

    public void setBrowserData ( String key , Object value ) {
        getBrowser().setData(key, value);
    }

    public void executeInBrowser ( String javascript ) {
        getBrowser().execute(javascript);
    }
*/
    public int getSequence ( ) {
        return sequence;
    }

    public String getFullMessage ( ) {
        return sFullMessage;
    }

    public String getListenerId ( ) {
        return listenerId;
    }

    public String getOperationId ( ) {
        return operationId;
    }


    public boolean isParamObject ( ) {
        return paramType == OBJECT_PARAM;
    }
    
    public boolean isParamArray ( ) {
        return paramType == ARRAY_PARAM;
    }
    
    public boolean isParamList ( ) {
        return paramType == LIST_PARAM;
    }
    
    public String getParams ( ) {
        return params;
    }
    
    public JSONObject getDecodedObject ( ) {
        if ( ! isParamObject() ) {
            throw new IllegalStateException("Decoded parameter is not a JSONObject");
        }
        return (JSONObject) decodedParams;
    }

    public JSONArray getDecodedArray ( ) {
        if ( ! isParamArray() ) {
            throw new IllegalStateException("Decoded parameter is not a JSONArray");
        }
        return (JSONArray) decodedParams;
    }

    public List getDecodedList ( ) {
        if ( ! isParamList() ) {
            throw new IllegalStateException("Decoded parameter is not a List");
        }
        return (List) decodedParams;
    }

    public void addCompletionListener(MessageCompletionListener l) {
    	completionListeners.add(l);
    }
    
    public void removeCompletionListener(MessageCompletionListener l) {
    	completionListeners.remove(l);
    }
    
    private void triggerCompletionListeners(boolean success, Object data) {
		for (Iterator iterator = completionListeners.iterator(); iterator.hasNext();) {
			MessageCompletionListener l = (MessageCompletionListener) iterator.next();
			try {
				l.completed(success, data);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}
    
    /**
     * Sets the message complete and fires of the listeners who are waiting
     * for a response.
     * 
     * @param bOnlyNonDelayed Only mark complete if this message does not have a delayed reponse
     * @param success Success level of the message
     * @param data Any data the message results wants to send
     */
    public void complete(boolean bOnlyNonDelayed, boolean success, Object data) {
    	System.out.println("complete called with " + bOnlyNonDelayed);
    	if (completed || (bOnlyNonDelayed && completeDelayed)) {
    		System.out.println("exit early" + completed);
    		return;
    	}
    	triggerCompletionListeners(success, data);
    	completed = true;
    }
    
    public void setCompleteDelayed(boolean bCompleteDelayed) {
    	completeDelayed = bCompleteDelayed;
    }

    public String toString ( ) {
        return "[" + sequence + "] " + listenerId + "." 
                + operationId + "(" + params + ")";
    }
}
