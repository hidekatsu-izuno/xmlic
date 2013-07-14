package net.arnx.xmlic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.xpath.XPathNSResolver;

public class XML {
	private static final URI NULL_URI = URI.create("");
	
	public static XML load(URL url) throws IOException {
		try {
			return load(url.toURI(), url.openConnection().getInputStream());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static XML load(File file) throws IOException {
		return load(file.toURI(), new FileInputStream(file));
	}
	
	public static XML load(InputStream in) throws IOException {
		return load(DOMFactory.createLSInput(NULL_URI, in));
	}
	
	public static XML load(Reader reader) throws IOException {
		return load(DOMFactory.createLSInput(NULL_URI, reader));
	}
	
	public static XML load(URI uri, InputStream in) throws IOException {
		return load(DOMFactory.createLSInput(uri, in));
	}
	
	public static XML load(URI uri, Reader reader) throws IOException {
		return load(DOMFactory.createLSInput(uri, reader));
	}
	
	private static XML load(LSInput input) throws IOException {
		LSParser parser = DOMFactory.createLSParser();
		DOMConfiguration config = parser.getDomConfig();
		
		config.setParameter("cdata-sections", false);
		config.setParameter("entities", false);
		config.setParameter("resource-resolver", new ResourceResolver());
		
		try {
			return new XML(parser.parse(input));
		} catch (RuntimeException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			} else {
				throw e;
			}
		}
	}
	
	final Document doc;
	final XPathNSResolver nsResolver;
	
	public XML(String name) {
		this(name, Collections.<String, String>emptyMap());
	}
	
	public XML(String name, Map<String, String> namespaces) {
		this(name, DOMFactory.createXPathNSResolver(namespaces));
	}
	
	public XML(String name, XPathNSResolver nsResolver) {
		this(createDocumentNS(name, nsResolver), nsResolver);
	}
	
	private static Document createDocumentNS(String name, XPathNSResolver nsResolver) {
		int index = name.indexOf(':');
		String uri = null;
		String localName;
		if (index != -1) {
			if (index == 0 || index + 1 >= name.length()) {
				localName = name;
			} else {
				localName = name.substring(index + 1);
				if (!localName.isEmpty()) {
					uri = nsResolver.lookupNamespaceURI(name.substring(0, index));
					if (uri == null) localName = name;
				} else {
					localName = name;
				}
			}
		} else {
			localName = name;
		}
		
		return DOMFactory.createDocument(uri, localName);
	}
	
	public XML(Document doc, Map<String, String> namespaces) {
		this(doc, DOMFactory.createXPathNSResolver(namespaces));
	}
	
	public XML(Document doc) {
		this(doc, DOMFactory.createXPathNSResolver(doc));
	}
	
	public XML(Document doc, XPathNSResolver nsResolver) {
		this.doc = doc;
		this.nsResolver = nsResolver;
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
		
		LSParser parser = DOMFactory.createLSParser();
		DOMConfiguration config = parser.getDomConfig();
		config.setParameter("cdata-sections", false);
		config.setParameter("entities", false);
		
		Document newDoc = parser.parse(DOMFactory.createLSInput(NULL_URI, "<x>" + text + "</x>"));
		NodeList list = doc.importNode(newDoc.getDocumentElement(), true).getChildNodes();

		Nodes nodes = new Nodes(document(), list.getLength());
		for (int i = 0; i < list.getLength(); i++) {
			nodes.add(list.item(i));
		}
		return nodes;
	}
	
	public XML clone() {
		return new XML((Document)doc.cloneNode(true), nsResolver);
	}
	
	public Nodes find(String xpath) {
		return document().find(xpath);
	}
	
	public XML transform(Transformer t) throws TransformerException {
		t.setURIResolver(new ResourceResolver());
		DOMResult result = new DOMResult();
		t.transform(new DOMSource(doc), result);
		return new XML((Document)result.getNode());
	}
	
	public void writeTo(File file, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		writeTo(new FileOutputStream(file), declaration, encoding, prettyPrint);
	}
	
	public void writeTo(OutputStream out, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		LSOutput output = DOMFactory.createLSOutput(out);
		writeTo(output, declaration, encoding, prettyPrint);
	}
	
	public void writeTo(Writer writer, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		LSOutput output = DOMFactory.createLSOutput(writer);
		writeTo(output, declaration, encoding, prettyPrint);
	}
	
	private void writeTo(LSOutput output, boolean declaration, String encoding, boolean prettyPrint) throws IOException {
		if (encoding != null) output.setEncoding(encoding);
		
		LSSerializer serializer = DOMFactory.createLSSerializer();
		DOMConfiguration conf = serializer.getDomConfig();
		conf.setParameter("format-pretty-print", prettyPrint);
		conf.setParameter("xml-declaration", declaration);
		if (encoding != null) output.setEncoding(encoding);
		
		serializer.write(doc, output);
	}
	
	private static class ResourceResolver implements LSResourceResolver, URIResolver {
		@Override
		public LSInput resolveResource(String type, String namespaceURI, 
				String publicId, String systemId, String baseURI) {
			if (systemId == null) return null;
			
			try {
				URI uri = toURI(baseURI, systemId);
				return DOMFactory.createLSInput(uri, uri.toURL().openConnection().getInputStream());
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
		LSSerializer serializer = DOMFactory.createLSSerializer();
		DOMConfiguration conf = serializer.getDomConfig();
		conf.setParameter("format-pretty-print", false);
		conf.setParameter("xml-declaration", false);
		StringBuilder sb = new StringBuilder();
		sb.append(serializer.writeToString(doc));
		return sb.toString();
	}
}
