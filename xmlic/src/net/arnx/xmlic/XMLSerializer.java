package net.arnx.xmlic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
	
	public void writeTo(File file, Node node) throws IOException {
		writeTo(new FileOutputStream(file), node);
	}
	
	public void writeTo(OutputStream out, Node node) throws IOException {
		String encoding = (this.encoding != null) ? this.encoding : "UTF-8";
		Writer writer = new BufferedWriter(new OutputStreamWriter(out, encoding));
		try {
			writeTo(writer, node);
		} finally {
			writer.flush();
		}
	}
	
	public void writeTo(Writer writer, Node node) throws IOException {
		Document doc = (node instanceof Document) ? (Document)node : node.getOwnerDocument();
		
		LSSerializer serializer = createLSSerializer(doc);
		if (linesep != null) serializer.setNewLine(linesep);
		
		DOMConfiguration conf = serializer.getDomConfig();
		conf.setParameter("format-pretty-print", prittyPrint);
		conf.setParameter("xml-declaration", xmlDeclaration);
		
		LSOutput output = createLSOutput(doc);
		output.setCharacterStream(writer);
		if (encoding != null) output.setEncoding(encoding);
		
		serializer.write(node, output);
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
