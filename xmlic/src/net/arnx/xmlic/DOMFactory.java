package net.arnx.xmlic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathExpression;
import org.w3c.dom.xpath.XPathNSResolver;

public class DOMFactory {
	private static final ThreadLocal<DOMFactory> THREAD_LOCAL = new ThreadLocal<DOMFactory>() {
		@Override
		protected DOMFactory initialValue() {
			return new DOMFactory();
		}
	};
	
	private static DOMFactory getInstance() {
		return THREAD_LOCAL.get();
	}
	
	private DOMImplementation imp;
	private DOMImplementationLS ls;
	private XPathEvaluator xpe;
	private TransformerFactory tf;
	private boolean sortDetachedSupported;
	
	private DOMFactory() {
		DOMImplementationRegistry registry;
		try {
			registry = DOMImplementationRegistry.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		imp = (DOMImplementation)registry.getDOMImplementation("XML 3.0 +LS 3.0 +XPath 3.0");
		if (imp == null) {
			throw new IllegalStateException("this environment isn't supported DOM level3, Load & Save API and XPath API.");
		}
		ls = (DOMImplementationLS)imp.getFeature("+LS", "3.0");
		xpe = (XPathEvaluator)imp.getFeature("+XPath", "3.0");
		tf = TransformerFactory.newInstance();
		
		Document doc = imp.createDocument(null, "x", null);
		Element e1 = doc.createElement("e1");
		Element e2 = doc.createElement("e2");
		sortDetachedSupported = ((e1.compareDocumentPosition(e2) & Node.DOCUMENT_POSITION_DISCONNECTED) != 0);
	}
	
	private static DOMImplementation getDOMImplementation() {
		return getInstance().imp;
	}
	
	private static DOMImplementationLS getDOMimplementationLS() {
		return getInstance().ls;
	}
	
	private static XPathEvaluator getXPathEvaluator() {
		return getInstance().xpe;
	}
	
	private static TransformerFactory getTransformerFactory() {
		return getInstance().tf;
	}
	
	public static Document createDocument(String namespaceURI, String localName) {
		return getDOMImplementation().createDocument(namespaceURI, localName, null);
	}
	
	public static boolean isSortDetachedSupported() {
		return getInstance().sortDetachedSupported;
	}
	
	public static LSInput createLSInput(URI uri) throws IOException {
		LSInput input = getDOMimplementationLS().createLSInput();
		URI dir = uri.resolve(".").normalize();
		input.setBaseURI(dir.toASCIIString());
		input.setSystemId(uri.relativize(dir).toASCIIString());
		return input;
	}
	
	public static LSInput createLSInput(URI uri, String text) {
		LSInput input = getDOMimplementationLS().createLSInput();
		URI dir = uri.resolve(".").normalize();
		input.setBaseURI(dir.toASCIIString());
		input.setSystemId(uri.relativize(dir).toASCIIString());
		input.setStringData(text);
		return input;
	}
	
	public static LSInput createLSInput(URI uri, InputStream in) {
		LSInput input = getDOMimplementationLS().createLSInput();
		URI dir = uri.resolve(".").normalize();
		input.setBaseURI(dir.toASCIIString());
		input.setSystemId(uri.relativize(dir).toASCIIString());
		input.setByteStream(in);
		return input;
	}
	
	public static LSInput createLSInput(URI uri, Reader reader) {
		LSInput input = getDOMimplementationLS().createLSInput();
		URI dir = uri.resolve(".").normalize();
		input.setBaseURI(dir.toASCIIString());
		input.setSystemId(uri.relativize(dir).toASCIIString());
		input.setCharacterStream(reader);
		return input;
	}
	
	public static LSOutput createLSOutput(OutputStream out) {
		LSOutput output = getDOMimplementationLS().createLSOutput();
		output.setByteStream(out);
		return output;
	}
	
	public static LSOutput createLSOutput(OutputStream out, String encoding) {
		LSOutput output = getDOMimplementationLS().createLSOutput();
		output.setByteStream(out);
		output.setEncoding(encoding);
		return output;
	}
	
	public static LSOutput createLSOutput(Writer writer) {
		LSOutput output = getDOMimplementationLS().createLSOutput();
		output.setCharacterStream(writer);
		return output;
	}
	
	public static LSParser createLSParser() {
		return getDOMimplementationLS().createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
	}
	
	public static LSSerializer createLSSerializer() {
		return getDOMimplementationLS().createLSSerializer();
	}
	
	public static XPathExpression createXPathExpression(String xpath, XPathNSResolver resolver) {
		return getXPathEvaluator().createExpression(xpath, resolver);
	}
	
	public static XPathNSResolver createXPathNSResolver(Node node) {
		return getXPathEvaluator().createNSResolver(node);
	}
	
	public static XPathNSResolver createXPathNSResolver(Map<String, String> map) {
		return new XPathNSResolverImpl(map);
	}
	
	public static Transformer createTramsformer(URI uri) throws TransformerConfigurationException {
		return getTransformerFactory().newTransformer(new StreamSource(uri.normalize().toASCIIString()));
	}
	
	public static Transformer createTramsformer(File file) throws TransformerConfigurationException {
		return getTransformerFactory().newTransformer(new StreamSource(file));
	}
	
	public static Transformer createTramsformer(URI uri, InputStream in) throws TransformerConfigurationException {
		return getTransformerFactory().newTransformer(new StreamSource(in, uri.normalize().toASCIIString()));
	}
	
	public static Transformer createTramsformer(URI uri, Reader reader) throws TransformerConfigurationException {
		return getTransformerFactory().newTransformer(new StreamSource(reader, uri.normalize().toASCIIString()));
	}
	
	public static Transformer createTransformer(Document doc) throws TransformerConfigurationException {
		Source src = getTransformerFactory().getAssociatedStylesheet(new DOMSource(doc), null, null, null);
		return (src != null) ? getTransformerFactory().newTransformer(src) : null;
	}
	
	private static class XPathNSResolverImpl implements XPathNSResolver {
		private ConcurrentMap<String, String> map = new ConcurrentHashMap<String, String>();
		
		public XPathNSResolverImpl(Map<String, String> namespaces) {
			map = new ConcurrentHashMap<String, String>(namespaces);
			map.put(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);
			map.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
		}
		
		@Override
		public String lookupNamespaceURI(String prefix) {
			return map.get(prefix);
		}
	}
}
