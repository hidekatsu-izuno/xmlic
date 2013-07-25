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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import net.arnx.xmlic.internal.org.jaxen.JaxenException;
import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.org.jaxen.dom.DOMXPath;

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
	final ResourceResolver resResolver;
	final NamespaceContextImpl nsContext;
	
	public XML() {
		this(Collections.<String, String>emptyMap());
	}
	
	public XML(Map<String, String> namespaces) {
		this(getDocumentBuilder().newDocument(), namespaces);
	}
	
	public XML(Document doc) {
		this(doc, null);
	}
	
	public XML(Document doc, Map<String, String> namespaces) {
		this.doc = doc;
		this.resResolver = new ResourceResolver();
		
		NamespaceContextImpl nsContext = new NamespaceContextImpl();
		if (namespaces == null) {
			Object expr = compileXPath("//namespace::*");
			NodeList list = evaluate(expr, doc, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				String prefix = node.getLocalName();
				if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
					prefix = XMLConstants.DEFAULT_NS_PREFIX;
				}
				
				nsContext.addNamespace(prefix, node.getNodeValue());
			}
		} else {
			for (Map.Entry<String, String> entry : namespaces.entrySet()) {
				nsContext.addNamespace(entry.getKey(), entry.getValue());
			}
		}
		this.nsContext = nsContext;
	}
	
	XML(Document doc, ResourceResolver resResolver, NamespaceContextImpl nsContext) {
		this.doc = doc;
		this.resResolver = resResolver;
		this.nsContext = nsContext;
	}
	
	public Document getDocument() {
		return doc;
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
		
		for (Map.Entry<String, List<String>> entry : nsContext) {
			String prefix = entry.getKey();
			List<String> uris = entry.getValue();
			
			if (uris.isEmpty()) continue;
			if (prefix != null && !prefix.isEmpty()) {
				sb.append(" xmlns:").append(prefix).append("=\"");
			} else {
				sb.append(" xmlns=\"");
			}
			sb.append(uris.get(0).replace("\"", "&quot;")).append("\"");
		}
		sb.append(">").append(text).append("</x>");
		
		try {
			DocumentBuilder db = getDocumentBuilder();
			Document ndoc = db.parse(new InputSource(new StringReader(sb.toString())));
			
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
		return new XML((Document)doc.cloneNode(true), resResolver, nsContext);
	}
	
	public Transformer stylesheet() throws TransformerConfigurationException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Source src = tf.getAssociatedStylesheet(new DOMSource(doc), null, null, null);
		return (src != null) ? tf.newTransformer(src) : null;
	}
	
	public XML transform(Transformer t) throws TransformerException {
		t.setURIResolver(new ResourceResolver());
		DOMResult result = new DOMResult();
		t.transform(new DOMSource(doc), result);
		return new XML((Document)result.getNode(), resResolver, nsContext);
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
	
	static DocumentBuilder getDocumentBuilder() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setCoalescing(true);
		dbf.setNamespaceAware(true);
		dbf.setExpandEntityReferences(true);
		dbf.setXIncludeAware(true);
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new ResourceResolver());
			return db;
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	Object compileXPath(String xpath) {
		XPath xp;
		try {
			xp = new DOMXPath(xpath);
			if (nsContext != null) xp.setNamespaceContext(nsContext);
		} catch (JaxenException e) {
			throw new IllegalArgumentException(e);
		}
		return xp;
	}
	
	@SuppressWarnings("unchecked")
	<T> T evaluate(Object expr, Node node, Class<T> cls) {
		XPath xpath = (XPath)expr;
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
