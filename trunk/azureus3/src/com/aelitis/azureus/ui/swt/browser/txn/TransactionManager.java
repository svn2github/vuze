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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.browser.Browser;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;

/**
 * Manages the context for a single SWT {@link Browser} component.
 * 
 * @author dharkness
 * @created Jul 19, 2006
 */
public class TransactionManager
{
    private ClientMessageContext context;
    
    // { "type" -> Constructor }
    private Map txnCtors = new HashMap();

    // { "type" -> Transaction }
    private Map txns = new HashMap();

    private int lastTxnId = 0;


    public TransactionManager ( ClientMessageContext context ) {
        this.context = context;
    }


    /**
     * Returns the current transaction of the given type.
     * 
     * @param type used to locate the transaction
     * @return the transaction or <code>null</code> if none exists
     */
    public synchronized Transaction getTransaction ( String type ) {
        return (Transaction) txns.get(type);
    }
    
    /**
     * Starts a new transaction of the given type after canceling
     * the current transaction if any.
     * 
     * @param type used to determine the transaction class to create
     * @return a new started transaction
     */
    public synchronized Transaction startTransaction ( String type ) {
        cancelTransaction(type);
        
        Transaction txn = createTransaction(type);
        if ( txn != null ) {
            if ( ! txn.start() ) {
                return null;
            }
            
            txns.put(type, txn);
        }
        
        return txn;
    }

    /**
     * Cancels the current transaction if there is one for this listener 
     * and the browser in the given message.
     * 
     * @param type used to determine the transaction to return
     * @return the current transaction or <code>null</code> if none exists
     */
    public synchronized Transaction cancelTransaction ( String type ) {
        Transaction txn = getTransaction(type);
        if ( txn != null ) {
            txn.cancel();
        }
        
        return txn;
    }

    /**
     * Removes the given transaction from current status if it's current.
     * 
     * @param txn the Transaction to be removed
     * @return <code>true</code> if the transaction was current; <code>false</code> otherwise
     */
    synchronized boolean removeTransaction ( Transaction txn ) {
        if ( txn != getTransaction(txn.getType()) ) {
            return false;
        }
        
        return txns.remove(txn.getType()) == txn;
    }


    /**
     * Registers the given transaction subclass as that to be created
     * for the given type identifier.
     * 
     * @param clazz used when creating transactions
     * 
     * @throws IllegalArgumentException if txnClass is null or not a subclass
     *              of {@link Transaction}
     * @throws IllegalArgumentException if the appropriate constructor 
     *              of txnClass cannot be found or accessed 
     */
    public void registerTransactionType ( String type , Class clazz ) {
        if ( clazz == null ) {
            throw new IllegalArgumentException("Transaction class must be non-null");
        }
        if ( ! Transaction.class.isAssignableFrom(clazz) ) {
            throw new IllegalArgumentException("Transaction class " + clazz.getName() 
                    + " must be a subclass of Transaction");
        }
        
        try {
            Class[] ctorParams = 
                    new Class[] { 
                        Integer.TYPE, 
                        String.class, 
                        ClientMessageContext.class 
                    };
            Constructor ctor = clazz.getConstructor(ctorParams);
            txnCtors.put(type, ctor);
        }
        catch ( Exception e ) {
            throw new IllegalArgumentException(
                    "Cannot access appropriate constructor for " 
                    + clazz.getName());
        }
    }

    /**
     * Creates a new transaction for this listener and the browser 
     * in the given message. If no transaction class was specified
     * in the constructor, subclasses must override this method.
     * 
     * @param message holds a reference to the browser
     * @return a new transaction
     */
    protected Transaction createTransaction ( String type ) {
        Constructor ctor = (Constructor) txnCtors.get(type);
        if ( ctor == null ) {
            throw new IllegalStateException("Unregistered transaction type: " + type);
        }
        
        try {
            Object[] params = 
                    new Object[] { 
                        new Integer(getNextTransactionId()), 
                        type, 
                        context
                    };
            return (Transaction) ctor.newInstance(params);
        }
        catch ( Exception e ) {
            throw new RuntimeException("Exception creating transaction for type " + type, e);
        }
    }

    /**
     * Increments the last transaction ID and returns the new value.
     * 
     * @return a unique ID to use for a new transaction
     */
    private int getNextTransactionId ( ) {
        return ++lastTxnId;
    }
}
