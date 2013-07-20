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
	boolean ignoreComments = false;
	boolean coalescing = true;
	boolean expandEntityReferences = true;
	boolean xincludeEnabled = true;
	
	public void setValidationg(boolean flag) {
		this.validating = flag;
	}
	
	public boolean isValidating() {
		return validating;
	}
	
	public void setIgnoreComments(boolean flag) {
		this.ignoreComments = flag;
	}
	
	public boolean isIgnoreComments() {
		return ignoreComments;
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
	
	public void setXIncludeEnabled(boolean flag) {
		this.xincludeEnabled = flag;
	}
	
	public boolean isXIncludeEnabled() {
		return xincludeEnabled;
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
		dbf.setValidating(validating);
		dbf.setIgnoringComments(ignoreComments);
		dbf.setCoalescing(coalescing);
		dbf.setNamespaceAware(true);
		dbf.setExpandEntityReferences(expandEntityReferences);
		dbf.setXIncludeAware(xincludeEnabled);
		
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			db.setEntityResolver(new ResourceResolver());
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
