package org.cdlib.xtf.util;

import net.sf.saxon.instruct.Instruction;
import net.sf.saxon.type.ValidationException;
import org.xml.sax.SAXException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMLocator;

/**
 * Replacement error listener that redirects Saxon error messages to the
 * standard XTF Trace facility.
 * 
 * @author Martin Haye
 */
public class XTFSaxonErrorListener implements ErrorListener {
    
    /**
     * Receive notification of a warning.
     *
     * <p>Transformers can use this method to report conditions that
     * are not errors or fatal errors.  The default behaviour is to
     * take no action.</p>
     *
     * <p>After invoking this method, the Transformer must continue with
     * the transformation. It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  transformer exception.
     *
     * @throws javax.xml.transform.TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see javax.xml.transform.TransformerException
     */

    public void warning(TransformerException exception)
        throws TransformerException {

        String message = "";
        if (exception.getLocator()!=null) {
            message = getLocationMessage(exception) + "\n  ";
        }
        message += wordWrap(getExpandedMessage(exception));

        Trace.warning(message);
    }

    /**
     * Receive notification of a recoverable error.
     *
     * <p>The transformer must continue to provide normal parsing events
     * after invoking this method.  It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * <p>The action of the standard error listener depends on the
     * recovery policy that has been set, which may be one of RECOVER_SILENTLY,
     * RECOVER_WITH_WARNING, or DO_NOT_RECOVER
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     *
     * @throws javax.xml.transform.TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see javax.xml.transform.TransformerException
     */

    public void error(TransformerException exception) throws TransformerException {
        String message = (exception instanceof ValidationException ?
                            "Validation error " :
                            "Error ") +
                         getLocationMessage(exception) +
                         "\n  " +
                         wordWrap(getExpandedMessage(exception));
        Trace.error(message);
    }

    /**
     * Receive notification of a non-recoverable error.
     *
     * <p>The application must assume that the transformation cannot
     * continue after the Transformer has invoked this method,
     * and should continue (if at all) only to collect
     * addition error messages. In fact, Transformers are free
     * to stop reporting events once this method has been invoked.</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     *
     * @throws javax.xml.transform.TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see javax.xml.transform.TransformerException
     */

    public void fatalError(TransformerException exception) throws TransformerException {
        error(exception);
        throw exception;
    }

    /**
    * Get a string identifying the location of an error.
    */

    public static String getLocationMessage(TransformerException err) {
        SourceLocator loc = err.getLocator();
        if (loc==null) {
            if (err.getException() instanceof TransformerException) {
                return getLocationMessage((TransformerException)err.getException());
            } else if (err.getCause() instanceof TransformerException) {
                return getLocationMessage((TransformerException)err.getCause());
            } else {
                return "";
            }
        } else {
            String locmessage = "";
            if (loc instanceof DOMLocator) {
                locmessage += "at " + ((DOMLocator)loc).getOriginatingNode().getNodeName() + " ";
            } else if (loc instanceof Instruction) {
                locmessage += "at " + ((Instruction)loc).getInstructionName() + " ";
                //String elname = (String)((Instruction)loc).getProperty("name");
                //if (elname != null) {
                //    locmessage += elname + " ";
                //}
            }
            locmessage += "on line " + loc.getLineNumber() + " ";
            if (loc.getColumnNumber() != -1) {
                locmessage += "column " + loc.getColumnNumber() + " ";
            }
            if (loc.getSystemId() != null) {
                locmessage += "of " + loc.getSystemId() + ":";
            }
            return locmessage;
        }
    }

    /**
    * Get a string containing the message for this exception and all contained exceptions
    */

    public static String getExpandedMessage(TransformerException err) {
        String message = "";
        Throwable e = err;
        while (true) {
            if (e == null) {
                break;
            }
            String next = e.getMessage();
            if (next==null) next="";
            if (!next.equals("TRaX Transform Exception") && !message.endsWith(next)) {
                if (!message.equals("")) {
                    message += ": ";
                }
                message += e.getMessage();
            }
            if (e instanceof TransformerException) {
                e = ((TransformerException)e).getException();
            } else if (e instanceof SAXException) {
                e = ((SAXException)e).getException();
            } else {
                // e.printStackTrace();
                break;
            }
        }

        return message;
    }

    /**
     * Wordwrap an error message into lines of 72 characters or less (if possible)
     */

    private static String wordWrap(String message) {
        int nl = message.indexOf('\n');
        if (nl < 0) {
            nl = message.length();
        }
        if (nl > 100) {
            int i = 90;
            while (message.charAt(i) != ' ' && i>0) {
                i--;
            }
            if (i>10) {
                return message.substring(0, i) + "\n  " + wordWrap(message.substring(i+1));
            } else {
                return message;
            }
        } else {
            return message;
        }
    }

}

// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
