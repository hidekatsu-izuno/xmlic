package net.arnx.xmlic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XMLLoader is for loading XML file and building DOM.
 */
public class XMLLoader {
	boolean validating = false;
	boolean ignoringComments = false;
	boolean coalescing = false;
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
	
	public Document load(URI uri) throws XMLException {
		return load(new InputSource(uri.normalize().toASCIIString()));
	}
	
	public Document load(InputStream in) throws XMLException {
		return load(new InputSource(in));
	}
	
	public Document load(Reader reader) throws XMLException {
		return load(new InputSource(reader));
	}
	
	Document load(InputSource is) throws XMLException {
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
		
		if (is.getSystemId() != null) {
			db.setEntityResolver(new EntityResolverImpl(is.getSystemId()));
		}
		
		try {
			return db.parse(is);
		} catch (Exception e) {
			throw new XMLException(e.getMessage(), e);
		}
	}
	
	private static class EntityResolverImpl implements EntityResolver {
		private String base;
		
		public EntityResolverImpl(String base) {
			this.base = base;
		}
		
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			if (systemId == null) return null;
			
			try {
				URI uri = new URI(systemId);
				if (!uri.isAbsolute()) {
					if (this.base != null && !this.base.isEmpty()) {
						uri = new URI(this.base).resolve(uri);
					} else {
						throw new SAXException("base url is missing.");
					}
				}
				return new InputSource(uri.normalize().toASCIIString());
			} catch (URISyntaxException e) {
				throw new SAXException(e);
			}
		}
	}
}
