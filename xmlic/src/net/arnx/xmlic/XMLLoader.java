package net.arnx.xmlic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLLoader {
	boolean validating = false;
	boolean ignoringComments = false;
	boolean coalescing = true;
	boolean expandEntityReferences = true;
	boolean xincludeAware = true;
	
	public void setValidationg(boolean flag) {
		this.validating = flag;
	}
	
	public boolean isValidating() {
		return validating;
	}
	
	public void setIgnoringComments(boolean flag) {
		this.ignoringComments = flag;
	}
	
	public boolean isIgnoringComments() {
		return ignoringComments;
	}
	
	public void setCoalescing(boolean flag) {
		this.coalescing = flag;
	}
	
	public boolean isCoalescing() {
		return coalescing;
	}
	
	public void setExpandEntityReferences(boolean flag) {
		this.expandEntityReferences = flag;
	}
	
	public boolean isExpandEntityReferences() {
		return expandEntityReferences;
	}
	
	public void setXIncludeAware(boolean flag) {
		this.xincludeAware = flag;
	}
	
	public boolean isXIncludeAware() {
		return xincludeAware;
	}
	
	public Document load(URI uri) throws IOException {
		return load(new InputSource(uri.normalize().toASCIIString()));
	}
	
	public Document load(InputStream in) throws IOException {
		return load(new InputSource(in));
	}
	
	public Document load(Reader reader) throws IOException {
		return load(new InputSource(reader));
	}
	
	Document load(InputSource is) throws IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(validating);
		dbf.setIgnoringComments(ignoringComments);
		dbf.setCoalescing(coalescing);
		dbf.setExpandEntityReferences(expandEntityReferences);
		dbf.setXIncludeAware(xincludeAware);
		
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		
		try {
			return db.parse(is);
		} catch (SAXException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
