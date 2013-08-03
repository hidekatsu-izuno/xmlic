package net.arnx.xmlic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import net.arnx.xmlic.internal.function.CurrentFunction;
import net.arnx.xmlic.internal.function.DocumentFunction;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionContext;
import net.arnx.xmlic.internal.org.jaxen.JaxenException;
import net.arnx.xmlic.internal.org.jaxen.UnresolvableException;
import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.org.jaxen.dom.DOMXPath;
import net.arnx.xmlic.internal.util.XMLContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XML implements Serializable {
	private static final long serialVersionUID = 1L;

	public static XML load(File file) throws IOException {
		XMLLoader parser = new XMLLoader();
		return new XML(parser.load(file.toURI()));
	}
	
	public static XML load(URI uri) throws IOException {
		XMLLoader parser = new XMLLoader();
		return new XML(parser.load(uri));
	}
	
	public static XML load(URL url) throws IOException {
		try {
			XMLLoader parser = new XMLLoader();
			return new XML(parser.load(url.toURI()));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static XML load(InputStream in) throws IOException {
		XMLLoader parser = new XMLLoader();
		return new XML(parser.load(in));
	}
	
	public static XML load(Reader reader) throws IOException {
		XMLLoader parser = new XMLLoader();
		return new XML(parser.load(reader));
	}
	
	final Document doc;
	final XMLContext xmlContext;
	
	public XML() {
		this(null);
	}
	
	public XML(Document doc) {
		this.xmlContext = new XMLContext();
		if (doc != null) {
			this.doc = doc;
			
			Object expr = compileXPath("//namespace::*");
			NodeList list = evaluate(expr, doc, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				String prefix = node.getLocalName();
				if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
					prefix = XMLConstants.DEFAULT_NS_PREFIX;
				}
				
				if (xmlContext.getNamespaceURI(prefix) == null) {
					xmlContext.addNamespace(prefix, node.getNodeValue());
				}
			}
		} else {
			this.doc = XMLContext.getDocumentBuilder().newDocument();
		}
		
		xmlContext.addFunction(null, "document", new DocumentFunction(this.doc.getBaseURI()));
	}
	
	XML(Document doc, XMLContext nsContext) {
		this.doc = doc;
		this.xmlContext = nsContext;
	}
	
	public Document getDocument() {
		return doc;
	}
	
	public void addNamespaceMapping(String prefix, String uri) {
		xmlContext.addNamespace(prefix, uri);
	}
	
	public String getNamespaceMapping(String prefix) {
		return xmlContext.getNamespaceURI(prefix);
	}
	
	public void removeNamespaceMapping(String prefix) {
		xmlContext.removeNamespace(prefix);
	}
	
	public Nodes doc() {
		Nodes nodes = new Nodes(this, null, 1);
		nodes.add(doc);
		return nodes;
	}
	
	public Nodes translate(Node node) {
		if (node == null) {
			return new Nodes(this, null, 0);
		}
		return new Nodes(this, null, node);
	}
	
	public Nodes translate(Node... list) {
		return translate(Arrays.asList((Node[])list));
	}
	
	public Nodes translate(Collection<Node> list) {
		if (list == null || list.isEmpty()) {
			return new Nodes(this, null, 0);
		}
		
		Nodes nodes = new Nodes(this, null, list.size());
		nodes.addAll(list);
		Nodes.unique(nodes);
		return nodes;
	}
	
	public Nodes translate(NodeList list) {
		if (list == null || list.getLength() == 0) {
			return new Nodes(this, null, 0);
		}
		
		Nodes nodes = new Nodes(this, null, list.getLength());
		for (int i = 0; i < list.getLength(); i++) {
			nodes.add(list.item(i));
		}
		Nodes.unique(nodes);
		return nodes;
	}
	
	public Nodes parse(String text) {
		if (text == null || text.isEmpty()) {
			return new Nodes(this, null, 0);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<x");
		
		for (String prefix : xmlContext.getPrefixes()) {
			String uri = xmlContext.getNamespaceURI(prefix);
			if (prefix != null && !prefix.isEmpty()) {
				sb.append(" xmlns:").append(prefix).append("=\"");
			} else {
				sb.append(" xmlns=\"");
			}
			sb.append(uri.replace("\"", "&quot;")).append("\"");
		}
		sb.append(">").append(text).append("</x>");
		
		try {
			DocumentBuilder db = XMLContext.getDocumentBuilder();
			InputSource src = new InputSource(new StringReader(sb.toString()));
			src.setPublicId(doc.getBaseURI());
			Document ndoc = db.parse(src);
			
			NodeList list = ndoc.getDocumentElement().getChildNodes();
			Nodes nodes = new Nodes(this, null, list.getLength());
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				nodes.add(node);
			}
			return nodes;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public <T> T evaluate(String xpath, Class<T> cls) {
		return doc().evaluate(xpath, cls);
	}
	
	public Nodes select(String xpath) {
		return doc().select(xpath);
	}
	
	public Nodes find(String xpath) {
		return doc().find(xpath);
	}
	
	public Nodes remove(String xpath) {
		return doc().remove(xpath);
	}
	
	public Nodes normalize() {
		return doc().normalize();
	}
	
	public XML clone() {
		return new XML((Document)doc.cloneNode(true), xmlContext);
	}
	
	public Transformer stylesheet() throws TransformerConfigurationException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Source src = tf.getAssociatedStylesheet(new DOMSource(doc), null, null, null);
		return (src != null) ? tf.newTransformer(src) : null;
	}
	
	public XML transform(Transformer t) throws TransformerException {
		DOMResult result = new DOMResult();
		t.transform(new DOMSource(doc), result);
		return new XML((Document)result.getNode(), xmlContext);
	}
	
	public void writeTo(File file) throws IOException {
		writeTo(new FileOutputStream(file));
	}
	
	public void writeTo(OutputStream out) throws IOException {
		XMLWriter serializer = new XMLWriter();
		serializer.setEncoding("UTF-8");
	
		try {
			serializer.writeTo(out, doc);
		} finally {
			out.close();
		}
	}
	
	public void writeTo(Writer writer) throws IOException {
		XMLWriter serializer = new XMLWriter();
		
		try {
			serializer.writeTo(writer, doc);
		} finally {
			writer.close();
		}
	}
	
	Object compileXPath(String text) {
		XPath xpath;
		try {
			xpath = new DOMXPath(text);
			xpath.setNamespaceContext(xmlContext);
			xpath.setVariableContext(xmlContext);
			xpath.setFunctionContext(xmlContext);
		} catch (JaxenException e) {
			throw new IllegalArgumentException(e);
		}
		return xpath;
	}
	
	@SuppressWarnings("unchecked")
	<T> T evaluate(Object expr, Node node, Class<T> cls) {
		XPath xpath = (XPath)expr;
		
		final FunctionContext parent = xpath.getFunctionContext();
		final CurrentFunction current = new CurrentFunction(node);
		xpath.setFunctionContext(new FunctionContext() {
			@Override
			public Function getFunction(String namespaceURI, String prefix, String localName) throws UnresolvableException {
				if (namespaceURI == null && "current".equals(localName)) {
					return current;
				}
				return parent.getFunction(namespaceURI, prefix, localName);
			}
		});
		
		try {
			if (cls.equals(Nodes.class)) {
				List<Node> list = (List<Node>)xpath.selectNodes(node);
				return (T)((list != null) ? translate(list) : null);
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
		} catch (JaxenException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public String toString() {
		XMLWriter xwriter = new XMLWriter();
		xwriter.setShowXMLDeclaration(false);
		StringWriter writer = new StringWriter();
		try {
			xwriter.writeTo(writer, doc);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return writer.toString();
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
