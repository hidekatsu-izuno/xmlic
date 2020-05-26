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
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.util.NodeMatcher;
import net.arnx.xmlic.internal.util.XmlicContext;
import net.arnx.xmlic.internal.util.XmlicContext.Key;

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
	 * Load an XML document from a input file path.
	 *
	 * @param path a input file path.
	 * @return a new XML instance.
	 * @throws XMLException if XML parsing error caused.
	 */
	public static XML load(Path path) throws XMLException {
		return load(path.toFile());
	}

	/**
	 * Load an XML document from a input file.
	 *
	 * @param file a input file.
	 * @return a new XML instance.
	 * @throws XMLException if XML parsing error caused.
	 */
	public static XML load(File file) throws XMLException {
		return load(file.toURI());
	}

	/**
	 * Load an XML document from a URI.
	 *
	 * @param uri a URI.
	 * @return a new XML instance.
	 * @throws XMLException if XML parsing error caused.
	 */
	public static XML load(URI uri) throws XMLException {
		return new XMLLoader().load(uri);
	}

	/**
	 * Load an XML document from a URL.
	 *
	 * @param url a URL.
	 * @return a new XML instance.
	 * @throws XMLException if XML parsing error caused.
	 */
	public static XML load(URL url) throws XMLException {
		try {
			return load(url.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Load an XML document from a binary input stream.
	 *
	 * @param in a binary input stream.
	 * @return a new XML instance.
	 * @throws XMLException if XML parsing error caused.
	 */
	public static XML load(InputStream in) throws XMLException {
		return new XMLLoader().load(in);
	}

	/**
	 * Load an XML document from a character input stream.
	 *
	 * @param reader a character input stream.
	 * @return a new XML instance.
	 * @throws XMLException if XML parsing error caused.
	 */
	public static XML load(Reader reader) throws XMLException {
		return new XMLLoader().load(reader);
	}

	final Document doc;
	final XmlicContext xmlContext;
	final Collection<XMLException.Detail> warnings;

	/**
	 * Construct a new XML instance by the empty Document.
	 */
	public XML() {
		this.xmlContext = new XmlicContext();
		this.doc = XmlicContext.getDocumentBuilder().newDocument();
		this.warnings = Collections.emptyList();
	}

	/**
	 * Construct a new XML instance by parsing the specified text.
	 *
	 * @param text an XML text.
	 * @throws XMLException if XML parsing error caused.
	 */
	public XML(String text) throws XMLException {
		XMLLoader loader = new XMLLoader();
		XML xml = loader.load(new StringReader(text));

		this.xmlContext = xml.xmlContext;
		this.doc = xml.doc;
		this.warnings = xml.warnings;
	}

	/**
	 * Construct a new XML instance by the specified {@link org.w3c.dom.Document} instance.
	 *
	 * @param doc an instance of Document.
	 */
	public XML(Document doc) {
		this(doc, Collections.<XMLException.Detail>emptyList());
	}

	XML(Document doc, Collection<XMLException.Detail> warnings) {
		if (doc == null) throw new NullPointerException("doc must not be null.");
		if (warnings == null) throw new NullPointerException("warnings must not be null.");

		this.xmlContext = new XmlicContext();
		this.doc = doc;
		this.warnings = warnings;

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

	XML(XmlicContext nsContext, Document doc, Collection<XMLException.Detail> warnings) {
		this.xmlContext = nsContext;
		this.doc = doc;
		this.warnings = warnings;
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
	 * Gets namespace to a namespace prefix.
	 *
	 * @param prefix a namespace prefix
	 * @return the namespace URI for a sprcified prefix
	 */
	public String getNamespaceMapping(String prefix) {
		return xmlContext.getNamespaceURI(prefix);
	}

	/**
	 * Gets a mapping of namespace for all namespace prefixes.
	 *
	 * @return a mapping of namespace for all namespace prefixes
	 */
	public Map<String, String> getNamespaceMappings() {
		Map<String, String> map = new HashMap<String, String>();
		for (String prefix : xmlContext.getPrefixes()) {
			map.put(prefix, xmlContext.getNamespaceURI(prefix));
		}
		return map;
	}

	/**
	 * Removes a mapping of namespace for using XPath expression.
	 *
	 * @param prefix a namespace prefix
	 * @return a reference to this object
	 */
	public XML removeNamespaceMapping(String prefix) {
		xmlContext.removeNamespace(prefix);
		return this;
	}

	/**
	 * Adds a key definition.
	 *
	 * @param name a key name
	 * @param match a node pattern which the key will be applied
	 * @param use the value of the key for each of the nodes
	 * @return a reference to this object
	 */
	public XML addKey(String name, String match, String use) {
		xmlContext.addKey(name, new Key(match, use));
		return this;
	}

	/**
	 * Remove a key definition.
	 *
	 * @param name a key name
	 * @return a reference to this object
	 */
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
		Element elem = doc.getDocumentElement();
		if (elem != null) {
			Nodes nodes = new Nodes(this, doc(), 1);
			nodes.add(elem);
			return nodes;
		} else {
			return new Nodes(this, doc(), 0);
		}
	}

	/**
	 * Sets the root element of this document.
	 *
	 * @param nodes the root element of this document
	 * @return a Nodes instance that has the root element of this document
	 */
	public Nodes root(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) {
			return doc().empty();
		} else {
			return doc().empty().append(nodes);
		}
	}

	/**
	 * Evaluate a specified XPath expression at this document.
	 * And gets as a specified type.
	 * This method is same to doc().evaluate(xpath, cls).
	 *
	 * @param <T> a result type parameter
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
	 * @param pattern a pattern
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
		return new XML(xmlContext, (Document)doc.cloneNode(true), warnings);
	}

	static final Pattern ATTR_PATTERN = Pattern.compile("\\G[ \\t\\r\\n]*([^ \\t\\r\\n=]+)[ \\t\\r\\n]*=[ \\t\\r\\n]*(?:\"([^\"]+)\"|'([^'])')[ \\t\\r\\n]*");

	/**
	 * Gets a XSLT template transformer from a associated stylesheet.
	 *
	 * @return a XSLT template transformer. null if not exists.
	 * @throws XMLException if XSLT load error caused.
	 */
	public XSLT stylesheet() throws XMLException {
		String target = null;

		NodeList list = doc.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) break;
			if (node.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE) continue;

			ProcessingInstruction pi = (ProcessingInstruction)node;
			if (!"xml-stylesheet".equals(pi.getTarget()) || pi.getData() == null) continue;

			Matcher m = ATTR_PATTERN.matcher(pi.getData());
			boolean valid = true;
			String type = null;
			String href = null;
			while (m.find()) {
				String aname = m.group(1);
				String avalue = unescape((m.group(2) != null) ? m.group(2) : m.group(3));
				if ("href".equals(aname)) {
					href = avalue;
				} else if ("type".equals(aname)) {
					type = avalue;
				} else if ("title".equals(aname)
						|| "media".equals(aname)
						|| "charset".equals(aname)
						|| "alternate".equals(aname)) {
					// no handle
				} else {
					valid = false;
					break;
				}
			}

			if (valid && "text/xsl".equals(type) && href != null) {
				target = href;
			}
		}

		if (target != null) {
			try {
				URI uri = new URI(target);
				if (!uri.isAbsolute()) {
					if (doc.getBaseURI() != null) {
						uri = new URI(doc.getBaseURI()).resolve(uri);
					} else {
						throw new IllegalStateException("base url is missing.");
					}
				}
				return XSLT.load(uri);
			} catch (URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		} else {
			return null;
		}
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
			serializer.writeTo(out, this);
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
			serializer.writeTo(writer, this);
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
			xwriter.writeTo(writer, this);
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