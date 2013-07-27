package net.arnx.xmlic.internal.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	private Map<String, List<String>> nsMap = Collections.synchronizedMap(new LinkedHashMap<String, List<String>>());
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
		synchronized (this) {
			List<String> list = nsMap.get(prefix);
			if (list == null) {
				list = new ArrayList<String>(1);
				list.add(namespaceURI);
				nsMap.put(prefix, list);
			} else if (!list.contains(namespaceURI)) {
				list.add(namespaceURI);
			}
		}
	}
	
	public String getNamespaceURI(String prefix) {
		if (prefix == null) {
			throw new IllegalArgumentException("prefix is null.");
		} else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			List<String> list = nsMap.get(prefix);
			return (list != null && !list.isEmpty()) ? list.get(0) : XMLConstants.NULL_NS_URI;
		} else if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		} else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		} else {
			List<String> list = nsMap.get(prefix);
			return (list != null && !list.isEmpty()) ? list.get(0) : null;
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
			for (Map.Entry<String, List<String>> entry : nsMap.entrySet()) {
				if (entry.getValue().contains(namespaceURI)) {
					return entry.getKey();
				}
			}
			return null;
		}
	}
	
	public Set<String> getPrefixes() {
		return Collections.unmodifiableSet(nsMap.keySet());
	}
	
	public Iterator<?> getPrefixes(String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException("namespaceURI is null.");
		} else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.DEFAULT_NS_PREFIX).iterator();
		} else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.XML_NS_PREFIX).iterator();
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.XMLNS_ATTRIBUTE).iterator();
		} else {
			Set<String> result = new LinkedHashSet<String>();
			for (Map.Entry<String, List<String>> entry : nsMap.entrySet()) {
				if (entry.getValue().contains(namespaceURI)) {
					result.add(entry.getKey());
				}
			}
			return Collections.unmodifiableSet(result).iterator();
		}
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
