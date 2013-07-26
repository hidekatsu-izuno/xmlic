package net.arnx.xmlic.internal.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ResourceResolver implements EntityResolver, URIResolver, Serializable {
	private static final long serialVersionUID = 1L;
	
	public DocumentBuilder getDocumentBuilder() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setCoalescing(true);
		dbf.setNamespaceAware(true);
		dbf.setExpandEntityReferences(true);
		dbf.setXIncludeAware(true);
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(this);
			return db;
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		if (systemId == null) return null;
		
		try {
			URI uri = toURI(publicId, systemId);
			return new InputSource(uri.toURL().openConnection().getInputStream());
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}
	
	@Override
	public Source resolve(String href, String base) throws TransformerException {
		if (href == null) return null;
		
		try {
			URI uri = toURI(base, href);
			return new StreamSource(uri.toURL().openConnection().getInputStream(),
					uri.normalize().toASCIIString());
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}
	
	private static URI toURI(String base, String href) {
		if (base != null && !base.isEmpty()) {
			return URI.create(base).resolve(href);
		} else {
			return URI.create(href);
		}
	}
}
