/* 
 * Copyright (c) 2001 by Matt Welsh and The Regents of the University of 
 * California. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice and the following
 * two paragraphs appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Authors: Eric Wagner <eric@xcf.berkeley.edu> 
 *          Matt Welsh <mdw@cs.berkeley.edu> 
 */

package org.gudy.azureus2.ui.web2.stages.hdapi;

import seda.sandStorm.api.*;
import seda.sandStorm.core.*;
import seda.sandStorm.lib.http.*;
import seda.sandStorm.lib.aDisk.*;
import java.io.*;
import java.util.*;

import org.gudy.azureus2.ui.web2.UI;
import org.gudy.azureus2.ui.web2.WebConst;
import org.gudy.azureus2.ui.web2.stages.http.*;
import org.gudy.azureus2.ui.web2.util.WildcardDictionary;

public class WildcardDynamicHttp implements EventHandlerIF, WebConst {

	private static final boolean DEBUG = false;

	// Whether to close the HTTP connection after each request
	private static final boolean CLOSE_CONNECTION = false;

	// Whether each URL should be handled by its own stage
	private static final boolean SEPARATE_STAGES = true;

	private SinkIF mysink;
	private static ConfigDataIF config;
	private static SinkIF mainsink;
	private static Hashtable handlerCache, stageCache;
	private static WildcardDictionary dynPages;
	private String myurl;

	public WildcardDynamicHttp() {
		myurl = null;
	}

	private WildcardDynamicHttp(String myurl) {
		this.myurl = myurl;
	}

	public void init(ConfigDataIF config) throws Exception {
		WildcardDynamicHttp.config = config;
		mysink = config.getStage().getSink();

		if (myurl == null) {
			mainsink = mysink;

			/* Read HDAPI configuration file */
//			String conffname = config.getString("configfile");
//			if (conffname == null)
//				throw new IllegalArgumentException("Must specify DynamicHttp.configfile");
//			AFile af = new AFile(conffname, mysink, false, true);
//			BufferElement configfile = new BufferElement((int) af.stat().length);

			dynPages = new WildcardDictionary();
            dynPages.put("*.tmpl", "org.gudy.azureus2.ui.web2.stages.hdapi.impl.TemplateHandler");
			handlerCache = new Hashtable();
			stageCache = new Hashtable();
//			af.read(configfile);

			System.err.println("DynamicHttp: Started");

		} else {
			System.err.println("DynamicHttp handlerStage [" + myurl + "]: Started");
		}
	}

	public void destroy() {
	}

	/**
	 * Handle the given request, passing it to the appropriate handler
	 * (and stage, if necessary). Returns true if the request can be 
	 * processed, or false if the request was not for a dynamic URL.
	 */
	public static boolean handleRequest(httpRequest req) throws Exception {
		UI.numRequests++;
		String url = req.getURL();
		if (dynPages.get(url) == null)
			return false;

		if (SEPARATE_STAGES) {
			SinkIF thesink;
			synchronized (stageCache) {
				thesink = (SinkIF) stageCache.get(url);
				if (thesink == null)
					thesink = makeStage(url);
			}
			thesink.enqueue(req);
		} else {
			mainsink.enqueue(req);
		}
		return true;
	}

	public void handleEvent(QueueElementIF item) {
		if (DEBUG) {
			if (myurl == null)
				System.err.println("DynamicHttp: GOT QEL: " + item);
			else
				System.err.println("DynamicHttp [" + myurl + "]: GOT QEL: " + item);
		}

		if (item instanceof httpRequest) {
			httpRequest req = (httpRequest) item;
			try {
				doRequest(req);
			} catch (Exception e) {
				HttpSend.sendResponse(
					new httpResponder(
						new httpInternalServerErrorResponse(
							req,
							"The following exception occurred:<p><pre>" + e + "</pre>"),
						req,
						true));
			}

		} else if (item instanceof AFileIOCompleted) {
			AFileIOCompleted comp = (AFileIOCompleted) item;
			process_config(comp);
			System.err.println("DynamicHttp: finished reading config file");

		} else {
			System.err.println("DynamicHttp: Don't know what to do with " + item);
		}
	}

	public void handleEvents(QueueElementIF items[]) {
		for (int i = 0; i < items.length; i++) {
			handleEvent(items[i]);
		}
	}

	private void doRequest(httpRequest req) {
		String url;
		String classname;

		url = req.getURL();
		classname = (String) dynPages.get(url);

		// No class registered for this URL -- shouldn't happen as we are
		// screened by handleRequest()
		if (classname == null) {
			HttpSend.sendResponse(
				new httpResponder(
					new httpInternalServerErrorResponse(
						req,
						"Got dynamic URL with no class -- this is a bug, please contact mdw@cs.berkeley.edu"),
					req,
					true));
			System.err.println(
				"DynamicHttp: Warning: Got dynamic URL with no class: " + url);
			return;
		}

		try {
			handlerPool pool;
			synchronized (this) {
				pool = (handlerPool) handlerCache.get(classname);
				if (pool == null) {
					try {
						pool = new handlerPool(classname);
						System.err.println(
							"DynamicHttp: Loaded class " + classname + " for url " + url);
					} catch (ClassNotFoundException cnfe) {
						HttpSend.sendResponse(
							new httpResponder(
								new httpInternalServerErrorResponse(req, cnfe.toString()),
								req,
								true));
						return;
					}
					handlerCache.put(classname, pool);
				}
			}

			httpRequestHandlerIF handler = pool.getHandler();
			httpResponse resp;
			resp = handler.handleRequest(req);

			httpResponder respd = new httpResponder(resp, req, CLOSE_CONNECTION);
			HttpSend.sendResponse(respd);
			pool.doneWithHandler(handler);
		} catch (Exception e) {
			HttpSend.sendResponse(
				new httpResponder(
					new httpInternalServerErrorResponse(req, e.toString()),
					req,
					true));
			return;
		}
	}

	class handlerPool {
		Class theclass;
		Vector pool;

		handlerPool(String classname) throws ClassNotFoundException {
			this.theclass = Class.forName(classname);
			this.pool = new Vector();
		}

		httpRequestHandlerIF getHandler()
			throws InstantiationException, IllegalAccessException {
			httpRequestHandlerIF handler;
			synchronized (pool) {
				if (pool.size() == 0) {
					handler = (httpRequestHandlerIF) theclass.newInstance();
				} else {
					handler = (httpRequestHandlerIF) pool.remove(pool.size() - 1);
				}
			}
			return handler;
		}

		void doneWithHandler(httpRequestHandlerIF handler) {
			synchronized (pool) {
				pool.addElement(handler);
			}
		}
	}

	private static SinkIF makeStage(String url) throws Exception {
		StageIF thestage;
		thestage =
			config.getManager().createStage(
				"DynamicHttp [" + url + "]",
				new WildcardDynamicHttp(url),
				null);
		stageCache.put(url, thestage.getSink());
		return thestage.getSink();
	}

	private void addURL(String url, String classname) {
		System.err.println(
			"DynamicHttp: Adding URL [" + url + "] class [" + classname + "]");
		dynPages.put(url, classname);
	}

	private void process_config(AFileIOCompleted comp) {
		BufferElement conf_buf = ((AFileReadRequest) comp.getRequest()).getBuffer();

		String s = new String(conf_buf.data);
		BufferedReader buf_reader = new BufferedReader(new StringReader(s));
		String tmp;
		try {
			while ((tmp = buf_reader.readLine()) != null) {
				if (tmp.startsWith("#"))
					continue; // Skip comment lines
				StringTokenizer st = new StringTokenizer(tmp);
				String url, class_name;
				if (st.hasMoreElements()) {
					url = st.nextToken();
				} else {
					// Ignore empty lines
					continue;
				}
				if (st.hasMoreElements()) {
					class_name = st.nextToken();
				} else {
					System.out.println(
						"DynamicHttp: Bad line format in configuration file: " + tmp);
					continue;
				}

				addURL(url, class_name);
			}
		} catch (IOException ioe) {
			System.err.println(
				"DynamicHttp: IOException processing configuration file:" + ioe);
		}
	}
}
