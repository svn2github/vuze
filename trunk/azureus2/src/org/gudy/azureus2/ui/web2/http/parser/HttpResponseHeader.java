/*
 * Copyright (c) 2001 Regents of the University of California.  All
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

package org.gudy.azureus2.ui.web2.http.parser;

import org.gudy.azureus2.ui.web2.http.util.*;
import org.gudy.azureus2.ui.web2.util.AssertionViolatedException;

import seda.sandStorm.lib.aSocket.ATcpConnection;

public class HttpResponseHeader extends HttpHeader {

    public HttpResponseHeader (ATcpConnection c, 
	    HttpString fw, HttpString sw, HttpString tw) {
	super (c, fw, sw, tw);
	if (fw.equals (HTTP11)) {
	    _major = 1; _minor = 1;
	}
	else if (fw.equals (HTTP10)) {
	    _major = 1; _minor = 0;
	}
	else 
	    throw new AssertionViolatedException ("not 1.1 or 1.0, but " + fw);
    }

    public void set_http_version (int major, int minor) {
	if ((major == 1) && (minor == 1)) {
	    _major = 1; _minor = 1;
	    fw = HTTP11;
	}
	else if ((major == 1) && (minor == 0)) {
	    _major = 1; _minor = 0;
	    fw = HTTP10;
	}
	else 
	    throw new AssertionViolatedException ("not 1.1 or 1.0");
    }

    public int response_code () {
	return sw.parse_int ();
    }
 
    private int _major, _minor;
    public int major_version () {
	return _major;
    }

    public int minor_version () {
	return _minor;
    }
}

