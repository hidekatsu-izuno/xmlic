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
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.util.NodeMatcher;
import net.arnx.xmlic.internal.util.XmlicContext;
import net.arnx.xmlic.internal.util.XmlicContext.Key;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * XML class is for managing XML Document and namespace settings.
 */
public class XML implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final String[] ESCAPE_CHARS = new String[128];
	
	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = "&#";
		}
		ESCAPE_CHARS['&'] = "&amp;";
		ESCAPE_CHARS['<'] = "&lt;";
		ESCAPE_CHARS['>'] = "&gt;";
		ESCAPE_CHARS['\''] = "&apos;";
		ESCAPE_CHARS['"'] = "&quot;";
		ESCAPE_CHARS[0x7F] = "&#";
	}
	
	/**
	 * Load an XML document from a input file.
	 * 
	 * @param file a input file.
	 * @return a new XML instance.
	 * @throws IOException if I/O error occurred.
	 */
	public static XML load(File file) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(file.toURI()));
	}
	
	/**
	 * Load an XML document from a URI.
	 * 
	 * @param uri a URI.
	 * @return a new XML instance.
	 * @throws IOException if I/O error occurred.
	 */
	public static XML load(URI uri) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(uri));
	}
	
	/**
	 * Load an XML document from a URL.
	 * 
	 * @param url a URL.
	 * @return a new XML instance.
	 * @throws IOException if I/O error occurred.
	 */
	public static XML load(URL url) throws IOException {
		try {
			XMLLoader loader = new XMLLoader();
			return new XML(loader.load(url.toURI()));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Load an XML document from a binary input stream.
	 * 
	 * @param in a binary input stream.
	 * @return a new XML instance.
	 * @throws IOException if I/O error occurred.
	 */
	public static XML load(InputStream in) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(in));
	}
	
	/**
	 * Load an XML document from a character input stream.
	 * 
	 * @param reader a character input stream.
	 * @return a new XML instance.
	 * @throws IOException if I/O error occurred.
	 */
	public static XML load(Reader reader) throws IOException {
		XMLLoader loader = new XMLLoader();
		return new XML(loader.load(reader));
	}
	
	final Document doc;
	final XmlicContext xmlContext;
	
	/**
	 * Construct a new XML instance by the empty Document.
	 */
	public XML() {
		this.xmlContext = new XmlicContext();
		this.doc = XmlicContext.getDocumentBuilder().newDocument();
	}
	
	/**
	 * Construct a new XML instance by parsing the specified text.
	 * 
	 * @param text an XML text.
	 */
	public XML(String text) {
		this.xmlContext = new XmlicContext();
		try {
			this.doc = XmlicContext.getDocumentBuilder().parse(new InputSource(new StringReader(text)));
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	/**
	 * Construct a new XML instance by the specified {@link org.w3c.dom.Document} instance.
	 * 
	 * @param doc an instance of Document.
	 */
	public XML(Document doc) {
		this.xmlContext = new XmlicContext();
		this.doc = doc;
		
		Element root = doc.getDocumentElement();
		if (root != null) {
			NamedNodeMap attrs = root.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				Node node = attrs.item(i);
				if (!(node instanceof Attr)) continue;
				
				if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(node.getNamespaceURI())) {
					String prefix = node.getPrefix();
					if (prefix == null || prefix.isEmpty()) {
						prefix = XMLConstants.DEFAULT_NS_PREFIX;
					} else {
						prefix = node.getLocalName();
					}
					xmlContext.addNamespace(prefix, node.getNodeValue());
				}
			}
		}
	}
	
	XML(XmlicContext nsContext, Document doc) {
		this.xmlContext = nsContext;
		this.doc = doc;
	}
	
	/**
	 * Get the current {@link org.w3c.dom.Document}.
	 * 
	 * @return the current Document.
	 */
	public Document get() {
		return doc;
	}
	
	/**
	 * Gets the encoding of this document.
	 * 
	 * @return the encoding
	 */
	public String encoding() {
		String encoding = doc.getXmlEncoding();
		if (encoding == null) {
			encoding = doc.getInputEncoding();
		}
		return encoding;
	}
	
	/**
	 * Adds a namespace mapping for using XPath expression.
	 * 
	 * @param prefix a namespace prefix
	 * @param uri a namespace URI
	 * @return a reference to this object
	 */
	public XML addNamespaceMapping(String prefix, String uri) {
		xmlContext.addNamespace(prefix, uri);
		return this;
	}
	
	/**
	 * Gets the namespace mapping for a namespace prefix
	 * 
	 * @param prefix a namespace prefix
	 * @return the namespace URI for a sprcified prefix 
	 */
	public String getNamespaceMapping(String prefix) {
		return xmlContext.getNamespaceURI(prefix);
	}
	
	public Map<String, String> getNamespaceMappings() {
		Map<String, String> map = new HashMap<String, String>();
		for (String prefix : xmlContext.getPrefixes()) {
			map.put(prefix, xmlContext.getNamespaceURI(prefix));
		}
		return map;
	}
	
	/**
	 * Removes a namespace mapping for using XPath expression.
	 * 
	 * @param prefix a namespace prefix
	 * @return a reference to this object
	 */
	public XML removeNamespaceMapping(String prefix) {
		xmlContext.removeNamespace(prefix);
		return this;
	}
	
	public XML addKey(String name, String match, String use) {
		xmlContext.addKey(name, new Key(match, use));
		return this;
	}
	
	public XML removeKey(String name) {
		xmlContext.removeKey(name);
		return this;
	}
	
	/**
	 * Gets a Nodes instance that has document node.
	 * 
	 * @return a Nodes instance that has document node
	 */
	public Nodes doc() {
		Nodes nodes = new Nodes(this, null, 1);
		nodes.add(doc);
		return nodes;
	}
	
	/**
	 * Gets a Nodes instance that has the root element of this document.
	 * 
	 * @return a Nodes instance that has the root element of this document
	 */
	public Nodes root() {
		Nodes nodes = new Nodes(this, doc(), 1);
		nodes.add(doc.getDocumentElement());
		return nodes;
	}
	
	/**
	 * Sets the root element of this document.
	 * 
	 * @return a Nodes instance that has the root element of this document
	 */
	public Nodes root(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) {
			return doc().empty();
		} else {
			return doc().append(nodes);
		}
	}
	
	/**
	 * Build new Nodes instance for specified XML contents.
	 * 
	 * @param xml XML contents
	 * @return new Nodes instance
	 */
	public Nodes parse(String xml) {
		if (xml == null || xml.isEmpty()) {
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
		sb.append(">").append(xml).append("</x>");
		
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
			factory.setProperty(XMLInputFactory.IS_COALESCING, false);
			factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			XMLStreamReader reader = factory.createXMLStreamReader(doc.getBaseURI(), new StringReader(sb.toString()));
			
			Element root = null;
			Element current = null;
			while (reader.hasNext()) {
				switch (reader.next()) {
				case XMLStreamConstants.START_ELEMENT: {

					Element elem = doc.createElementNS(
							reader.getNamespaceURI(), 
							reader.getLocalName());
					
					for (int i = 0; i < reader.getAttributeCount(); i++) {
						elem.setAttributeNS(
								reader.getAttributeNamespace(i), 
								reader.getAttributeLocalName(i), 
								reader.getAttributeValue(i));
					}
					
					if (root == null) {
						root = elem;
					} else {
						current.appendChild(elem);
					}
					current = elem;
					break;
				}
				case XMLStreamConstants.END_ELEMENT: {
					current = (Element)current.getParentNode();						
					break;
				}
				case XMLStreamConstants.PROCESSING_INSTRUCTION: {
					String target = reader.getPITarget();
					String data = reader.getPIData();
					current.appendChild(doc.createProcessingInstruction(target, data));
					break;
				}
				case XMLStreamConstants.COMMENT: {
					String data = reader.getText();
					current.appendChild(doc.createComment(data));
					break;
				}
				case XMLStreamConstants.CDATA: {
					String data = reader.getText();
					current.appendChild(doc.createCDATASection(data));
					break;
				}
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.SPACE: {
					String data = reader.getText();
					current.appendChild(doc.createTextNode(data));
					break;
				}
				}
			}
			
			NodeList list = root.getChildNodes();
			Nodes nodes = new Nodes(this, null, list.getLength());
			for (int i = 0; i < list.getLength(); i++) {
				nodes.add(list.item(i));
			}
			while (root.hasChildNodes()) {
				root.removeChild(root.getLastChild());
			}
			return nodes;
		} catch (XMLStreamException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Evaluate a specified XPath expression at this document.
	 * And gets as a specified type.
	 * This method is same to doc().evaluate(xpath, cls). 
	 * 
	 * @param xpath a XPath expression
	 * @param cls a result type
	 * @return a result value.
	 */
	public <T> T evaluate(String xpath, Class<T> cls) {
		return doc().evaluate(xpath, cls);
	}
	
	/**
	 * Selects nodes by a specified XPath expression.
	 * This method is same to doc().select(xpath). 
	 * 
	 * @param xpath a XPath expression
	 * @return a set of nodes
	 */
	public Nodes select(String xpath) {
		return doc().select(xpath);
	}
	
	/**
	 * Finds elements matched a specified XPath pattern.
	 * This method is same to doc().find(pattern). 
	 * 
	 * @param pattern a XPath pattern
	 * @return a set of elements
	 */
	public Nodes find(String pattern) {
		return doc().find(pattern);
	}
	
	/**
	 * Gets the set of child nodes for current document.
	 * This method is same to doc().contents(). 
	 * 
	 * @return the set of child nodes
	 */
	public Nodes contents() {
		return doc().contents();
	}
	
	/**
	 * Gets the filtered set of child nodes for current document.
	 * This method is same to doc().contents(pattern). 
	 * 
	 * @return the filtered set of child nodes
	 */
	public Nodes contents(String pattern) {
		return doc().contents(pattern);
	}
	
	/**
	 * Traverses all nodes that matched a specified pattern.
	 * This method is same to doc().traverse(pattern, func). 
	 * 
	 * @param pattern a pattern
	 * @param func a visitor function
	 * @return a Nodes instance that has document node
	 */
	public Nodes traverse(String pattern, Visitor<Nodes> func) {
		return doc().traverse(pattern, func);
	}
	
	/**
	 * Removes all child nodes of the set of current nodes.
	 * This method is same to doc().empty(). 
	 * 
	 * @return a Nodes instance that has document node
	 */
	public Nodes empty() {
		return doc().empty();
	}
	
	/**
	 * Removes the filtered set of all nodes.
	 * This method is same to doc().remove(pattern). 
	 * 
	 * @param pattern a pattern
	 * @return a Nodes instance that has document node
	 */
	public Nodes remove(String pattern) {
		return doc().remove(pattern);
	}
	
	/**
	 * Gets a concatenated text of this document.
	 * This method is same to doc().text(). 
	 * 
	 * @return a concatenated text of this document.
	 */
	public String text() {
		return doc().text();
	}
	
	/**
	 * Get a XML text for this document.
	 * This method is same to doc().xml(). 
	 * 
	 * @return a XML text for this document
	 */
	public String xml() {
		return doc().xml();
	}
	
	/**
	 * Sets the XML contents of this document.
	 * 
	 * @param xml a XML contents
	 * @return a Nodes instance that has document node
	 */
	public Nodes xml(String xml) {
		return doc().xml(xml);
	}
	
	/**
	 * Normalizes this document.
	 * This method executes below two action:
	 * - executes {@link org.w3c.dom.Document#normalize()}. 
	 * - removes waste namespace declarations.
	 * 
	 * @return a Nodes instance that has document node
	 */
	public Nodes normalize() {
		return doc().normalize();
	}
	
	@Override
	public XML clone() {
		return new XML(xmlContext, (Document)doc.cloneNode(true));
	}
	
	/**
	 * Creates a XSLT template transformer from this document.
	 * 
	 * @return a XSLT template transformer. null if not exists. 
	 * @throws TransformerConfigurationException if failed to load XSLT. 
	 */
	public Transformer toTransformer() throws TransformerConfigurationException {
		TransformerFactory tf = TransformerFactory.newInstance();
		return tf.newTransformer(new DOMSource(doc));
	}
	
	/**
	 * Gets a XSLT template transformer from a associated stylesheet.
	 * 
	 * @return a XSLT template transformer. null if not exists. 
	 * @throws TransformerConfigurationException if failed to load XSLT. 
	 */
	public Transformer stylesheet() throws TransformerConfigurationException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Source src = tf.getAssociatedStylesheet(new DOMSource(doc), null, null, null);
		return (src != null) ? tf.newTransformer(src) : null;
	}
	
	/**
	 * Transform current document by a specified transformer.
	 * 
	 * @param tf a XSLT template transformer object
	 * @return a reference to this object
	 * @throws TransformerException if failed to transform.
	 */
	public XML transform(Transformer tf) throws TransformerException {
		DOMResult result = new DOMResult();
		tf.transform(new DOMSource(doc), result);
		return new XML(xmlContext, (Document)result.getNode());
	}
	
	/**
	 * Writes to a file in UTF-8. This method close the stream.
	 * 
	 * @param file a file
	 * @throws IOException if I/O Error occurred
	 */
	public void writeTo(File file) throws IOException {
		writeTo(new FileOutputStream(file));
	}
	
	/**
	 * Writes to a binary stream in UTF-8. This method close the stream.
	 * 
	 * @param out a binary stream
	 * @throws IOException if I/O Error occurred
	 */
	public void writeTo(OutputStream out) throws IOException {
		XMLWriter serializer = new XMLWriter();
		serializer.setEncoding("UTF-8");
	
		try {
			serializer.writeTo(out, doc);
		} finally {
			out.close();
		}
	}
	
	/**
	 * Writes to a character stream. This method close the stream.
	 * 
	 * @param writer a character stream
	 * @throws IOException if I/O Error occurred
	 */
	public void writeTo(Writer writer) throws IOException {
		XMLWriter serializer = new XMLWriter();
		try {
			serializer.writeTo(writer, doc);
		} finally {
			writer.close();
		}
	}
	
	NodeMatcher compileXPathPattern(String text) {
		return xmlContext.compileXPathPattern(text);
	}

	Object compileXPath(String text, boolean pattern) {
		return xmlContext.compileXPath(text, pattern);
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
	
	/**
	 * Escapes text for XML contents.
	 * 
	 * @param text a text
	 * @return a escaped text
	 */
	public static String escape(String text) {
		StringBuilder sb = null;
		int start = 0;
		for (int i = 0; i < text.length(); i++) {
			int c = text.charAt(i);
			if (c < ESCAPE_CHARS.length) {
				String x = ESCAPE_CHARS[c];
				if (x != null) {
					if (sb == null) sb = new StringBuilder((int)(text.length() * 1.5));
					if (start < i) sb.append(text, start, i);
					sb.append(x);
					if (!x.endsWith(";")) sb.append((int)c).append(";");
					start = i + 1;
				}
			}
		}
		if (sb != null) {
			if (start < text.length()) sb.append(text, start, text.length());
			text = sb.toString();
		}
		return text;
	}
	
	/**
	 * Unescapes text for plain text.
	 * 
	 * @param text a escaped text
	 * @return a text
	 */
	public static String unescape(String text) {
		StringBuilder sb = null;
		int start = 0;
		
		// 0 & 1 a 2 m 3 p 4
		// 0 & 1 a 2 p 5 o 6 s 7
		// 0 & 1 l 8 t 9
		// 0 & 1 g 10 t 11
		// 0 & 1 q 12 u 13 o 14 t 15
		// 0 & 1 # 16 [0-9]+ 17 ; 0
		int state = 0;
		for (int i = 0; i < text.length(); i++) {
			int c = text.charAt(i);
			switch (c) {
			case '&':
				if (state == 0) {
					state = 1;
				} else {
					state = 0;
				}
				break;
			case 'a':
				if (state == 1) {
					state = 2;
				} else {
					state = 0;
				}
				break;
			case 'm':
				if (state == 2) {
					state = 3;
				} else {
					state = 0;
				}
				break;
			case 'p':
				if (state == 2) {
					state = 5;
				} else if (state == 3) {
					state = 4;
				} else {
					state = 0;
				}
				break;
			case 'o':
				if (state == 5) {
					state = 6;
				} else if (state == 13) {
					state = 14;
				} else {
					state = 0;
				}
				break;
			case 's':
				if (state == 6) {
					state = 7;
				} else {
					state = 0;
				}
				break;
			case 'l':
				if (state == 1) {
					state = 8;
				} else {
					state = 0;
				}
				break;
			case 'g':
				if (state == 1) {
					state = 10;
				} else {
					state = 0;
				}
				break;
			case 't':
				if (state == 8) {
					state = 9;
				} else if (state == 10) {
					state = 11;
				} else if (state == 14) {
					state = 15;
				} else {
					state = 0;
				}
				break;
			case 'q':
				if (state == 1) {
					state = 12;
				} else {
					state = 0;
				}
				break;
			case 'u':
				if (state == 12) {
					state = 13;
				} else {
					state = 0;
				}
				break;
			case '#':
				if (state == 1) {
					state = 16;
				} else {
					state = 0;
				}
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				if (state >= 16) {
					state++;
				} else {
					state = 0;
				}
				break;
			case ';':
				if (state == 4) {
					if (sb == null) sb = new StringBuilder(text.length());
					if (start < i - 4) sb.append(text, start, i - 4);
					sb.append('&');
					start = i + 1;
				} else if (state == 7) {
					if (sb == null) sb = new StringBuilder(text.length());
					if (start < i - 5) sb.append(text, start, i - 5);
					sb.append('\'');
					start = i + 1;
				} else if (state == 9) {
					if (sb == null) sb = new StringBuilder(text.length());
					if (start < i - 3) sb.append(text, start, i - 3);
					sb.append('<');
					start = i + 1;
				} else if (state == 11) {
					if (sb == null) sb = new StringBuilder(text.length());
					if (start < i - 3) sb.append(text, start, i - 3);
					sb.append('>');
					start = i + 1;
				} else if (state == 15) {
					if (sb == null) sb = new StringBuilder(text.length());
					if (start < i - 5) sb.append(text, start, i - 5);
					sb.append('"');
					start = i + 1;
				} else if (state > 16) {
					if (sb == null) sb = new StringBuilder(text.length());
					if (start < i - (state - 16 + 2)) sb.append(text, start, i - (state - 16 + 2));
					String num = text.substring(i - (state - 16), i);
					sb.appendCodePoint(Integer.parseInt(num));
					start = i + 1;
				}
				state = 0;
				break;
			}
		}
		if (sb != null) {
			if (start < text.length()) sb.append(text, start, text.length());
			text = sb.toString();
		}
		return text;
	}
}
