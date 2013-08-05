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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.util.XMLContext;
import net.arnx.xmlic.internal.util.XMLContext.Key;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XML implements Serializable {
	private static final long serialVersionUID = 1L;

	public static XML load(File file) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(file.toURI()));
	}
	
	public static XML load(URI uri) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(uri));
	}
	
	public static XML load(URL url) throws IOException {
		try {
			XMLLoader loader = new XMLLoader();
			return new XML(loader.load(url.toURI()));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static XML load(InputStream in) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(in));
	}
	
	public static XML load(Reader reader) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(reader));
	}
	
	final Document doc;
	final XMLContext xmlContext;
	
	public XML() {
		this.xmlContext = new XMLContext();
		this.doc = XMLContext.getDocumentBuilder().newDocument();
	}
	
	public XML(String text) {
		this.xmlContext = new XMLContext();
		try {
			this.doc = XMLContext.getDocumentBuilder().parse(new InputSource(new StringReader(text)));
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public XML(Document doc) {
		this.xmlContext = new XMLContext();
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
	}
	
	XML(Document doc, XMLContext nsContext) {
		this.doc = doc;
		this.xmlContext = nsContext;
	}
	
	public Document get() {
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
	
	public void addKey(String name, String match, String use) {
		xmlContext.addKey(name, new Key(match, use));
	}
	
	public void removeKey(String name) {
		xmlContext.removeKey(name);
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
		return xmlContext.compileXPath(text);
	}
	
	<T> T evaluate(Object expr, Node node, Class<T> cls) {
		return xmlContext.evaluate(this, (XPath)expr, node, cls);
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
}
