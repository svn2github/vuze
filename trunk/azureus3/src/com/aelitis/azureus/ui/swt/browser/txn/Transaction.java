/*
 * Created on Jul 19, 2006 10:16:26 PM
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
package com.aelitis.azureus.ui.swt.browser.txn;

import java.util.Map;

import org.eclipse.swt.browser.Browser;

import com.aelitis.azureus.core.messenger.ClientMessageContext;

/**
 * Encapsulates a single transaction that interacts with a single
 * SWT {@link Browser}, possibly across multiple pages.
 * 
 * Transactions are started after being created and cannot be restarted.
 * They can be marked as cancelled or completed while running.
 * Once stopped, they are left as either completed or cancelled.
 * 
 * Subclasses may override starting, canceling, and stopping to perform
 * their own specific actions and return <code>false</code> to abort.
 * 
 * @author dharkness
 * @created Jul 19, 2006
 */
public abstract class Transaction
{
    private int id;
    private String type;
    private ClientMessageContext context;

    private boolean running = false;
    private boolean cancelled = false;
    private boolean completed = false;


    public Transaction ( int id , String type , ClientMessageContext context ) {
        this.id = id;
        this.type = type;
        this.context = context;
    }


    public int getId ( ) {
        return id;
    }

    public String getType ( ) {
        return type;
    }

    /**
     * Starts the transaction if it is not yet running, completed or cancelled.
     * A transaction may be started only once. After that, <code>false</code> is returned.
     */
    public boolean start ( ) {
        debug("start");
        if ( running || completed || cancelled ) {
            return false;
        }
        
        debug("starting");
        if ( ! starting() ) {
            return false;
        }
        
        debug("started");
        running = true;
        return true;
    }

    /**
     * Performs subclass-specific initialization to start the transaction.
     * Return <code>false</code> to abort the start.
     * 
     * @return whether or not the transaction can start
     */
    protected boolean starting ( ) {
        return true;
    }

    /**
     * Returns <code>true</code> if this transaction has started and is still running.
     */
    public boolean isRunning ( ) {
        return running;
    }


    /**
     * Marks this transaction as cancelled if it is running and not complete.
     * A transaction may be cancelled only once. After that, <code>false</code> is returned.
     */
    public boolean cancel ( ) {
        debug("cancel");
        if ( ! running || completed || cancelled ) {
            return false;
        }
        
        debug("canceling");
        if ( ! canceling() ) {
            return false;
        }
        
        debug("cancelled");
        cancelled = true;
        context.getTransactionManager().removeTransaction(this);
        
        return true;
    }

    /**
     * Performs subclass-specific initialization to cancel the transaction.
     * Return <code>false</code> to abort the cancellation.
     * 
     * @return whether or not the transaction can be cancelled
     */
    protected boolean canceling ( ) {
        return true;
    }

    /**
     * Returns <code>true</code> if this transaction has been cancelled.
     */
    public boolean isCancelled ( ) {
        return cancelled;
    }


    /**
     * Marks this transaction as completed if it is running and not cancelled.
     * If it is running and cancelled, it is simply marked as not running.
     * A transaction may be stopped only once. After that, <code>false</code> is returned.
     */
    protected boolean stop ( ) {
        debug("stop");
        if ( ! running ) {
            return false;
        }
        
        debug("stopping");
        if ( ! stopping() ) {
            return false;
        }
        
        debug("stopped");
        running = false;
        completed = ! cancelled;
        context.getTransactionManager().removeTransaction(this);
        
        return true;
    }

    /**
     * Performs subclass-specific initialization to stop the transaction.
     * Return <code>false</code> to abort the stop.
     * 
     * @return whether or not the transaction can be stopped
     */
    protected boolean stopping ( ) {
        return true;
    }


    /**
     * Returns <code>true</code> if this transaction has started and is still running.
     */
    public boolean isCompleted ( ) {
        return completed;
    }


    /**
     * Sends a message to the JavaScript in the page.
     * 
     * @param key identifies the listener to receive the message
     * @param op identifies the operation to perform
     */
    protected void sendBrowserMessage ( String key , String op ) {
        sendBrowserMessage(key, op, null);
    }
    
    /**
     * Sends a message to the JavaScript in the page.
     * 
     * @param key identifies the listener to receive the message
     * @param op identifies the operation to perform
     * @param params optional message parameters
     */
    protected void sendBrowserMessage ( String key , String op , Map params ) {
        if ( context.getTransaction(type) == this ) {
            context.sendBrowserMessage(key, op, params);
        }
        else {
            debug("Non-current transaction cannot send: " + key + "." + op);
        }
    }
    

    /**
     * Executes the given Javascript code in the browser.
     * 
     * @param javascript the code to execute
     */
    protected void executeInBrowser ( String javascript ) {
        if ( context.getTransaction(type) == this ) {
            context.executeInBrowser(javascript);
        }
        else {
            debug("Non-current transaction cannot execute: " + javascript);
        }
    }

    public String toString ( ) {
        return type + "-" + id
                + (running ? "-running" : "")
                + (cancelled ? "-cancelled" : "")
                + (completed ? "-completed" : "");
    }

    /**
     * Displays a debug message tagged with the context ID.
     * 
     * @param message sent to the debug log
     */
    protected void debug ( String message ) {
        context.debug("[" + this + "] " + message);
    }

    /**
     * Displays a debug message and exception tagged with the context ID.
     * 
     * @param message sent to the debug log
     * @param t exception to log with message
     */
    protected void debug ( String message , Throwable t ) {
        context.debug("[" + this + "] " + message, t);
    }
}
