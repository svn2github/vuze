/**
 * Copyright (c) 2002 Regents of the University of California.  All
 * rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 * 3. Neither the name of the University nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.gudy.azureus2.ui.web2.stages.net;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkException;
import seda.sandStorm.api.SinkIF;
import java.net.InetAddress;

/**
 * Allows a SandStorm stage to do DNS lookups and not block.  Enqueue a
 * LookupReq and wait for a LookupResp.  The address field of the response
 * will be null in case of failure.
 *
 * @author Sean C. Rhea
 * @version $Id: ADns.java,v 1.1 2003-11-30 23:49:59 belgabor Exp $
 */
public class ADns implements seda.sandStorm.api.EventHandlerIF {

    public static class LookupReq implements QueueElementIF {
	public String hostname;
	public Object user_data;
	public SinkIF return_address;
	public LookupReq (String h, Object u, SinkIF r) { 
	    hostname = h;  
	    user_data = u; 
	    return_address = r;
	}
    }

    public static class LookupResp implements QueueElementIF {
	public String hostname;
	public Object user_data;
	public InetAddress address;
	public LookupResp (LookupReq req, InetAddress a) {
	    hostname = req.hostname;
	    user_data = req.user_data;
	    address = a;
	}
    }
    
    public void init(ConfigDataIF config) throws Exception {}

    public void destroy() {}

    public void handleEvents(QueueElementIF element_array[]) 
    throws EventHandlerException {
	for (int i = 0; i < element_array.length; ++i)
	    handleEvent(element_array[i]);
    }

    public void handleEvent(QueueElementIF item) 
    throws EventHandlerException {
	if (item instanceof LookupReq) {
	    LookupReq req = (LookupReq) item;
	    InetAddress address = null;

	    try {
		address = InetAddress.getByName (req.hostname);
	    }
	    catch (java.net.UnknownHostException e) {
		// do nothing
	    }

	    LookupResp resp = new LookupResp (req, address);
	    try {
		req.return_address.enqueue (resp);
	    }
	    catch (SinkException e) {
		System.err.println ("ERROR: couldn't enqueue " + resp +
			"onto " + req.return_address + ".  Dropping it.");
	    }
	}
	else {
	    System.err.println ("unknown event: " + item);
	    System.exit (1);
	}
    }
}

