package net.arnx.xmlic.internal.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.arnx.xmlic.Nodes;
import net.arnx.xmlic.XML;
import net.arnx.xmlic.XPathSyntaxException;
import net.arnx.xmlic.internal.function.CurrentFunction;
import net.arnx.xmlic.internal.function.DocumentFunction;
import net.arnx.xmlic.internal.function.KeyFunction;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionContext;
import net.arnx.xmlic.internal.org.jaxen.JaxenException;
import net.arnx.xmlic.internal.org.jaxen.NamespaceContext;
import net.arnx.xmlic.internal.org.jaxen.UnresolvableException;
import net.arnx.xmlic.internal.org.jaxen.VariableContext;
import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.org.jaxen.XPathFunctionContext;
import net.arnx.xmlic.internal.org.jaxen.dom.DOMXPath;

public class XMLContext implements NamespaceContext, VariableContext, FunctionContext, Serializable {
	private static final long serialVersionUID = 1L;
	
	private ThreadLocal<Node> current = new ThreadLocal<Node>();
	
	private Map<String, String> nsMap = new ConcurrentHashMap<String, String>();
	private Map<String, Key> keyMap = new ConcurrentHashMap<String, Key>();
	private Map<QName, Object> varMap = new ConcurrentHashMap<QName, Object>();
	private XPathFunctionContext fnContext = new XPathFunctionContext(false);
	
	public XMLContext() {
		fnContext.registerFunction(null, "document", new DocumentFunction());
		fnContext.registerFunction(null, "current", new CurrentFunction());
		fnContext.registerFunction(null, "key", new KeyFunction());
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
	
	public Node getCurrentNode() {
		return current.get();
	}
	
	public void addKey(String name, Key key) {
		keyMap.put(name, key);
	}
	
	public Key getKey(String name) {
		return keyMap.get(name);
	}
	
	public void removeKey(String name) {
		keyMap.remove(name);
	}
	
	public void addVariable(String namespaceURI, String localName, Object value) {
		varMap.put(new QName(namespaceURI, localName), value);
	}
	
	@Override
	public Object getVariableValue(String namespaceURI, String prefix, String localName) throws UnresolvableException {
		return varMap.get(new QName(namespaceURI, localName));
	}
	
	public void addFunction(String namespaceURI, String localName, Function function) {
		fnContext.registerFunction(namespaceURI, localName, function);
	}

	@Override
	public Function getFunction(String namespaceURI, String prefix, String localName) throws UnresolvableException {
		return fnContext.getFunction(namespaceURI, prefix, localName);
	}
	
	public XPath compileXPath(String text) {
		XPath xpath;
		try {
			xpath = new DOMXPath(text);
			xpath.setNamespaceContext(this);
			xpath.setVariableContext(this);
			xpath.setFunctionContext(this);
		} catch (net.arnx.xmlic.internal.org.jaxen.XPathSyntaxException e) {
			throw new XPathSyntaxException(e.getXPath(), e.getPosition(), e.getMultilineMessage(), e);
		} catch (JaxenException e) {
			throw new IllegalArgumentException(e);
		}
		return xpath;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T evaluate(XML owner, XPath xpath, Node node, Class<T> cls) {
		try {
			current.set(node);
			
			if (cls.equals(Nodes.class)) {
				List<Node> list = (List<Node>)xpath.selectNodes(node);
				return (T)((list != null) ? owner.translate(list) : null);
			} else if (cls.equals(List.class)) {
				return (T)xpath.selectNodes(node);
			} else if (cls.equals(NodeList.class)) {
				return (T)new ListNodeList(xpath.selectNodes(node));
			} else if (cls.equals(Node.class)) {
				return (T)xpath.selectSingleNode(node);
			} else if (cls.equals(String.class)) {
				return (T)xpath.stringValueOf(node);
			} else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
				return (T)Boolean.valueOf(xpath.booleanValueOf(node));
			} else if (cls.equals(Number.class) || cls.equals(double.class) || cls.equals(Double.class)) {
				Number num = xpath.numberValueOf(node);
				return (T)(Double)((num != null) ? num.doubleValue() : cls.equals(double.class) ? 0.0 : null);
			} else if (cls.equals(float.class) || cls.equals(Float.class)) {
				Number num = xpath.numberValueOf(node);
				return (T)(Float)((num != null) ? num.floatValue() : cls.equals(float.class) ? 0.0F : null);
			} else if (cls.equals(long.class) || cls.equals(Long.class)) {
				Number num = xpath.numberValueOf(node);
				return (T)(Long)((num != null) ? num.longValue() : cls.equals(long.class) ? 0L : null);
			} else if (cls.equals(int.class) || cls.equals(Integer.class)) {
				Number num = xpath.numberValueOf(node);
				return (T)(Integer)((num != null) ? num.intValue() : cls.equals(int.class) ? 0 : null);
			} else if (cls.equals(short.class) || cls.equals(Short.class)) {
				Number num = xpath.numberValueOf(node);
				return (T)(Short)((num != null) ? num.shortValue() : cls.equals(short.class) ? (short)0 : null);
			} else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
				Number num = xpath.numberValueOf(node);
				return (T)(Byte)((num != null) ? num.byteValue() : cls.equals(byte.class) ? (byte)0 : null);
			} else if (cls.equals(BigDecimal.class)) {
				Number num = xpath.numberValueOf(node);
				return (T)((num != null) ? new BigDecimal(num.doubleValue()) : null);
			} else {
				throw new UnsupportedOperationException("Unsupported Convert class: " + cls);
			}
		} catch (net.arnx.xmlic.internal.org.jaxen.XPathSyntaxException e) {
			throw new XPathSyntaxException(e.getXPath(), e.getPosition(), e.getMultilineMessage(), e);
		} catch (JaxenException e) {
			throw new IllegalStateException(e);
		} finally {
			current.remove();
		}
	}
	
	public static class Key {
		public final String match;
		public final String use;
		
		public Key(String match, String use) {
			this.match = match;
			this.use = use;
		}
	}
	
	
	private static class ListNodeList implements NodeList {
		private List<Node> items;
		
		public ListNodeList(List<Node> items) {
			this.items = items;
		}
		
		@Override
		public Node item(int index) {
			return items.get(index);
		}

		@Override
		public int getLength() {
			return items.size();
		}	
	}
}
