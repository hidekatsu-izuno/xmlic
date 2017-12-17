package net.arnx.xmlic;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import net.arnx.xmlic.internal.util.XmlicErrorHandler;

import org.w3c.dom.Document;

public class XSLT {
	
	/**
	 * Load an XSLT transformer from a input file.
	 * 
	 * @param file a input file.
	 * @return a new XSLT instance.
	 * @throws XMLException if XSLT parsing error caused. 
	 */
	public static XSLT load(File file) throws XMLException {
		return load(file.toURI());
	}
	
	/**
	 * Load an XSLT transformer from a URI.
	 * 
	 * @param uri a URI.
	 * @return a new XML instance.
	 * @throws XMLException if XSLT parsing error caused. 
	 */
	public static XSLT load(URI uri) throws XMLException {
		String path = uri.normalize().toASCIIString();
		return load(new StreamSource(path), path);
	}
	
	/**
	 * Load an XSLT transformer from a URL.
	 * 
	 * @param url a URL.
	 * @return a new XSLT instance.
	 * @throws XMLException if XSLT parsing error caused. 
	 */
	public static XSLT load(URL url) throws XMLException {
		try {
			return load(url.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Load an XSLT transformer from a binary input stream.
	 * 
	 * @param in a binary input stream.
	 * @return a new XSLT instance.
	 * @throws XMLException if XSLT parsing error caused. 
	 */
	public static XSLT load(InputStream in) throws XMLException {
		return load(new StreamSource(in), null);
	}
	
	/**
	 * Load an XSLT transformer from a character input stream.
	 * 
	 * @param reader a character input stream.
	 * @return a new XSLT instance.
	 * @throws XMLException if XSLT parsing error caused. 
	 */
	public static XSLT load(Reader reader) throws XMLException {
		return load(new StreamSource(reader), null);
	}
	
	/**
	 * Load an XSLT transformer from a DOM document.
	 * 
	 * @param doc a DOM document
	 * @return a new XSLT instance.
	 * @throws XMLException if XSLT parsing error caused. 
	 */
	public static XSLT load(Document doc) throws XMLException {
		return load(new DOMSource(doc), doc.getBaseURI());
	}
	
	/**
	 * Load an XSLT transformer from a DOM document.
	 * 
	 * @param xml a XML instance
	 * @return a new XSLT instance.
	 * @throws XMLException if XSLT syntax error caused. 
	 */
	public static XSLT load(XML xml) throws XMLException {
		return load(new DOMSource(xml.get()), xml.get().getBaseURI());
	}
	
	static XSLT load(Source source, String base) throws XMLException {
		TransformerFactory tf = TransformerFactory.newInstance();
		XmlicErrorHandler handler = new XmlicErrorHandler();
		URIResolver resolver = new URIResolverImpl(base);
		
		try {
			tf.setErrorListener(handler);
			tf.setURIResolver(resolver);
			Transformer t = tf.newTransformer(source);
			t.setURIResolver(resolver);
			return new XSLT(t, handler.getWarnings());
		} catch (TransformerConfigurationException e) {
			throw new XMLException(e.getMessage(), e, handler.getWarnings(), handler.getErrors());
		}
	}
	
	final Transformer transformer;
	final Collection<XMLException.Detail> warnings;
	
	public XSLT(Transformer transformer) {
		this(transformer, Collections.<XMLException.Detail>emptyList());
	}
	
	XSLT(Transformer transformer, Collection<XMLException.Detail> warnings) {
		this.transformer = transformer;
		this.warnings = warnings;
	}
	
	public Transformer get() {
		return transformer;
	}
	
	public Collection<XMLException.Detail> getWarnings() {
		return warnings;
	}
	
	public XML transform(XML xml) throws XMLException {
		DOMResult result = new DOMResult();
		XmlicErrorHandler handler = new XmlicErrorHandler();
		transformer.setErrorListener(handler);
		try {
			transformer.transform(new DOMSource(xml.get()), result);
		} catch (TransformerException e) {
			throw new XMLException(e.getMessage(), e, handler.getWarnings(), handler.getErrors());
		}
		return new XML(xml.xmlContext, (Document)result.getNode(), handler.getWarnings());
	}
	
	static class URIResolverImpl implements URIResolver {
		private String base;
		
		public URIResolverImpl(String base) {
			this.base = base;
		}
		
		@Override
		public Source resolve(String href, String base) throws TransformerException {
			try {
				URI uri = new URI(href);
				if (!uri.isAbsolute()) {
					if (base != null && !base.isEmpty()) {
						uri = new URI(base).resolve(uri);
					} else if (this.base != null && !this.base.isEmpty()) {
						uri = new URI(this.base).resolve(uri);
					} else {
						throw new TransformerException("base url is missing.");
					}
				}
				return new StreamSource(uri.normalize().toASCIIString());
			} catch (URISyntaxException e) {
				throw new TransformerException(e);
			}
		}
	}
}
