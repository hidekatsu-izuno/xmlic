package net.arnx.xmlic;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * XMLWriter is for converting DOM to XML Text file or stream.
 */
public class XMLWriter {
	private boolean prittyPrinting = false;
	private boolean showXmlDeclaration = true;
	private String encoding;
	private String linesep;
	
	public void setPrettyPrinting(boolean flag) {
		this.prittyPrinting = flag;
	}
	
	public boolean isPrittyPrinting() {
		return prittyPrinting;
	}
	
	public void setShowXMLDeclaration(boolean flag) {
		this.showXmlDeclaration = flag;
	}
	
	public boolean isShowXMLDeclaration() {
		return showXmlDeclaration;
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
		conf.setParameter("format-pretty-print", prittyPrinting);
		conf.setParameter("xml-declaration", showXmlDeclaration);
		
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
