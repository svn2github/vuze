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

import java.util.HashMap;
import java.util.Map;

import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;

/**
 * A listener that requires a {@link Transaction} before
 * dispatching messages.
 * 
 * @author dharkness
 * @created Jul 19, 2006
 */
public abstract class AbstractTransactionalListener extends AbstractMessageListener
{
    /** Start a new transaction or use an existing one (like J2EE REQUIRED) */
    public static final String TXN_REQUIRED = "Required";
    
    /** Start a new transaction, aborting any existing one (like J2EE REQUIRES_NEW) */
    public static final String TXN_REQUIRES_NEW = "RequiresNew";
    
    /** Require an existing transaction, erroring if there isn't one (like J2EE MANDATORY) */
    public static final String TXN_MANDATORY = "Mandatory";
    
    /** Handle without a transaction, ignoring any existing one (like J2EE NOT_SUPPORTED) */
    public static final String TXN_NONE = "NotSupported";


    private String txnType;

    // { "operation-id" -> "txn-state" }
    private Map txnStatesByOp = new HashMap();


    /**
     * Uses the listener's ID as the transaction type.
     * 
     * @param id uniquely identifies this listener to the dispatcher 
     *              and defines the transaction type
     */
    public AbstractTransactionalListener ( String id ) {
        this(id, id);
    }

    /**
     * Provides a transaction type different from the listener's ID.
     * 
     * @param id uniquely identifies this listener to the dispatcher
     * @param txnType used to access transactions via the context
     */
    public AbstractTransactionalListener ( String id , String txnType ) {
        super(id);
        this.txnType = txnType;
    }


    /**
     * Returns the transaction state (attribute)
     * @param operationId
     * @return
     */
    protected String getOperationTxnState ( String operationId ) {
        return (String) txnStatesByOp.get(operationId);
    }

    protected void registerOperationTxnRequired ( String operationId ) {
        registerOperation(operationId, TXN_REQUIRED);
    }

    protected void registerOperationTxnRequiresNew ( String operationId ) {
        registerOperation(operationId, TXN_REQUIRES_NEW);
    }

    protected void registerOperationTxnMandatory ( String operationId ) {
        registerOperation(operationId, TXN_MANDATORY);
    }

    protected void registerOperationTxnNone ( String operationId ) {
        registerOperation(operationId, TXN_NONE);
    }

    private void registerOperation ( String operationId , String txnState ) {
        txnStatesByOp.put(operationId, txnState);
    }


    /**
     * Returns the transaction type used by this listener.
     * 
     * @return the transaction type used by this listener
     */
    public String getTransactionType ( ) {
        return txnType;
    }

    /**
     * Returns the current transaction for this listener.
     * 
     * @return the current transaction or <code>null</code> if none exists
     */
    protected Transaction getTransaction ( ) {
        return context.getTransaction(txnType);
    }
    
    /**
     * Starts a new transaction for this listener.
     * 
     * @return a new started transaction
     */
    protected Transaction startTransaction ( ) {
        return context.startTransaction(txnType);
    }

    /**
     * Cancels the current transaction if there is one for this listener.
     * 
     * @return the cancelled transaction or <code>null</code> if none exists
     */
    protected Transaction cancelTransaction ( ) {
        return context.cancelTransaction(txnType);
    }


    /**
     * Ensures the correct transaction state before handling the message.
     * 
     * @param message contains the operation ID used to lookup the transaction state
     */
    public void handleMessage ( BrowserMessage message ) {
        String state = getOperationTxnState(message.getOperationId());
        
        if ( state == null ) {
            context.debug("Ignoring message without transactional state: " + message);
            return;
        }
        
        Transaction txn = getTransaction();
        if ( state.equals(TXN_REQUIRED) ) {
            // start new if none exists
            if ( txn == null ) {
                txn = startTransaction();
            }
        }
        else if ( state.equals(TXN_REQUIRES_NEW) ) {
            // cancel current and start new
            cancelTransaction();
            txn = startTransaction();
        }
        else if ( state.equals(TXN_MANDATORY) ) {
            // use existing or error
            if ( txn == null ) {
                transactionStateViolated(message, state, null);
                return;
            }
        }
        else if ( state.equals(TXN_NONE) ) {
            // ignore existing
            if ( txn != null ) {
                txn = null;
            }
        }
        else {
            context.debug("Ignoring message with invalid transactional state (" + state + "): " + message);
            return;
        }
        
        if ( txn != null ) {
            handleTxnlMessage(message, txn);
        }
        else {
            handleNonTxnlMessage(message);
        }
    }

    /**
     * Handle a browser message that require a transaction. Subclasses must
     * implement this method, otherwise they should extend {@link AbstractMessageListener}.
     * 
     * @param message the mesage to be handled
     * @param txn the current transaction
     */
    protected abstract void handleTxnlMessage ( BrowserMessage message , Transaction txn ) ;

    /**
     * Handle a browser message that doesn't require a transaction.
     * Subclasses must override if they have any non-transactional operations.
     * 
     * @param message the mesage to be handled
     */
    protected void handleNonTxnlMessage ( BrowserMessage message ) {
        
    }

    /**
     * Called when the necessary transactional state is not correct
     * for the given message. Override to handle the error more gracefully.
     * 
     * @param message the message being handled
     * @param state the state required by the message
     * @param txn the existing transaction or <code>null</code> if none
     */
    protected void transactionStateViolated ( BrowserMessage message , String state , Transaction txn ) {
        throw new IllegalStateException("Transaction state violated - state: " + state + ", txn: " + txn);
    }
}
