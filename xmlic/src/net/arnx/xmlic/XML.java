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
		XMLParser parser = new XMLParser();
		return new XML(parser.parse(file.toURI()));
	}
	
	public static XML load(URI uri) throws IOException {
		XMLParser parser = new XMLParser();
		return new XML(parser.parse(uri));
	}
	
	public static XML load(URL url) throws IOException {
		try {
			XMLParser parser = new XMLParser();
			return new XML(parser.parse(url.toURI()));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static XML load(InputStream in) throws IOException {
		XMLParser parser = new XMLParser();
		return new XML(parser.parse(in));
	}
	
	public static XML load(Reader reader) throws IOException {
		XMLParser parser = new XMLParser();
		return new XML(parser.parse(reader));
	}
	
	final Document doc;
	final ResourceResolver resolver;
	final NamespaceContext context;
	
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
		this.resolver = new ResourceResolver();
		
		if (namespaces == null) {
			NamespaceContextImpl context = new NamespaceContextImpl();
			XPathExpression expr = compileXPath("//namespace::*");
			NodeList list = evaluateAsNodeList(expr, doc);
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				String prefix = node.getLocalName();
				if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
					prefix = XMLConstants.DEFAULT_NS_PREFIX;
				}
				
				context.addNamespace(prefix, node.getNodeValue());
			}
			this.context = context;
		} else {
			this.context = new NamespaceContextImpl(namespaces);
		}
	}
	
	XML(Document doc, ResourceResolver resolver, NamespaceContext context) {
		this.doc = doc;
		this.resolver = resolver;
		this.context = context;
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
		
		if (node.getOwnerDocument() != doc) {
			node = doc.importNode(node, true);
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
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setCoalescing(true);
		dbf.setNamespaceAware(true);
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new ResourceResolver());
			Document ndoc = db.parse(new InputSource(new StringReader("<x>" + text + "</x>")));
			NodeList list = doc.importNode(ndoc.getDocumentElement(), true).getChildNodes();
			
			Nodes nodes = new Nodes(doc(), list.getLength());
			for (int i = 0; i < list.getLength(); i++) {
				nodes.add(list.item(i));
			}
			return nodes;
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public Nodes find(String xpath) {
		return doc().find(xpath);
	}
	
	public Nodes remove(String xpath) {
		return doc().remove(xpath);
	}
	
	public XML clone() {
		return new XML((Document)doc.cloneNode(true), resolver, context);
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
		return new XML((Document)result.getNode(), resolver, context);
	}
	
	public void writeTo(File file) throws IOException {
		writeTo(new FileOutputStream(file));
	}
	
	public void writeTo(OutputStream out) throws IOException {
		XMLSerializer serializer = new XMLSerializer();
		serializer.setEncoding("UTF-8");
	
		try {
			serializer.serialize(doc, out);
		} finally {
			out.close();
		}
	}
	
	public void writeTo(Writer writer) throws IOException {
		XMLSerializer serializer = new XMLSerializer();
		
		try {
			serializer.serialize(doc, writer);
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
			return dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	XPathExpression compileXPath(String xpath) {
		XPath xpc = XPathFactory.newInstance().newXPath();
		if (context != null) xpc.setNamespaceContext(context);
		try {
			return xpc.compile(xpath);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	boolean evaluteAsBoolean(XPathExpression expr, Node node) {
		try {
			return (Boolean)expr.evaluate(node, XPathConstants.BOOLEAN);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	NodeList evaluateAsNodeList(XPathExpression expr, Node node) {
		try {
			return (NodeList)expr.evaluate(node, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	@Override
	public String toString() {
		XMLSerializer serializer = new XMLSerializer();
		serializer.setXMLDeclarationVisible(false);
		StringWriter writer = new StringWriter();
		try {
			serializer.serialize(doc, writer);
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
