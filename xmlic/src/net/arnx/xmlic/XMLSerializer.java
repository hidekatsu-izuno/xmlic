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

public class XMLSerializer {
	private boolean prittyPrint = false;
	private boolean xmlDeclaration = false;
	private String encoding;
	private String linesep;
	
	public void setPrettyPrintEnabled(boolean flag) {
		this.prittyPrint = flag;
	}
	
	public boolean isPrittyPrintEnabled() {
		return prittyPrint;
	}
	
	public void setXMLDeclarationVisible(boolean flag) {
		this.xmlDeclaration = flag;
	}
	
	public boolean isXMLDeclarationVisible() {
		return xmlDeclaration;
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
		conf.setParameter("format-pretty-print", prittyPrint);
		conf.setParameter("xml-declaration", xmlDeclaration);
		
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
