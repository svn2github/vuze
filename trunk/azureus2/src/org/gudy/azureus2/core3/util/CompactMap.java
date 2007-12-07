/*
 * Created on Dec 4, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.core3.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class 
CompactMap 
	implements Map
{
	public static Map
	cloneMap(
		Map		m )
	{
		if ( m instanceof CompactMap ){
		
			HashMap	result = new HashMap();
			
			((CompactMap)m).putAllTo( result );
			
			return( result );
			
		}else{
			
			return( new HashMap( m ));
		}
	}
	
	private volatile Object	_impl;
	
	public
	CompactMap(
		Map		basis )
	{
		Set entries = basis.entrySet();
		
		Object[][]	impl = new Object[entries.size()][];
		
		Iterator it = entries.iterator();
		
		int	pos = 0;
		
		while( it.hasNext()){
			
			Map.Entry entry = (Map.Entry)it.next();
			
			impl[pos++] = new Object[]{ entry.getKey(), entry.getValue() };
		}
		
		_impl	= impl;
	}
	
	private void
	modified()
	{
		Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			return;
		}
		
		Map	m = new HashMap();
		
		Object[][]	compact = (Object[][])impl;
		
		for (int i=0;i<compact.length;i++){
			
			m.put( compact[i][0], compact[i][1]);
		}
		
		_impl	= m;
	}
	
    public int 
    size()
    {
		Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			return(((Map)impl).size());
			
		}else{
			
			return(((Object[][])impl).length );
		}
    }

    public boolean 
    isEmpty()
    {
    	return( size() == 0 );
    }

 
    public boolean 
    containsKey(
    	Object key )
    {
		Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			return(((Map)impl).containsKey( key ));
			
		}else{
			
			Object[][] compact = (Object[][])impl;
			
			for (int i=0;i<compact.length;i++){
				
				Object k = compact[i][0];
				
				if ( k == key || ( k != null && k.equals( key ))){
					
					return( true );
				}
			}
			
			return( false );
		}
    }
 
    public boolean 
    containsValue(
    	Object value )
    {
		Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			return(((Map)impl).containsValue( value ));
			
		}else{
			
			Object[][] compact = (Object[][])impl;
			
			for (int i=0;i<compact.length;i++){
				
				Object v = compact[i][1];
				
				if ( v == value || ( v != null && v.equals( value ))){
					
					return( true );
				}
			}
			
			return( false );
		}
    }

  
    public Object 
    get(
    	Object key )
    {
    	Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			return(((Map)impl).get( key ));
			
		}else{
			
			Object[][] compact = (Object[][])impl;
			
			for (int i=0;i<compact.length;i++){
				
				Object k = compact[i][0];
				
				if ( k == key || ( k != null && k.equals( key ))){
					
					return( compact[i][1] );
				}
			}
			
			return( null );
		}
    }

 
    public Object 
    put(
    	Object key, 
    	Object value )
    {
    	modified();
    	
    	return(((Map)_impl).put( key, value ));
    }


    public Object 
    remove(
    	Object key )
    {
    	modified();
    	
    	return(((Map)_impl).remove( key ));
    }

    public void 
    putAll(
    	Map t )
    {
    	modified();
    	
    	((Map)_impl).putAll( t );
    }

    public void
    putAllTo(
    	Map		target )
    {
    	Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			target.putAll( ((Map)impl));
			
		}else{
			
			Object[][] compact = (Object[][])impl;
			
			for (int i=0;i<compact.length;i++){
				
				target.put( compact[i][0], compact[i][1]);
			}
		}
    }
    
    public void 
    clear()
    {
    	modified();
    	
    	((Map)_impl).clear();
    }

    public Set 
    keySet()
    {
    	modified();
    	
    	return(((Map)_impl).keySet());	
    }

    public Iterator
    getKeySetIterator()
    {
    	Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			return(((Map)impl).keySet().iterator());
			
		}else{
			
			return( new compactKeySetIterator());
		}
    }
    
    public Collection 
    values()
    {
    	modified();
    	
    	return(((Map)_impl).values());
    }

    public Set 
    entrySet()
    {
    	modified();
    	
    	return(((Map)_impl).entrySet());
    }
    
    public String
    toString()
    {
    	Object	impl = _impl;
		
		if ( impl instanceof Map ){
			
			return(((Map)impl).toString());
			
		}else{
			
			String	str = "{";
			
			Object[][] compact = (Object[][])impl;
			
			for (int i=0;i<compact.length;i++){
				
				str += (i==0?"":",") + compact[i][0] + "=" + compact[i][1];
			}
			
			return( str + "}" );
		}
    }
    
    protected class
    compactKeySetIterator
    	implements Iterator
    {
    	private volatile Iterator	_it_impl;
    	
    	int	pos = 0;
    	
    	private void
    	itModified()
    	{
    		modified();
    		
    		Iterator it_impl = keySet().iterator();
    		
    			// resync the iterator
    		
    		for (int i=0;i<pos;i++ ){
    			
    			if ( it_impl.hasNext()){
    				
    				it_impl.next();
    			}
    		}
    		
    		_it_impl = it_impl;
    	}
    	
    	public boolean 
    	hasNext()
    	{
    		Object	it_impl = _it_impl;
    		
    		if ( it_impl instanceof Iterator ){
    			
    			return(((Iterator)it_impl).hasNext());
    			
    		}else{
    			
    			Object	impl = _impl;
    			
    			if ( impl instanceof Map ){
    				
    				itModified();
    				
    				return(((Iterator)it_impl).hasNext());
    				
    			}else{
    				
    				return(((Object[][])impl).length >= pos + 1 );
    			}
    		}
    	}

    	public Object
    	next()
    	{
    		Object	it_impl = _it_impl;
    		
    		if ( it_impl instanceof Iterator ){
    			
    			return(((Iterator)it_impl).next());
    			
    		}else{
    			
    			Object	impl = _impl;
    			
    			if ( impl instanceof Map ){
    				
    				itModified();
    				
    				return(((Iterator)it_impl).next());
    				
    			}else{
    				
    				return(((Object[][])impl)[pos++][0]);
    			}
    		}
    	}

    	public void 
    	remove()
    	{
    		itModified();
    		
    		((Iterator)_it_impl).remove();
    	}
    }
}
