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
 * Author: Matt Welsh <mdw@cs.berkeley.edu>
 * 
 */

package org.gudy.azureus2.ui.web2.stages.cache;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.UI;
import org.gudy.azureus2.ui.web2.WebConst;
import org.gudy.azureus2.ui.web2.stages.http.*;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpBadRequestResponse;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpConnection;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpNotFoundResponse;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpOKResponse;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpRedirectResponse;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpRequest;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpResponder;

import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkClosedEvent;
import seda.sandStorm.api.SinkException;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.api.StageIF;
import seda.sandStorm.core.BufferElement;
import seda.sandStorm.core.ssLinkedList;
import seda.sandStorm.lib.aDisk.AFile;
import seda.sandStorm.lib.aDisk.AFileIOCompleted;
import seda.sandStorm.lib.aDisk.AFileStat;
import seda.sandStorm.lib.aDisk.FileIsDirectoryException;

// For JDK1.1 Collections package
//import com.sun.java.util.collections.*;

/**
 * This version of PageCache maintains a list of cacheEntries for each
 * page size, and attempts to reuse old entries of the same size on reject.
 * This is the best implementation of the Haboob web page cache.
 */
public class PageCacheSized implements EventHandlerIF, WebConst {

	private static final Logger logger =
		Logger.getLogger("azureus2.ui.web.stages.PageCacheSized");
	private static final boolean PROFILE = false;

	// Don't actually read file; just store empty buffer in cache
	private static final boolean DEBUG_NO_FILE_READ = false;
	// Don't even stat file; just allocate buffer of fixed size
	private static final boolean DEBUG_NO_FILE_READ_SAMESIZE = false;
	private static final int DEBUG_NO_FILE_READ_SAMESIZE_SIZE = 8192;
	// Don't read file through aFile interface - just do it directly
	private static final boolean DEBUG_DIRECT_FILE = false;

	// Rewrite incoming filename so all cache entries hit
	private static final boolean DEBUG_SINGLE_CACHE_PAGE = false;
	// If true, rewrite all request URLs to DEBUG_SINGLE_CACHE_PAGE_FNAME
	// If false, all cache misses access same file, but different entries
	private static final boolean DEBUG_SINGLE_CACHE_PAGE_SAMENAME = false;
	// This file is of size 8192 bytes
	private static final String DEBUG_SINGLE_CACHE_PAGE_FNAME =
		"/dir00000/class1_7";

	// Whether to prioritize cache hits over misses
	private static final boolean PRIORITIZE_HITS = true;
	private myComparator myComp;

	// Whether to handle misses in separate stage
	private static final boolean SEPARATE_MISS_STAGE = true;
	private SinkIF missStageSink;
	private boolean missStage;

	private String DEFAULT_URL;
	private String ROOT_DIR;

	private SinkIF mysink, sendSink;
	private Hashtable pageTbl; // Map URL -> cacheEntry
	private Hashtable sizeTbl; // Map size -> linked list of free cacheEntries
	private Hashtable aFileTbl; // Map aFile -> cacheEntry
	private int maxCacheSize;
	private Random rand;

	private Hashtable mimeTbl; // Filename extension -> MIME type
	private static final String defaultMimeType = "text/plain";

	// Used to initialize hit stage
	public PageCacheSized() {
		missStage = false;
	}

	// Used to initialize miss stage
	PageCacheSized(PageCacheSized hitStage) {
		missStage = true;
		pageTbl = hitStage.pageTbl;
		sizeTbl = hitStage.sizeTbl;
		aFileTbl = hitStage.aFileTbl;
		mimeTbl = hitStage.mimeTbl;
		rand = new Random();
		DEFAULT_URL = hitStage.DEFAULT_URL;
		ROOT_DIR = hitStage.ROOT_DIR;
		maxCacheSize = hitStage.maxCacheSize;
		if (PRIORITIZE_HITS) {
			myComp = hitStage.myComp;
		}
	}

	public void init(ConfigDataIF config) throws Exception {
		mysink = config.getStage().getSink();
		logger.info(
			"PageCacheSized: missStage=" + missStage + ", mysink=" + mysink);
		sendSink = config.getManager().getStage(HTTP_SEND_STAGE).getSink();

		if (!missStage) {
			pageTbl = new Hashtable();
			sizeTbl = new Hashtable();
			aFileTbl = new Hashtable();
			rand = new Random();

			mimeTbl = new Hashtable();
			mimeTbl.put(".html", "text/html");
			mimeTbl.put(".gif", "image/gif");
			mimeTbl.put(".jpg", "image/jpeg");
			mimeTbl.put(".jpeg", "image/jpeg");
            mimeTbl.put(".png", "image/png");
            mimeTbl.put(".ico", "image/ico");
            mimeTbl.put(".css", "text/css");
            mimeTbl.put(".xml", "text/xml");
            mimeTbl.put(".ps", "application/postscript");
            mimeTbl.put(".eps", "application/postscript");
			mimeTbl.put(".pdf", "application/pdf");

			DEFAULT_URL = config.getString("defaultURL");
			if (DEFAULT_URL == null)
				throw new IllegalArgumentException("Must specify defaultURL");
			ROOT_DIR = config.getString("rootDir");
			if (ROOT_DIR == null)
				throw new IllegalArgumentException("Must specify rootDir");
			maxCacheSize = config.getInt("maxCacheSize");

			if (PRIORITIZE_HITS) {
				myComp = new myComparator();
			}

			if (SEPARATE_MISS_STAGE) {
				StageIF missStage =
					config.getManager().createStage(
						"PageCacheSized missStage",
						new PageCacheSized(this),
						null);
				missStageSink = missStage.getSink();
			}
		}
	}

	public void destroy() {
	}

	public void handleEvent(QueueElementIF item) {
		if (logger.isDebugEnabled())
			logger.debug(
				"PageCacheSized (missStage=" + missStage + "): GOT QEL: " + item);

		if (item instanceof httpRequest) {
			UI.numRequests++;

			httpRequest req = (httpRequest) item;
			if (req.getRequest() != httpRequest.REQUEST_GET) {
				UI.numErrors++;
				HttpSend.sendResponse(
					new httpResponder(
						new httpBadRequestResponse(
							req,
							"Only GET requests supported at this time"),
						req,
						true));
				return;
			}

			String url;
			if (DEBUG_SINGLE_CACHE_PAGE && DEBUG_SINGLE_CACHE_PAGE_SAMENAME) {
				url = DEBUG_SINGLE_CACHE_PAGE_FNAME;
			} else {
				url = req.getURL();
			}

			cacheEntry entry;
			synchronized (pageTbl) {

				if (logger.isDebugEnabled())
					logger.debug("PageCacheSized: Checking cache for URL " + url);
				long t1 = 0, t2;
				if (PROFILE)
					t1 = System.currentTimeMillis();
				entry = (cacheEntry) pageTbl.get(url);
				if (PROFILE) {
					t2 = System.currentTimeMillis();
					UI.numCacheLookup++;
					UI.timeCacheLookup += (t2 - t1);
				}

				if (entry == null) {
					// Got a cache miss
					handleCacheMiss(req);
					return;
				}
			}

			if (logger.isDebugEnabled())
				logger.debug("PageCacheSized: Got entry " + entry);
			UI.numCacheHits++;
			synchronized (entry) {
				if (entry.pending) {
					// Entry still pending - wait for it
					if (logger.isDebugEnabled())
						logger.debug("PageCacheSized: Entry still pending");
					entry.addWaiter(req);
				} else {
					// Got a hit - send it
					if (logger.isDebugEnabled())
						logger.debug("PageCacheSized: Sending entry");
					entry.send(req);
				}
			}

		} else if (item instanceof AFileIOCompleted) {

			AFileIOCompleted comp = (AFileIOCompleted) item;
			AFile af = comp.getFile();
			if (logger.isDebugEnabled())
				logger.debug("PageCacheSized: Got AIOComp for " + af.getFilename());

			cacheEntry entry = (cacheEntry) aFileTbl.get(af);
			if (entry == null) {
				logger.info(
					"PageCacheSized: WARNING: Got AFileIOCompleted for non-entry: "
						+ comp);
				return;
			}
			entry.done(comp);

		} else if (item instanceof SinkClosedEvent) {
			SinkClosedEvent sce = (SinkClosedEvent) item;
			if (sce.sink instanceof httpConnection) {
				// Pass on to sendSink if not a file close event
				sendSink.enqueue_lossy(sce);
			}

		} else {
			logger.info("PageCacheSized: Got unknown event type: " + item);
		}
	}

	class myComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			if ((o1 instanceof httpRequest) && (o2 instanceof httpRequest)) {
				httpRequest req1 = (httpRequest) o1;
				httpRequest req2 = (httpRequest) o2;
				int req1sz = isHit(req1);
				int req2sz = isHit(req2);
				if ((req1sz != -1) && (req2sz != -1)) {
					if (req1sz < req2sz)
						return -1;
					else if (req1sz > req2sz)
						return 1;
					else
						return 0;
				} else if ((req1sz == -1) && (req2sz != -1)) {
					return 1;
				} else if ((req1sz != -1) && (req2sz == -1)) {
					return -1;
				} else {
					return 0;
				}
			} else if (
				(o1 instanceof httpRequest) && (!(o2 instanceof httpRequest))) {
				return -1;
			} else if (
				!(o1 instanceof httpRequest) && ((o2 instanceof httpRequest))) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	public void handleEvents(QueueElementIF items[]) {
		if (PRIORITIZE_HITS) {
			// Sort entries first
			Arrays.sort(items, myComp);
		}

		for (int i = 0; i < items.length; i++) {
			handleEvent(items[i]);
		}
	}

	private int isHit(httpRequest req) {
		cacheEntry entry = (cacheEntry) pageTbl.get(req.getURL());
		if ((entry != null) && (!entry.pending))
			return entry.size;
		else
			return -1;
	}

	private void handleCacheMiss(httpRequest req) {
		String url;
		String fname;
		long t1 = 0, t2;

		if (SEPARATE_MISS_STAGE && !missStage) {
			if (!missStageSink.enqueue_lossy(req)) {
				logger.info(
					"PageCacheSized: WARNING: Could not enqueue "
						+ req
						+ " to missStageSink");
			}
			return;
		}

		if (logger.isDebugEnabled())
			logger.debug("PageCacheSized: Handling cache miss for " + req);
		UI.numCacheMisses++;

		if (DEBUG_SINGLE_CACHE_PAGE) {
			if (DEBUG_SINGLE_CACHE_PAGE_SAMENAME) {
				// Rewrite url
				url = DEBUG_SINGLE_CACHE_PAGE_FNAME;
				fname = ROOT_DIR + url;
			} else {
				// Rewrite fname, not url
				url = req.getURL();
				fname = ROOT_DIR + DEBUG_SINGLE_CACHE_PAGE_FNAME;
			}
		} else {
			url = req.getURL();
			fname = ROOT_DIR + url;
		}

		AFile af = null;
		AFileStat stat = null;
		cacheEntry entry;

		if (DEBUG_NO_FILE_READ && DEBUG_NO_FILE_READ_SAMESIZE) {
			// Create bogus entry
			if (logger.isDebugEnabled())
				logger.debug("PageCacheSized: Creating bogus cacheEntry");
			entry = getEntry(req, null, DEBUG_NO_FILE_READ_SAMESIZE_SIZE);

		} else if (DEBUG_DIRECT_FILE) {

			// Don't use AFile - just read file directly
			try {
				File f = new File(fname);
				RandomAccessFile raf = new RandomAccessFile(f, "r");
				if (logger.isDebugEnabled())
					logger.debug("PageCacheSized: Got file size " + f.length());
				entry = getEntry(req, null, (int) f.length());
				if (logger.isDebugEnabled())
					logger.debug(
						"PageCacheSized: Reading file directly, length "
							+ f.length()
							+ ", entrysize "
							+ entry.response.getPayload().getBytes().length);
				BufferElement payload = entry.response.getPayload();
				raf.readFully(payload.getBytes(), payload.offset, payload.size);
				raf.close();
				entry.pending = false;
				httpResponder respd = new httpResponder(entry.response, req);
				HttpSend.sendResponse(respd);
				return;

			} catch (IOException ioe) {
				// File not found
				logger.info(
					"PageCacheSized: Could not open file " + fname + ": " + ioe);
				ioe.printStackTrace();
				UI.numErrors++;
				httpNotFoundResponse notfound =
					new httpNotFoundResponse(req, ioe.getMessage());
				HttpSend.sendResponse(new httpResponder(notfound, req, true));
				return;
			}

		} else {

			while (true) {
				// Open file and stat it to determine size
				try {
					af = new AFile(fname, mysink, false, true);
					stat = af.stat();
					break;

				} catch (FileIsDirectoryException fde) {
					// Tried to open a directory

					if (url.endsWith("/")) {
						// Replace file with DEFAULT_URL and try again
						if (fname.endsWith("/")) {
							fname = fname + DEFAULT_URL;
						} else {
							fname = fname + "/" + DEFAULT_URL;
						}
						continue;
					} else {
						// Redirect to url+"/" (so that img src works in the document)
						String newURL = url + "/";
						httpRedirectResponse redirect =
							new httpRedirectResponse(req, newURL);
						HttpSend.sendResponse(new httpResponder(redirect, req, true));
						return;
					}

				} catch (IOException ioe) {
					// File not found
					logger.info(
						"PageCacheSized: Could not open file " + fname + ": " + ioe);
					UI.numErrors++;
					httpNotFoundResponse notfound =
						new httpNotFoundResponse(req, ioe.getMessage());
					HttpSend.sendResponse(new httpResponder(notfound, req, true));
					return;
				}
			}

			// Allocate entry
			if (logger.isDebugEnabled())
				logger.debug("PageCacheSized: Got file size " + stat.length);
			entry = getEntry(req, af, (int) stat.length);
		}

		if (DEBUG_NO_FILE_READ) {
			// Pretend we got it already
			entry.done(null);
		} else {
			// Issue read 
			entry.doRead();
		}
	}

	private String getMimeType(String url) {
		Enumeration e = mimeTbl.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			if (url.endsWith(key))
				return (String) mimeTbl.get(key);
		}
		return defaultMimeType;
	}

	// Obtain a new cache entry (either allocating a new entry or 
	// reusing an old one)
	private cacheEntry getEntry(httpRequest req, AFile af, int size) {
		cacheEntry entry = null;

		if (logger.isDebugEnabled())
			logger.debug("PageCacheSized: Finding entry of size " + size);

		if ((maxCacheSize != -1)
			&& (UI.cacheSizeBytes + size > maxCacheSize * 1024)) {
			// Cache is full, try to reuse entry
			if (logger.isDebugEnabled())
				logger.debug(
					"PageCacheSized: Cache is full (size "
						+ (UI.cacheSizeBytes / 1024)
						+ " Kb)");
			Integer isz = new Integer(size);
			ssLinkedList ll = (ssLinkedList) sizeTbl.get(isz);
			if ((ll == null) || (ll.size() == 0)) {
				// No entries available, allocate new
				if (logger.isDebugEnabled())
					logger.debug("PageCacheSized: No entry of this size, allocating");
				return new cacheEntry(req, af, size);
			} else {
				// Reuse entry
				if (logger.isDebugEnabled())
					logger.debug(
						"PageCacheSized: Sizelist has " + ll.size() + " elements");
				boolean found = false;
				int count = 0;
				while (count < ll.size()) {
					entry = (cacheEntry) ll.remove_head();
					if (entry.pending) {
						ll.add_to_tail(entry);
						count++;
					} else {
						if (logger.isDebugEnabled())
							logger.debug("PageCacheSized: Reusing entry " + entry);
						found = true;
						break;
					}
				}
				if (!found) {
					// All entries pending, allocate anyway
					if (logger.isDebugEnabled())
						logger.debug("PageCacheSized: All entries pending, allocating new");
					return new cacheEntry(req, af, size);
				}

				// Place back on list and reuse
				ll.add_to_tail(entry);
				entry.reuse(req, af);
				return entry;
			}

		} else {
			if (logger.isDebugEnabled())
				logger.debug(
					"PageCacheSized: Cache not full (size "
						+ (UI.cacheSizeBytes / 1024)
						+ " Kb), allocating");
			// Cache not full, go ahead and allocate
			return new cacheEntry(req, af, size);
		}

	}

	private class cacheEntry {
		httpOKResponse response;
		boolean pending;
		int size;
		AFile af;
		ssLinkedList waiting, sizeList;
		String url;
		long tStartRead, tEndRead;

		// Allocate a new cache entry
		private cacheEntry(httpRequest req, AFile af, int size) {
			if (logger.isDebugEnabled())
				logger.debug(
					"PageCacheSized: Allocating new cache entry for "
						+ af.getFilename()
						+ ", size="
						+ size);

			if (af == null) {
				this.response = new httpOKResponse("text/plain", size);
			} else {
				this.response = new httpOKResponse(getMimeType(af.getFilename()), size);
			}
			this.size = size;
			this.url = req.getURL();
			this.af = af;
			pending = true;
			waiting = new ssLinkedList();
			addWaiter(req);

			// Add to pageTbl
			pageTbl.put(url, this);
			// Add to aFileTbl
			if (af != null) {
				aFileTbl.put(af, this);
			}
			// Add to sizeTbl
			Integer isz = new Integer(size);
			ssLinkedList ll = (ssLinkedList) sizeTbl.get(isz);
			if (ll == null) {
				ll = new ssLinkedList();
				sizeTbl.put(isz, ll);
			}
			ll.add_to_tail(this);
			this.sizeList = ll;
			UI.cacheSizeEntries++;
			UI.cacheSizeBytes += size;
		}

		// Reuse a cache entry
		/* FIXME: Avoid reuse of cache entry that is currently being
		 * written out to another socket? (Maintain 'write count' which
		 * is incremented for each send, decremented for each SinkDreainedEvent,
		 * and SinkClosedEvent (when the associated SinkDrainedEvent did
		 * not arrive yet due to the conn being closed first).
		 */
		private synchronized void reuse(httpRequest req, AFile af) {
			if (logger.isDebugEnabled())
				logger.debug(
					"PageCacheSized: entry "
						+ this
						+ " being reused for "
						+ af.getFilename());
			if (this.af != null) {
				aFileTbl.remove(this.af);
				this.af.close();
			}
			this.af = af;
			if (af != null) {
				aFileTbl.put(af, this);
			}
			synchronized (pageTbl) {
				pageTbl.remove(url);
				this.url = req.getURL();
				pageTbl.put(url, this);
			}
			pending = true;
			waiting.remove_all();
			addWaiter(req);
		}

		// Initiate file read
		void doRead() {
			if (logger.isDebugEnabled())
				logger.debug("PageCacheSized: Initiating read on entry " + this);
			if (af == null)
				return;
			try {
				if (PROFILE)
					tStartRead = System.currentTimeMillis();
				af.read(response.getPayload());
			} catch (SinkException se) {
				logger.info(
					"PageCacheSized: Got SinkException attempting read on "
						+ af
						+ ": "
						+ se);
				UI.numErrors++;
				httpRequest waiter;
				while ((waiter = (httpRequest) waiting.remove_head()) != null) {
					httpNotFoundResponse notfound =
						new httpNotFoundResponse(waiter, se.getMessage());
					httpResponder respd = new httpResponder(notfound, waiter, true);
					HttpSend.sendResponse(respd);
				}
				free();
			}
		}

		// Free cache entry and remove from system for GC
		void free() {
			logger.info("PageCacheSized: Freeing entry " + this);
			if (af != null) {
				aFileTbl.remove(af);
				af.close();
				af = null;
			}
			pageTbl.remove(url);
			sizeList.remove_item(this);
			response = null;
		}

		synchronized void addWaiter(httpRequest req) {
			waiting.add_to_tail(req);
		}

		httpOKResponse getResponse() {
			return response;
		}

		// Send response to all waiters when done reading
		synchronized void done(AFileIOCompleted comp) {
			if (logger.isDebugEnabled())
				logger.debug("PageCacheSized: Done with file read on " + this);

			if ((comp != null) && (comp.sizeCompleted != size)) {
				throw new RuntimeException(
					"PageCacheSized: WARNING: Got "
						+ comp.sizeCompleted
						+ " bytes read, expecting "
						+ size);
			}

			if (af != null) {
				af.close();
				aFileTbl.remove(af);
				af = null;
			}
			if (PROFILE) {
				tEndRead = System.currentTimeMillis();
				UI.numFileRead++;
				UI.timeFileRead += (tEndRead - tStartRead);
			}

			pending = false;
			httpRequest waiter;

			while ((waiter = (httpRequest) waiting.remove_head()) != null) {
				httpResponder respd = new httpResponder(response, waiter);
				HttpSend.sendResponse(respd);
			}
		}

		// Send cache entry on hit
		void send(httpRequest req) {
			httpResponder respd = new httpResponder(response, req);
			HttpSend.sendResponse(respd);
		}

		public String toString() {
			if (af != null) {
				return "cacheEntry [af=" + af.getFilename() + ", size=" + size + "]";
			} else {
				return "cacheEntry [size=" + size + "]";
			}
		}
	}

	// An experiment: Try running cache misses in a separate stage
	class CacheMissStage implements EventHandlerIF {
		SinkIF mysink;

		public void init(ConfigDataIF config) throws Exception {
			logger.info("CacheMissStage: mysink " + mysink);
			mysink = config.getStage().getSink();
		}

		public void destroy() throws Exception {
		}

		public void handleEvent(QueueElementIF event) {
			// Actually run the enclosing class method, since
			// a cache entry may be loaded by an earlier request 
			// in the pipeline.
			logger.info("CacheMissStage: handling " + event);
			PageCacheSized.this.handleEvent(event);
		}

		public void handleEvents(QueueElementIF items[]) {
			for (int i = 0; i < items.length; i++) {
				handleEvent(items[i]);
			}
		}

	}

}
