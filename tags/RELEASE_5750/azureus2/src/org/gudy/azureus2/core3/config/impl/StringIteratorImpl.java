/*
 * Created on 24 sept. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.config.impl;

import java.util.Iterator;

import org.gudy.azureus2.core3.config.StringIterator;

/**
 * @author Olivier Chalouhi
 *
 */
public class StringIteratorImpl implements StringIterator {

	final Iterator iterator;
	
	public StringIteratorImpl(Iterator _iterator) {
		iterator = _iterator;
	}
	
	public boolean hasNext() {
		return iterator.hasNext();
	}
	
	public String next() {
		return (String) iterator.next();
	}
	
	public void remove() {
		iterator.remove();		
	}
}
