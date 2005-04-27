/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis.om.impl.llom.builder;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.axis.om.OMConstants;
import org.apache.axis.om.OMElement;
import org.apache.axis.om.OMException;
import org.apache.axis.om.OMFactory;
import org.apache.axis.om.OMNamespace;
import org.apache.axis.om.OMNode;
import org.apache.axis.om.OMXMLParserWrapper;
import org.apache.axis.om.impl.llom.OMElementImpl;
import org.apache.axis.om.impl.llom.OMNodeImpl;

/**
 * OM should be able to built from any data source. And the model it builds may be a SOAP specific one
 * or just an XML model. This class will give some common functionality of OM Building from StAX.
 */
public abstract class StAXBuilder implements OMXMLParserWrapper {
    /**
     * Field ombuilderFactory
     */
    protected OMFactory ombuilderFactory;

    /**
     * Field parser
     */
    protected XMLStreamReader parser;

    /**
     * Field omfactory
     */
    protected OMFactory omfactory;

    /**
     * Field lastNode
     */
    protected OMNode lastNode;

    // returns the state of completion

    /**
     * Field done
     */
    protected boolean done = false;

    // keeps the state of the cache

    /**
     * Field cache
     */
    protected boolean cache = true;

    // keeps the state of the parser access. if the parser is
    // accessed atleast once,this flag will be set

    /**
     * Field parserAccessed
     */
    protected boolean parserAccessed = false;

    /**
     * Constructor StAXBuilder
     *
     * @param ombuilderFactory
     * @param parser
     */
    protected StAXBuilder(OMFactory ombuilderFactory, XMLStreamReader parser) {
        this.ombuilderFactory = ombuilderFactory;
        this.parser = parser;
        omfactory = OMFactory.newInstance();
    }

    /**
     * Constructor StAXBuilder
     *
     * @param parser
     */
    protected StAXBuilder(XMLStreamReader parser) {
        this(OMFactory.newInstance(), parser);
        omfactory = OMFactory.newInstance();
    }

    /**
     * Method setOmbuilderFactory
     *
     * @param ombuilderFactory
     */
    public void setOmbuilderFactory(OMFactory ombuilderFactory) {
        this.ombuilderFactory = ombuilderFactory;
    }

    /**
     * Method processNamespaceData
     *
     * @param node
     * @param isSOAPElement
     */
    protected abstract void processNamespaceData(OMElement node,
                                                 boolean isSOAPElement);

    // since the behaviors are different when it comes to namespaces
    // this must be implemented differently

    /**
     * Method processAttributes
     *
     * @param node
     */
    protected void processAttributes(OMElement node) {
        int attribCount = parser.getAttributeCount();
        for (int i = 0; i < attribCount; i++) {
            OMNamespace ns = null;
            String uri = parser.getAttributeNamespace(i);
            if (uri.hashCode() != 0) {
                ns = node.findNamespace(uri,
                        parser.getAttributePrefix(i));
            }

            // todo if the attributes are supposed to namespace qualified all the time
            // todo then this should throw an exception here
            node.addAttribute(parser.getAttributeLocalName(i),
                    parser.getAttributeValue(i), ns);
        }
    }

    /**
     * Method createOMText
     *
     * @return
     * @throws OMException
     */
    protected OMNode createOMText() throws OMException {
        if (lastNode == null) {
            throw new OMException();
        }
        OMNode node;
        if (lastNode.isComplete()) {
            node = omfactory.createText(lastNode.getParent(), parser.getText());
            lastNode.setNextSibling(node);
            node.setPreviousSibling(lastNode);
        } else {
            OMElement e = (OMElement) lastNode;
            node = omfactory.createText(e, parser.getText());
            e.setFirstChild(node);
        }
        return node;
    }

    /**
     * Method reset
     *
     * @param node
     * @throws OMException
     */
    public void reset(OMNode node) throws OMException {
        lastNode = null;
    }

    /**
     * Method discard
     *
     * @param el
     * @throws OMException
     */
    public void discard(OMElement el) throws OMException {
        OMElementImpl elementImpl = null;
        if (el instanceof OMElementImpl) {
            elementImpl = (OMElementImpl) el;
        } else {
            throw new OMException();
        }
        if (elementImpl.isComplete() || !cache) {
            throw new OMException();
        }
        try {
            cache = false;
            do {
                while (parser.next() != XMLStreamConstants.END_ELEMENT) ;

                // TODO:
            } while (!parser.getName().equals(elementImpl.getQName()));
            lastNode = (OMNodeImpl) elementImpl.getPreviousSibling();
            if (lastNode != null) {
                lastNode.setNextSibling(null);
            } else {
                OMElement parent = elementImpl.getParent();
                if (parent == null) {
                    throw new OMException();
                }
                parent.setFirstChild(null);
                lastNode = parent;
            }
            cache = true;
        } catch (OMException e) {
            throw e;
        } catch (Exception e) {
            throw new OMException(e);
        }
    }

    /**
     * Method getText
     *
     * @return
     * @throws OMException
     */
    public String getText() throws OMException {
        return parser.getText();
    }

    /**
     * Method getNamespace
     *
     * @return
     * @throws OMException
     */
    public String getNamespace() throws OMException {
        return parser.getNamespaceURI();
    }

    /**
     * Method getNamespaceCount
     *
     * @return
     * @throws OMException
     */
    public int getNamespaceCount() throws OMException {
        try {
            return parser.getNamespaceCount();
        } catch (Exception e) {
            throw new OMException(e);
        }
    }

    /**
     * Method getNamespacePrefix
     *
     * @param index
     * @return
     * @throws OMException
     */
    public String getNamespacePrefix(int index) throws OMException {
        try {
            return parser.getNamespacePrefix(index);
        } catch (Exception e) {
            throw new OMException(e);
        }
    }

    /**
     * Method getNamespaceUri
     *
     * @param index
     * @return
     * @throws OMException
     */
    public String getNamespaceUri(int index) throws OMException {
        try {
            return parser.getNamespaceURI(index);
        } catch (Exception e) {
            throw new OMException(e);
        }
    }

    /**
     * Method setCache
     *
     * @param b
     */
    public void setCache(boolean b) {
        if (parserAccessed && b) {
            throw new UnsupportedOperationException(
                    "parser accessed. cannot set cache");
        }
        cache = b;
    }

    /**
     * Method getName
     *
     * @return
     * @throws OMException
     */
    public String getName() throws OMException {
        return parser.getLocalName();
    }

    /**
     * Method getPrefix
     *
     * @return
     * @throws OMException
     */
    public String getPrefix() throws OMException {
        return parser.getPrefix();
    }

    /**
     * Method getAttributeCount
     *
     * @return
     * @throws OMException
     */
    public int getAttributeCount() throws OMException {
        return parser.getAttributeCount();
    }

    /**
     * Method getAttributeNamespace
     *
     * @param arg
     * @return
     * @throws OMException
     */
    public String getAttributeNamespace(int arg) throws OMException {
        return parser.getAttributeNamespace(arg);
    }

    /**
     * Method getAttributeName
     *
     * @param arg
     * @return
     * @throws OMException
     */
    public String getAttributeName(int arg) throws OMException {
        return parser.getAttributeNamespace(arg);
    }

    /**
     * Method getAttributePrefix
     *
     * @param arg
     * @return
     * @throws OMException
     */
    public String getAttributePrefix(int arg) throws OMException {
        return parser.getAttributeNamespace(arg);
    }

    /**
     * Method getParser
     *
     * @return
     */
    public Object getParser() {
        if (!cache) {
            parserAccessed = true;
            return parser;
        } else {
            throw new UnsupportedOperationException(
                    "cache must be switched off to access the parser");
        }
    }

    /**
     * Method isCompleted
     *
     * @return
     */
    public boolean isCompleted() {
        return done;
    }

    /**
     * This method will be called with the XMLStreamConstants.START_ELEMENT event
     *
     * @return
     * @throws OMException
     */
    protected abstract OMNode createOMElement() throws OMException;

    /**
     * This should proceed the parser one step further, if parser is not completed yet.
     * If this has been called whist parser is done, then throw an OMException.
     * If the cache is set to false, then should be return the event, *without* building the OM tree.
     * If the cache is set to true, then this should handle all the events within this, and should build
     * the object structure appropriately and return the event.
     *
     * @return
     * @throws OMException
     */
    public abstract int next() throws OMException;

    /**
     * @return
     */
    public short getBuilderType() {
        return OMConstants.PULL_TYPE_BUILDER;
    }

    /**
     * Method registerExternalContentHandler
     *
     * @param obj
     */
    public void registerExternalContentHandler(Object obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * Method getRegisteredContentHandler
     *
     * @return
     */
    public Object getRegisteredContentHandler() {
        throw new UnsupportedOperationException();
    }
}