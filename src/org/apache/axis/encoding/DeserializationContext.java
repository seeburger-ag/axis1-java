package org.apache.axis.encoding;

/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.*;
import java.util.*;
import javax.xml.parsers.SAXParser;
import org.apache.axis.*;
import org.apache.axis.message.*;
import org.apache.axis.utils.Debug;
import org.apache.axis.utils.NSStack;
import org.apache.axis.utils.QName;
import org.apache.axis.utils.XMLUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * @author Glen Daniels (gdaniels@macromedia.com)
 */

public class DeserializationContext extends DefaultHandler
{
    private static final boolean DEBUG_LOG = false;
    
    static class LocalIDResolver implements IDResolver
    {
        HashMap idMap = null;
        
        public void addIDMapping(String id, Object referent)
        {
            if (idMap == null)
                idMap = new HashMap();
            
            idMap.put(id, referent);
        }
        
        public Object getReferencedObject(String href)
        {
            if ((idMap == null) || (href == null))
                return null;
            return idMap.get(href);
        }
    }
    
    private NSStack namespaces = new NSStack();
    
    private Locator locator;
                                             
    Stack handlerStack = new Stack();
    
    SAX2EventRecorder recorder = new SAX2EventRecorder();
    public SOAPEnvelope envelope;
    
    /** A map of IDs -> IDResolvers
     */
    HashMap idMap;
    LocalIDResolver localIDs;
    
    HashMap fixups;
    
    static final SOAPHandler nullHandler = new SOAPHandler();
    
    protected MessageContext msgContext;
    
    protected HandlerFactory initialFactory;
    
    protected boolean doneParsing = false;
    protected InputSource inputSource = null;
        
    public DeserializationContext(MessageContext ctx, String messageType)
    {
        msgContext = ctx;
        
        envelope = new SOAPEnvelope();
        envelope.setRecorder(recorder);
        envelope.setMessageType(messageType);
        
        pushElementHandler(new EnvelopeHandler(new EnvelopeBuilder()));
    }
    
    public DeserializationContext(InputSource is, MessageContext ctx, 
                                  String messageType)
    {
        this(ctx, messageType);
        inputSource = is;
    }
    
    public void parse() throws SAXException
    {
        if (inputSource != null) {
            SAXParser parser = XMLUtils.getSAXParser();
            try {
                parser.parse(inputSource, this);
            } catch (IOException e) {
                throw new SAXException(e);
            }
            inputSource = null;
        }
    }
    
    public MessageContext getMessageContext()
    {
        return msgContext;
    }
    
    public SOAPEnvelope getEnvelope()
    {
        return envelope;
    }
    
    public SAX2EventRecorder getRecorder()
    {
        return recorder;
    }
    
    /** Grab a namespace prefix
     */
    public String getNamespaceURI(String prefix)
    {
        return namespaces.getNamespaceURI(prefix);
    }
    
    public QName getQNameFromString(String qNameStr)
    {
        if (qNameStr == null)
            return null;
        
        // OK, this is a QName, so look up the prefix in our current mappings.
        
        int i = qNameStr.indexOf(":");
        if (i == -1)
            return null;
        
        String nsURI = getNamespaceURI(qNameStr.substring(0, i));
        
        //System.out.println("namespace = " + nsURI);
        
        if (nsURI == null)
            return null;  // ???
        
        return new QName(nsURI, qNameStr.substring(i + 1));
    }
    
    public QName getTypeFromAttributes(String namespace, String localName,
                                       Attributes attrs)
    {
        QName typeQName = null;
        
        if (typeQName == null) {
            QName myQName = new QName(namespace, localName);
            if (myQName.equals(SOAPTypeMappingRegistry.SOAP_ARRAY)) {
                typeQName = SOAPTypeMappingRegistry.SOAP_ARRAY;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_STRING)) {
                typeQName = SOAPTypeMappingRegistry.XSD_STRING;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_BOOLEAN)) {
                typeQName = SOAPTypeMappingRegistry.XSD_BOOLEAN;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_DOUBLE)) {
                typeQName = SOAPTypeMappingRegistry.XSD_DOUBLE;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_FLOAT)) {
                typeQName = SOAPTypeMappingRegistry.XSD_FLOAT;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_INT)) {
                typeQName = SOAPTypeMappingRegistry.XSD_INT;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_LONG)) {
                typeQName = SOAPTypeMappingRegistry.XSD_LONG;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_SHORT)) {
                typeQName = SOAPTypeMappingRegistry.XSD_SHORT;
            } else if (myQName.equals(SOAPTypeMappingRegistry.SOAP_BYTE)) {
                typeQName = SOAPTypeMappingRegistry.XSD_BYTE;
            }
        }

        if (typeQName != null)
            return typeQName;
        
        if (attrs == null)
            return null;
        
        // Check for type
        String type = null;
        for (int i=0; i<Constants.URIS_SCHEMA_XSI.length && type==null; i++)
            type = attrs.getValue(Constants.URIS_SCHEMA_XSI[i], "type");
        
        if (type == null)
            return null;

        return getQNameFromString(type);
    }
    
    public ServiceDescription getServiceDescription()
    {
        return msgContext.getServiceDescription();
    }
    
    public TypeMappingRegistry getTypeMappingRegistry()
    {
        return msgContext.getTypeMappingRegistry();
    }
    
    public Object getObjectByRef(String href)
    {
        if ((idMap == null) || (href == null))
            return null;
        
        IDResolver resolver = (IDResolver)idMap.get(href);
        if (resolver == null)
            return null;
        
        return resolver.getReferencedObject(href);
    }
    
    public void registerFixup(String id, Deserializer dser)
    {
        if (fixups == null)
            fixups = new HashMap();
        
        fixups.put(id, dser);
    }
    
    public void registerElementByID(String id, MessageElement elem)
    {
        if (localIDs == null)
            localIDs = new LocalIDResolver();
        
        String absID = "#" + id;
        
        localIDs.addIDMapping(absID, elem);
        
        registerResolverForID(absID, localIDs);
        
        if (fixups != null) {
            Deserializer dser = (Deserializer)fixups.get(absID);
            if (dser != null) {
                elem.setFixupDeserializer(dser);
            }
        }
    }
    
    public void registerResolverForID(String id, IDResolver resolver)
    {
        if ((id == null) || (resolver == null)) {
            // ??? Throw nullPointerException?
            return;
        }
        
        if (idMap == null)
            idMap = new HashMap();
        
        idMap.put(id, resolver);
    }
    
    public int getCurrentRecordPos()
    {
        if (recorder == null) return -1;
        return recorder.getLength() - 1;
    }
    
    protected int startOfMappingsPos = -1;
    
    public int getStartOfMappingsPos()
    {
        if (startOfMappingsPos == -1) {
            return getCurrentRecordPos();
        }
        
        return startOfMappingsPos;
    }
    
    /****************************************************************
     * Management of sub-handlers (deserializers)
     */
    
    public SOAPHandler getTopHandler()
    {
        try {
            return (SOAPHandler)handlerStack.peek();
        } catch (Exception e) {
            return null;
        }
    }
    
    public void pushElementHandler(SOAPHandler handler)
    {
        if (DEBUG_LOG) {
            System.out.println("Pushing handler " + handler);
        }
        
        handlerStack.push(handler);
    }
    
    /** Replace the handler at the top of the stack.
     * 
     * This is only used when we have a placeholder Deserializer
     * for a referenced object which doesn't know its type until we
     * hit the referent.
     */
    void replaceElementHandler(SOAPHandler handler)
    {
        handlerStack.pop();
        handlerStack.push(handler);
    }
    
    public SOAPHandler popElementHandler()
    {
        if (!handlerStack.empty()) {
            SOAPHandler handler = getTopHandler();
            if (DEBUG_LOG) {
                System.out.println("Popping handler " + handler);
            }
            handlerStack.pop();
            return handler;
        } else {
            if (DEBUG_LOG) {
                System.out.println("Popping handler...(null)");
            }
            return null;
        }
    }
    
    /****************************************************************
     * SAX event handlers
     */
    public void startDocument() throws SAXException {
        // Should never receive this in the midst of a parse.
        if (recorder != null)
            recorder.startDocument();
    }
    
    public void endDocument() throws SAXException {
        if (DEBUG_LOG) {
            System.err.println("EndDocument");
        }
        if (recorder != null)
            recorder.endDocument();
    }
    
    /** Record the current set of prefix mappings in the nsMappings table.
     *
     * !!! We probably want to have this mapping be associated with the
     *     MessageElements, since they may potentially need access to them
     *     long after the end of the prefix mapping here.  (example:
     *     when we need to record a long string of events scanning forward
     *     in the document to find an element with a particular ID.)
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException
    {
        if (recorder != null)
            recorder.startPrefixMapping(prefix, uri);
        
        if (startOfMappingsPos == -1)
            startOfMappingsPos = getCurrentRecordPos();
        
        if (prefix != null) {
            namespaces.add(uri, prefix);
        } else {
            namespaces.add(uri, "");
        }
       
        if (DEBUG_LOG) {
            System.err.println("StartPrefixMapping '" + prefix + "'->'" + uri + "'");
        }
        
        SOAPHandler handler = getTopHandler();
        if (handler != null)
            handler.startPrefixMapping(prefix, uri);
    }
    
    public void endPrefixMapping(String prefix)
        throws SAXException
    {
        if (DEBUG_LOG) {
            System.err.println("EndPrefixMapping '" + prefix + "'");
        }
        
        if (recorder != null)
            recorder.endPrefixMapping(prefix);
        
        SOAPHandler handler = getTopHandler();
        if (handler != null)
            handler.endPrefixMapping(prefix);
    }
    
    public void setDocumentLocator(Locator locator) 
    {
        if (recorder != null)
            recorder.setDocumentLocator(locator);
        this.locator = locator;
    }

    public void characters(char[] p1, int p2, int p3) throws SAXException {
        if (recorder != null)
            recorder.characters(p1, p2, p3);
        if (getTopHandler() != null)
            getTopHandler().characters(p1, p2, p3);
    }
    
    public void ignorableWhitespace(char[] p1, int p2, int p3) throws SAXException {
        if (recorder != null)
            recorder.ignorableWhitespace(p1, p2, p3);
        if (getTopHandler() != null)
            getTopHandler().ignorableWhitespace(p1, p2, p3);
    }
 
    public void processingInstruction(String p1, String p2) throws SAXException {
        // must throw an error since SOAP 1.1 doesn't allow
        // processing instructions anywhere in the message
        throw new SAXException("Processing instructions are not allowed within SOAP Messages");
    }

    public void skippedEntity(String p1) throws SAXException {
        if (recorder != null)
            recorder.skippedEntity(p1);
        getTopHandler().skippedEntity(p1);
    }

    /** This is a big workhorse.  Manage the state of the parser, check for
     * basic SOAP compliance (envelope, then optional header, then body, etc).
     * 
     * This guy also handles monitoring the recording depth if we're recording
     * (so we know when to stop), and might eventually do things to help with
     * ID/HREF management as well.
     * 
     */
    public void startElement(String namespace, String localName,
                             String qName, Attributes attributes)
        throws SAXException
    {
        SOAPHandler nextHandler = null;

        if (DEBUG_LOG) {
            System.out.println("startElement ['" + namespace + "' " +
                           localName + "]");
        }
        
        namespaces.push();
        
        if (recorder != null)
            recorder.startElement(namespace, localName, qName,
                                  attributes);
        
        String prefix = "";
        int idx = qName.indexOf(":");
        if (idx > 0)
            prefix = qName.substring(0, idx);

        if (!handlerStack.isEmpty()) {
            nextHandler = getTopHandler().onStartChild(namespace,
                                                       localName,
                                                       prefix,
                                                       attributes,
                                                       this);
        }
        
        if (nextHandler == null)
            nextHandler = nullHandler;
        
        pushElementHandler(nextHandler);
        
        nextHandler.startElement(namespace, localName, qName,
                                 attributes, this);
        
        startOfMappingsPos = -1;
    }
    
    public void endElement(String namespace, String localName, String qName)
        throws SAXException
    {
        if (DEBUG_LOG) {
            System.out.println("endElement ['" + namespace + "' " +
                           localName + "]");
        }
        
        if (recorder != null)
            recorder.endElement(namespace, localName, qName);
        
        try {
            SOAPHandler handler = popElementHandler();
            handler.endElement(namespace, localName, this);
            
            if (!handlerStack.isEmpty()) {
                getTopHandler().onEndChild(namespace, localName, this);
            } else {
                // We should be done!
                if (DEBUG_LOG) {
                    System.out.println("Done with document!");
                }
            }
            
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}

