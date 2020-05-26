package net.arnx.xmlic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

import net.arnx.xmlic.internal.util.XmlicErrorHandler;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XMLLoader is for loading XML file and building DOM.
 */
public class XMLLoader {
	boolean validating = false;
	Schema schema;
	boolean ignoringComments = false;
	boolean coalescing = false;
	boolean expandEntityReferences = true;
	boolean xincludeAware = true;

	Map<String, Boolean> features = new HashMap<>();
	Map<URI, URI> externalSources = new HashMap<>();
	
	public void setValidationg(boolean flag) {
		this.validating = flag;
	}
	
	public boolean isValidating() {
		return validating;
	}
	
	public void setSchema(Schema schema) {
		this.schema = schema;
	}
	
	public Schema getSchema() {
		return schema;
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

	public void setFeature(String key, boolean flag) {
		features.put(key, flag);
	}

	public boolean getFeature(String key) {
		Boolean result = features.get(key);
		if (result != null) {
			return result;
		}
		return false;
	}

	public void setExternalSource(String systemId, String url) {
		try {
			externalSources.put(new URI(systemId), new URI(url));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public String getExternalSource(String systemId) {
		try {
			return externalSources.get(new URI(systemId)).toASCIIString();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public XML load(URI uri) throws XMLException {
		return load(new InputSource(uri.normalize().toASCIIString()));
	}
	
	public XML load(InputStream in) throws XMLException {
		return load(new InputSource(in));
	}
	
	public XML load(Reader reader) throws XMLException {
		return load(new InputSource(reader));
	}
	
	XML load(InputSource is) throws XMLException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(validating);
		if (schema != null) {
			dbf.setSchema(schema);
		}
		dbf.setIgnoringComments(ignoringComments);
		dbf.setCoalescing(coalescing);
		dbf.setExpandEntityReferences(expandEntityReferences);
		dbf.setXIncludeAware(xincludeAware);

		for (Map.Entry<String, Boolean> entry : features.entrySet()) {
			try {
				dbf.setFeature(entry.getKey(), entry.getValue());
			} catch (ParserConfigurationException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
		if (is.getSystemId() != null) {
			db.setEntityResolver(new EntityResolverImpl(is.getSystemId(), externalSources));
		}
		
		XmlicErrorHandler handler = new XmlicErrorHandler();
		db.setErrorHandler(handler);
		
		try {
			return new XML(db.parse(is), handler.getWarnings());
		} catch (Exception e) {
			throw new XMLException(e.getMessage(), e, handler.getWarnings(), handler.getErrors());
		}
	}
	
	private static class EntityResolverImpl implements EntityResolver {
		private String base;
		private Map<URI, URI> externalSources;
		
		public EntityResolverImpl(String base, Map<URI, URI> externalSources) {
			this.base = base;
			this.externalSources = externalSources;
		}
		
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			if (systemId == null) return null;
			
			try {
				URI uri = new URI(systemId);
				if (uri.isAbsolute()) {
					URI replacement = externalSources.get(uri);
					if (replacement != null) {
						uri = replacement;
					}
				} else {
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
