package net.arnx.xmlic.internal.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionContext;
import net.arnx.xmlic.internal.org.jaxen.NamespaceContext;
import net.arnx.xmlic.internal.org.jaxen.SimpleVariableContext;
import net.arnx.xmlic.internal.org.jaxen.UnresolvableException;
import net.arnx.xmlic.internal.org.jaxen.VariableContext;
import net.arnx.xmlic.internal.org.jaxen.XPathFunctionContext;

public class XMLContext implements NamespaceContext, VariableContext, FunctionContext, Serializable {
	private static final long serialVersionUID = 1L;
	
	private Map<String, String> nsMap = new ConcurrentHashMap<String, String>();
	private SimpleVariableContext varContext = new SimpleVariableContext();
	private XPathFunctionContext fnContext = new XPathFunctionContext(false);
	
	public XMLContext() {
	}
	
	public static DocumentBuilder getDocumentBuilder() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setCoalescing(true);
		dbf.setNamespaceAware(true);
		dbf.setExpandEntityReferences(true);
		dbf.setXIncludeAware(true);
		try {
			return dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public void addNamespace(String prefix, String namespaceURI) {
		nsMap.put(prefix, namespaceURI);
	}
	
	public void removeNamespace(String prefix) {
		nsMap.remove(prefix);
	}
	
	public String getNamespaceURI(String prefix) {
		if (prefix == null) {
			throw new IllegalArgumentException("prefix is null.");
		} else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			String uri= nsMap.get(prefix);
			return (uri != null) ? uri : XMLConstants.NULL_NS_URI;
		} else if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		} else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		} else {
			return nsMap.get(prefix);
		}
	}
	
	public String getPrefix(String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException("namespaceURI is null.");
		} else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
			return XMLConstants.DEFAULT_NS_PREFIX;
		} else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XML_NS_PREFIX;
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XMLNS_ATTRIBUTE;
		} else {
			for (Map.Entry<String, String> entry : nsMap.entrySet()) {
				if (namespaceURI.equals(entry.getValue())) {
					return entry.getKey();
				}
			}
			return null;
		}
	}
	
	public Collection<String> getPrefixes() {
		return nsMap.keySet();
	}
	
	@Override
	public String translateNamespacePrefixToUri(String prefix) {
		return getNamespaceURI(prefix);
	}
	
	public void addVariable(String namespaceURI, String localName, Object value) {
		varContext.setVariableValue(namespaceURI, localName, value);
	}
	
	@Override
	public Object getVariableValue(String namespaceURI, String prefix, String localName) throws UnresolvableException {
		return varContext.getVariableValue(namespaceURI, prefix, localName);
	}
	
	public void addFunction(String namespaceURI, String localName, Function function) {
		fnContext.registerFunction(namespaceURI, localName, function);
	}

	@Override
	public Function getFunction(String namespaceURI, String prefix, String localName) throws UnresolvableException {
		return fnContext.getFunction(namespaceURI, prefix, localName);
	}
}
