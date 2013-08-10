package net.arnx.xmlic.internal.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.arnx.xmlic.Nodes;
import net.arnx.xmlic.XML;
import net.arnx.xmlic.XPathSyntaxException;
import net.arnx.xmlic.internal.function.CurrentFunction;
import net.arnx.xmlic.internal.function.DocumentFunction;
import net.arnx.xmlic.internal.function.KeyFunction;
import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.ContextSupport;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.JaxenException;
import net.arnx.xmlic.internal.org.jaxen.NamespaceContext;
import net.arnx.xmlic.internal.org.jaxen.UnresolvableException;
import net.arnx.xmlic.internal.org.jaxen.VariableContext;
import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.org.jaxen.XPathFunctionContext;
import net.arnx.xmlic.internal.org.jaxen.dom.DocumentNavigator;
import net.arnx.xmlic.internal.org.jaxen.function.BooleanFunction;
import net.arnx.xmlic.internal.org.jaxen.function.NumberFunction;
import net.arnx.xmlic.internal.org.jaxen.function.StringFunction;
import net.arnx.xmlic.internal.org.jaxen.pattern.Pattern;
import net.arnx.xmlic.internal.org.jaxen.pattern.PatternParser;
import net.arnx.xmlic.internal.org.jaxen.saxpath.SAXPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLContext implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final String VARIABLE_NAME = "__XML_CONTEXT__";
	
	private ThreadLocal<Node> current = new ThreadLocal<Node>();
	
	private Map<String, Key> keyMap = new ConcurrentHashMap<String, Key>();
	private NamespaceContextImpl nsContext = new NamespaceContextImpl();
	private VariableContextImpl varContext = new VariableContextImpl();
	private FunctionContextImpl fnContext = new FunctionContextImpl();
	private ContextSupport support = new ContextSupport(nsContext, fnContext, varContext, 
			DocumentNavigator.getInstance());
	
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
	
	public ContextSupport getContextSupport() {
		return support;
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
	
	public void addNamespace(String prefix, String namespaceURI) {
		nsContext.addNamespace(prefix, namespaceURI);
	}
	
	public void removeNamespace(String prefix) {
		nsContext.removeNamespace(prefix);
	}
	
	public String getNamespaceURI(String prefix) {
		return nsContext.translateNamespacePrefixToUri(prefix);
	}
	
	public String getPrefix(String namespaceURI) {
		return nsContext.getPrefix(namespaceURI);
	}
	
	public Collection<String> getPrefixes() {
		return nsContext.getPrefixes();
	}
	
	public void addVariable(String namespaceURI, String localName, Object value) {
		varContext.addVariable(namespaceURI, localName, value);
	}
	
	public Object getVariable(String namespaceURI, String localName) {
		try {
			return varContext.getVariableValue(namespaceURI, null, localName);
		} catch (UnresolvableException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void addFunction(String namespaceURI, String localName, Function function) {
		fnContext.registerFunction(namespaceURI, localName, function);
	}
	
	public Function getFunction(String namespaceURI, String localName) {
		try {
			return fnContext.getFunction(namespaceURI, null, localName);
		} catch (UnresolvableException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public NodeMatcher compileXPathPattern(String text) {
		final Pattern pattern;
		final Context context = new Context(support);
		try {
			pattern = PatternParser.parse(text);
		} catch (net.arnx.xmlic.internal.org.jaxen.XPathSyntaxException e) {
			throw new XPathSyntaxException(e.getXPath(), e.getPosition(), e.getMultilineMessage(), e);
		} catch (JaxenException e) {
			throw new IllegalArgumentException(e);
		} catch (SAXPathException e) {
			throw new IllegalArgumentException(e);
		}
		
		return new NodeMatcher() {
			@Override
			public boolean match(Node node) {
				try {
					return pattern.matches(node, context);
				} catch (JaxenException e) {
					throw new IllegalStateException(e);
				}
			}
		};
	}
	
	public XPath compileXPath(String text, boolean pattern) {
		return  new XmlicXPath(this, text, pattern);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T evaluate(XML owner, XPath xpath, Node node, Class<T> cls) {
		try {
			current.set(node);
			
			List result = xpath.selectNodes(node);
			if (cls.equals(Object.class)) {
				return (T)result;
			} if (!cls.isPrimitive() 
					&& (result == null || result.isEmpty() || (result.size() == 1 && result.get(0) == null))) {
				if (cls.equals(Nodes.class)) {
					List<Node> nodes = Collections.emptyList();
					return (T)new Nodes(owner, nodes);
				} else if (cls.equals(List.class)) {
					return (T)new ArrayList(0);
				} else if (cls.equals(NodeList.class)) {
					return (T)new ListNodeList(Collections.EMPTY_LIST);
				} else if (cls.equals(String.class)
						|| cls.equals(Boolean.class)
						|| cls.equals(Short.class)
						|| cls.equals(Integer.class)
						|| cls.equals(Long.class)
						|| cls.equals(Double.class)
						|| cls.equals(Number.class)
						|| cls.equals(BigInteger.class)
						|| cls.equals(BigDecimal.class)) {
					return null;
				} else {
					throw new UnsupportedOperationException("class " + cls.getName() + " is unsupported.");
				}
			} else if (cls.equals(Nodes.class)) {
				if (result.size() == 1) {
					if (result.get(0) instanceof Node) {
						return (T)new Nodes(owner, (Node)result.get(0));
					} else {
						throw new UnsupportedOperationException("result is not Node: " + result.get(0).getClass().getName());
					}
				} else {
					throw new IllegalStateException("multiple result found.");
				}
			} else if (cls.equals(List.class)) {
				if (result.get(0) instanceof Node) {
					return (T)result;
				} else {
					throw new UnsupportedOperationException("result is not Node: " + result.get(0).getClass().getName());
				}
			} else if (cls.equals(NodeList.class)) {
				if (result.get(0) instanceof Node) {
					return (T)new ListNodeList(result);
				} else {
					throw new UnsupportedOperationException("result is not Node: " + result.get(0).getClass().getName());
				}
			} else if (cls.equals(String.class)) {
				return (T)StringFunction.evaluate(result, xpath.getNavigator());
			} else if (cls.equals(Boolean.class) || cls.equals(boolean.class)) {
				return (T)BooleanFunction.evaluate(result, xpath.getNavigator());
			} else if (cls.equals(Short.class) || cls.equals(short.class)) {
				return (T)Short.valueOf(NumberFunction.evaluate(result, xpath.getNavigator()).shortValue());
			} else if (cls.equals(Integer.class) || cls.equals(int.class)) {
				return (T)Integer.valueOf(NumberFunction.evaluate(result, xpath.getNavigator()).intValue());
			} else if (cls.equals(Long.class) || cls.equals(long.class)) {
				return (T)Long.valueOf(NumberFunction.evaluate(result, xpath.getNavigator()).longValue());
			} else if (cls.equals(Float.class) || cls.equals(float.class)) {
				return (T)Float.valueOf(NumberFunction.evaluate(result, xpath.getNavigator()).floatValue());
			} else if (cls.equals(Double.class) || cls.equals(double.class) || cls.equals(Number.class)) {
				return (T)NumberFunction.evaluate(result, xpath.getNavigator());
			} else if (cls.equals(BigInteger.class)) {
				return (T)BigInteger.valueOf(NumberFunction.evaluate(result, xpath.getNavigator()).longValue());
			} else if (cls.equals(BigDecimal.class)) {
				return (T)BigDecimal.valueOf(NumberFunction.evaluate(result, xpath.getNavigator()));
			} else {
				throw new UnsupportedOperationException("class " + cls.getName() + " is unsupported.");
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
	
	private class NamespaceContextImpl implements NamespaceContext {
		private Map<String, String> nsMap = new ConcurrentHashMap<String, String>();
		
		public void addNamespace(String prefix, String namespaceURI) {
			nsMap.put(prefix, namespaceURI);
		}
		
		public void removeNamespace(String prefix) {
			nsMap.remove(prefix);
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
	}
	
	private class VariableContextImpl implements VariableContext {
		private Map<QName, Object> varMap = new ConcurrentHashMap<QName, Object>();
		
		public void addVariable(String namespaceURI, String localName, Object value) {
			varMap.put(new QName(namespaceURI, localName), value);
		}
		
		@Override
		public Object getVariableValue(String namespaceURI, String prefix, String localName) throws UnresolvableException {
			if (namespaceURI == null && VARIABLE_NAME.equals(localName)) {
				return XMLContext.this;
			}
			return varMap.get(new QName(namespaceURI, localName));
		}
	}
	
	private class FunctionContextImpl extends XPathFunctionContext {
		public FunctionContextImpl() {
			super(false);
			registerFunction(null, "document", new DocumentFunction());
			registerFunction(null, "current", new CurrentFunction());
			registerFunction(null, "key", new KeyFunction());
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
