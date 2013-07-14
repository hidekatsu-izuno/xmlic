package net.arnx.xmlic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XML {
	public static XML load(File file) throws IOException {
		return load(file.toURI());
	}
	
	public static XML load(URI uri) throws IOException {
		return load(new InputSource(uri.normalize().toASCIIString()));
	}
	
	public static XML load(URL url) throws IOException {
		try {
			return load(new InputSource(url.toURI().normalize().toASCIIString()));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static XML load(InputStream in) throws IOException {
		return load(new InputSource(in));
	}
	
	public static XML load(Reader reader) throws IOException {
		return load(new InputSource(reader));
	}
	
	private static XML load(InputSource is) throws IOException {
		try {
			DocumentBuilder db = getDocumentBuilder();
			db.setEntityResolver(new ResourceResolver());

			NamespaceContextImpl nci = new NamespaceContextImpl();
			XML xml = new XML(db.parse(is), nci);
			
			XPathExpression expr = xml.compileXPath("//namespace::*");
			NodeList list = xml.evaluateAsNodeList(expr, xml.doc);
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				String prefix = node.getLocalName();
				if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
					prefix = XMLConstants.DEFAULT_NS_PREFIX;
				}
				
				nci.addNamespace(prefix, node.getNodeValue());
			}
			
			return  xml;
		} catch (SAXException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	final Document doc;
	final NamespaceContextImpl nsResolver;
	final ResourceResolver resolver;
	
	public XML() {
		this(Collections.<String, String>emptyMap());
	}
	
	public XML(Map<String, String> namespaces) {
		this(createDocumentNS(), namespaces);
	}
	
	public XML(Document doc, Map<String, String> namespaces) {
		this(doc, new NamespaceContextImpl(namespaces));
	}
	
	private XML(Document doc, NamespaceContextImpl resolver) {
		this.doc = doc;
		this.nsResolver = resolver;
		this.resolver = new ResourceResolver();
	}
	
	private static Document createDocumentNS() {
		DocumentBuilder db = getDocumentBuilder();
		return db.newDocument();
	}
	
	public Nodes document() {
		Nodes nodes = new Nodes(this, null, 1);
		nodes.add(doc);
		return nodes;
	}
	
	public Nodes convert(Node node) {
		if (node == null) {
			return new Nodes(document(), 0);
		}
		
		if (node.getOwnerDocument() != doc) {
			node = doc.importNode(node, true);
		}
		return new Nodes(document(), node);
	}
	
	public Nodes convert(Node... list) {
		return convert(Arrays.asList((Node[])list));
	}
	
	public Nodes convert(Collection<Node> list) {
		if (list == null || list.isEmpty()) {
			return new Nodes(document(), 0);
		}
		
		Nodes nodes = new Nodes(document(), list.size());
		nodes.addAll(list);
		Nodes.unique(nodes);
		return nodes;
	}
	
	public Nodes convert(NodeList list) {
		if (list == null || list.getLength() == 0) {
			return new Nodes(document(), 0);
		}
		
		Nodes nodes = new Nodes(document(), list.getLength());
		for (int i = 0; i < list.getLength(); i++) {
			nodes.add(list.item(i));
		}
		Nodes.unique(nodes);
		return nodes;
	}
	
	public Nodes parse(String text) {
		if (text == null || text.isEmpty()) {
			return new Nodes(document(), 0);
		}
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setCoalescing(true);
		dbf.setNamespaceAware(true);
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new ResourceResolver());
			Document ndoc = db.parse(new InputSource(new StringReader("<x>" + text + "</x>")));
			NodeList list = doc.importNode(ndoc.getDocumentElement(), true).getChildNodes();
			
			Nodes nodes = new Nodes(document(), list.getLength());
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
		return document().find(xpath);
	}
	
	public Nodes remove(String xpath) {
		return document().remove(xpath);
	}
	
	public Nodes removeNamespace(String namespaceURI) {
		if (namespaceURI == null) namespaceURI = ""; 
		Nodes root = document();
		root.find("//*[namespace-uri()=" + escape(namespaceURI) + "]").namespaceURI(null);
		XPathExpression expr = compileXPath("//namespace::*[self::node()=" + escape(namespaceURI) + "]");
		NodeList list = evaluateAsNodeList(expr, doc);
		for (int i = 0; i < list.getLength(); i++) {
			Attr attr = (Attr)list.item(i);
			Element elem = attr.getOwnerElement();
			if (elem != null) elem.removeAttributeNode(attr);
		}
		return root;
	}
	
	public XML clone() {
		return new XML((Document)doc.cloneNode(true), nsResolver);
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
		return new XML((Document)result.getNode(), nsResolver);
	}
	
	public void writeTo(File file, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		writeTo(new FileOutputStream(file), declaration, encoding, prettyPrint);
	}
	
	public void writeTo(OutputStream out, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		LSOutput output = createLSOutput(doc);
		output.setByteStream(out);
		if (encoding != null) output.setEncoding(encoding);
		writeTo(output, declaration, encoding, prettyPrint);
	}
	
	public void writeTo(Writer writer, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		LSOutput output = createLSOutput(doc);
		output.setCharacterStream(writer);
		if (encoding != null) output.setEncoding(encoding);
		writeTo(output, declaration, encoding, prettyPrint);
	}
	
	private void writeTo(LSOutput output, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		LSSerializer serializer = createLSSerializer(doc);
		DOMConfiguration conf = serializer.getDomConfig();
		conf.setParameter("format-pretty-print", prettyPrint);
		conf.setParameter("xml-declaration", declaration);
		if (encoding != null) output.setEncoding(encoding);
		
		serializer.write(doc, output);
	}
	
	private static DocumentBuilder getDocumentBuilder() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setCoalescing(true);
		dbf.setNamespaceAware(true);
		try {
			return dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	XPathExpression compileXPath(String xpath) {
		XPath xpc = XPathFactory.newInstance().newXPath();
		xpc.setNamespaceContext(nsResolver);
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
	
	static class ResourceResolver implements EntityResolver, URIResolver {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			if (systemId == null) return null;
			
			try {
				URI uri = toURI(publicId, systemId);
				return new InputSource(uri.toURL().openConnection().getInputStream());
			} catch (MalformedURLException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}
		
		@Override
		public Source resolve(String href, String base) throws TransformerException {
			if (href == null) return null;
			
			try {
				URI uri = toURI(base, href);
				return new StreamSource(uri.toURL().openConnection().getInputStream(),
						uri.normalize().toASCIIString());
			} catch (MalformedURLException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}
		
		private static URI toURI(String base, String href) {
			if (base != null && !base.isEmpty()) {
				return URI.create(base).resolve(href);
			} else {
				return URI.create(href);
			}
		}
	}
	
	@Override
	public String toString() {
		LSSerializer serializer = createLSSerializer(doc);
		DOMConfiguration conf = serializer.getDomConfig();
		conf.setParameter("format-pretty-print", false);
		conf.setParameter("xml-declaration", false);
		StringBuilder sb = new StringBuilder();
		sb.append(serializer.writeToString(doc));
		return sb.toString();
	}
	
	static String escape(String xpath) {
		if (xpath.contains("'")) {
			return "concat('" + xpath.replace("'", "',\"'\",") + "')";
		} else {
			return "'" + xpath + "'";
		}
	}
	
	static DOMImplementationLS getDOMImplementationLS(Document doc) {
		return (DOMImplementationLS)doc.getImplementation().getFeature("+LS", "3.0");
	}
	
	static LSSerializer createLSSerializer(Document doc) {
		return getDOMImplementationLS(doc).createLSSerializer();
	}
	
	static LSOutput createLSOutput(Document doc) {
		return getDOMImplementationLS(doc).createLSOutput();
	}
}
