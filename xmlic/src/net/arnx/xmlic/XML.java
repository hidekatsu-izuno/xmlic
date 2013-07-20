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
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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
	final NamespaceContext nsContext;
	
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
		
		if (namespaces == null) {
			NamespaceContextImpl context = new NamespaceContextImpl();
			XPathExpression expr = compileXPath("//namespace::*");
			NodeList list = evaluate(expr, doc, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				String prefix = node.getLocalName();
				if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
					prefix = XMLConstants.DEFAULT_NS_PREFIX;
				}
				
				context.addNamespace(prefix, node.getNodeValue());
			}
			this.nsContext = context;
		} else {
			this.nsContext = new NamespaceContextImpl(namespaces);
		}
	}
	
	XML(Document doc, ResourceResolver resolver, NamespaceContext context) {
		this.doc = doc;
		this.resResolver = resolver;
		this.nsContext = context;
	}
	
	public Document get() {
		return doc;
	}
	
	public Nodes doc() {
		Nodes nodes = new Nodes(this, null, 1);
		nodes.add(doc);
		return nodes;
	}
	
	public Nodes convert(Node node) {
		if (node == null) {
			return new Nodes(doc(), 0);
		}
		return new Nodes(doc(), node);
	}
	
	public Nodes convert(Node... list) {
		return convert(Arrays.asList((Node[])list));
	}
	
	public Nodes convert(Collection<Node> list) {
		if (list == null || list.isEmpty()) {
			return new Nodes(doc(), 0);
		}
		
		Nodes nodes = new Nodes(doc(), list.size());
		nodes.addAll(list);
		Nodes.unique(nodes);
		return nodes;
	}
	
	public Nodes convert(NodeList list) {
		if (list == null || list.getLength() == 0) {
			return new Nodes(doc(), 0);
		}
		
		Nodes nodes = new Nodes(doc(), list.getLength());
		for (int i = 0; i < list.getLength(); i++) {
			nodes.add(list.item(i));
		}
		Nodes.unique(nodes);
		return nodes;
	}
	
	public Nodes parse(String text) {
		if (text == null || text.isEmpty()) {
			return new Nodes(doc(), 0);
		}
		
		try {
			DocumentBuilder db = getDocumentBuilder();
			Document ndoc = db.parse(new InputSource(new StringReader("<x>" + text + "</x>")));
			NodeList list = doc.importNode(ndoc.getDocumentElement(), true).getChildNodes();
			
			Nodes nodes = new Nodes(doc(), list.getLength());
			for (int i = 0; i < list.getLength(); i++) {
				nodes.add(list.item(i));
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
	
	XPathExpression compileXPath(String xpath) {
		XPath xpc = XPathFactory.newInstance().newXPath();
		if (nsContext != null) xpc.setNamespaceContext(nsContext);
		try {
			return xpc.compile(xpath);
		} catch (XPathExpressionException e) {
			String message;
			Throwable current = e;
			do {
				message = current.getMessage();
				if (message != null && !message.isEmpty()) break;
			} while ((current = e.getCause()) != null);
			
			StringBuilder sb = new StringBuilder();
			sb.append("XPath:");
			sb.append(message != null ? message : "Unexpected error.");
			
			throw new IllegalArgumentException(sb.toString(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	<T> T evaluate(XPathExpression expr, Node node, Class<T> cls) {
		try {
			if (cls.equals(Nodes.class)) {
				NodeList list = (NodeList)expr.evaluate(node, XPathConstants.NODESET);
				return (T)((list != null) ? convert(list) : null);
			} else if (cls.equals(NodeList.class)) {
				return (T)expr.evaluate(node, XPathConstants.NODESET);
			} else if (cls.equals(Node.class)) {
				return (T)expr.evaluate(node, XPathConstants.NODE);
			} else if (cls.equals(String.class)) {
				return (T)expr.evaluate(node, XPathConstants.STRING);
			} else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
				return (T)expr.evaluate(node, XPathConstants.BOOLEAN);
			} else if (cls.equals(Number.class) || cls.equals(BigDecimal.class)) {
				Double num = (Double)expr.evaluate(node, XPathConstants.NUMBER);
				return (T)((num != null) ? new BigDecimal((double)num) : null);
			} else if (cls.equals(double.class) || cls.equals(Double.class)) {
				Double num = (Double)expr.evaluate(node, XPathConstants.NUMBER);
				return (T)((num != null) ? num : cls.equals(double.class) ? 0.0 : null);
			} else if (cls.equals(float.class) || cls.equals(Float.class)) {
				Double num = (Double)expr.evaluate(node, XPathConstants.NUMBER);
				return (T)(Float)((num != null) ? ((Double)num).floatValue() : cls.equals(float.class) ? 0.0F : null);
			} else if (cls.equals(long.class) || cls.equals(Long.class)) {
				Double num = (Double)expr.evaluate(node, XPathConstants.NUMBER);
				return (T)(Long)((num != null) ? ((Double)num).longValue() : cls.equals(long.class) ? 0L : null);
			} else if (cls.equals(int.class) || cls.equals(Integer.class)) {
				Double num = (Double)expr.evaluate(node, XPathConstants.NUMBER);
				return (T)(Integer)((num != null) ? ((Double)num).intValue() : cls.equals(int.class) ? 0 : null);
			} else if (cls.equals(short.class) || cls.equals(Short.class)) {
				Double num = (Double)expr.evaluate(node, XPathConstants.NUMBER);
				return (T)(Short)((num != null) ? ((Double)num).shortValue() : cls.equals(short.class) ? (short)0 : null);
			} else {
				throw new UnsupportedOperationException("Unsupported Convert class: " + cls);
			}
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	@Override
	public String toString() {
		XMLWriter serializer = new XMLWriter();
		serializer.setShowXMLDeclaration(false);
		StringWriter writer = new StringWriter();
		try {
			serializer.writeTo(writer, doc);
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
}
