package net.arnx.xmlic;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * XMLWriter is for converting DOM to XML stream.
 */
public class XMLWriter {
	private String encoding;
	private String linesep;
	private Map<String, Object> params = new HashMap<>();
	
	public void setPrettyPrinting(boolean flag) {
		setParameter("format-pretty-print", flag);
	}
	
	public boolean isPrittyPrinting() {
		Object value = getParameter("format-pretty-print");
		if (value instanceof Boolean) {
			return (boolean)value;
		}
		return false;
	}
	
	public void setShowXMLDeclaration(boolean flag) {
		setParameter("xml-declaration", flag);
	}
	
	public boolean isShowXMLDeclaration() {
		Object value = getParameter("xml-declaration");
		if (value instanceof Boolean) {
			return (boolean)value;
		}
		return false;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public String getEncoding() {
		return encoding;
	}
	
	public void setLineSeparator(String linesep) {
		this.linesep = linesep;
	}
	
	public String getLineSeparator() {
		return linesep;
	}

	public void setParameter(String name, Object value) {
		if (value != null) {
			this.params.put(name, value);
		} else {
			this.params.remove(name);
		}
	}

	public Object getParameter(String name) {
		return this.params.get(name);
	}
	
	public void writeTo(OutputStream out, XML xml) throws IOException {
		writeTo(out, xml.get());
	}
	
	public void writeTo(Writer writer, XML xml) throws IOException {
		writeTo(writer, xml.get());
	}
	
	public void writeTo(OutputStream out, Node node) throws IOException {
		LSOutput output = createLSOutput(node);
		output.setByteStream(out);
		writeTo(output, node);
	}
	
	public void writeTo(Writer writer, Node node) throws IOException {
		LSOutput output = createLSOutput(node);
		output.setCharacterStream(writer);
		writeTo(output, node);
	}
	
	void writeTo(LSOutput output, Node node) throws IOException {
		LSSerializer serializer = createLSSerializer(node);
		if (linesep != null) serializer.setNewLine(linesep);
		
		DOMConfiguration conf = serializer.getDomConfig();
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			conf.setParameter(entry.getKey(), entry.getValue());
		}
		
		if (encoding != null) output.setEncoding(encoding);
		serializer.write(node, output);
	}
	
	static DOMImplementationLS getDOMImplementationLS(Document doc) {
		return (DOMImplementationLS)doc.getImplementation().getFeature("+LS", "3.0");
	}
	
	static LSSerializer createLSSerializer(Node node) {
		Document doc = (node instanceof Document) ? (Document)node : node.getOwnerDocument();
		return getDOMImplementationLS(doc).createLSSerializer();
	}
	
	static LSOutput createLSOutput(Node node) {
		Document doc = (node instanceof Document) ? (Document)node : node.getOwnerDocument();
		return getDOMImplementationLS(doc).createLSOutput();
	}
}
