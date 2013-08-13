/**
 * Created on Jun 4, 2010
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.vuze.tests;

import java.lang.reflect.Method;

import org.gudy.azureus2.core3.util.AERunnable;

/**
 * @author TuxPaper
 * @created Jun 4, 2010
 *
 * My Results:

10000000 calls:
direct took 9ms
reflect with cache took 210ms
basic reflect took 2693ms
AERunnable took 127ms

 */
public class RunnableVsReflect
{
	private static final int COUNT = 10000000;
	private int x;

	public RunnableVsReflect() throws SecurityException, NoSuchMethodException {
		long start;
		long diff;

		System.out.println(COUNT + " calls:");
		Class<? extends RunnableVsReflect> cla = this.getClass();
		Method meth = cla.getMethod("foo");

		
		start = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++) {
			try {
				foo();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		diff = System.currentTimeMillis() - start;
		System.out.println("direct took " + diff + "ms" );

		start = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++) {
			try {
				//this.getClass().getMethod("foo").invoke(this);
				//cla.getMethod("foo").invoke(this);
				meth.invoke(this);
				//reflectTo("foo", this);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		diff = System.currentTimeMillis() - start;
		System.out.println("reflect with cache took " + diff + "ms" );

		start = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++) {
			try {
				reflectTo("foo", this);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		diff = System.currentTimeMillis() - start;
		System.out.println("basic reflect took " + diff + "ms" );
		
		start = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++) {
			try {
				AERunnable runnable = new AERunnable() {
					public void runSupport() {
						foo();
					}
				};
				runnable.run();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		diff = System.currentTimeMillis() - start;
		System.out.println("AERunnable took " + diff + "ms" );

		start = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++) {
			try {
				Runnable runnable = new Runnable() {
					public void run() {
						foo();
					}
				};
				runnable.run();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		diff = System.currentTimeMillis() - start;
		System.out.println("Runnable took " + diff + "ms" );
	}
	
	public void reflectTo(String name, Object o) throws Throwable {
		o.getClass().getMethod(name).invoke(o);
	}
	
	public void foo() {
		// do something
		x++;
	}
	
	public static void main(String[] args) {
		try {
			RunnableVsReflect runnableVsReflect = new RunnableVsReflect();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
