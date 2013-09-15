package net.arnx.xmlic;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

public class XSLT {
	
	/**
	 * Load an XSLT transformer from a input file.
	 * 
	 * @param file a input file.
	 * @return a new XSLT instance.
	 * @throws XSLTException if XSLT parsing error caused. 
	 */
	public static XSLT load(File file) throws XSLTException {
		return load(file.toURI());
	}
	
	/**
	 * Load an XSLT transformer from a URI.
	 * 
	 * @param uri a URI.
	 * @return a new XML instance.
	 * @throws XSLTException if XSLT parsing error caused. 
	 */
	public static XSLT load(URI uri) throws XSLTException {
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			String base = uri.normalize().toASCIIString();
			URIResolver resolver = new URIResolverImpl(base);
			tf.setURIResolver(resolver);
			Transformer t = tf.newTransformer(new StreamSource(base));
			t.setURIResolver(resolver);
			return new XSLT(t);
		} catch (TransformerConfigurationException e) {
			throw toXSLTSyntaxException(e);
		}
	}
	
	/**
	 * Load an XSLT transformer from a URL.
	 * 
	 * @param url a URL.
	 * @return a new XSLT instance.
	 * @throws XSLTException if XSLT parsing error caused. 
	 */
	public static XSLT load(URL url) throws XSLTException {
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
	 * @throws XSLTException if XSLT parsing error caused. 
	 */
	public static XSLT load(InputStream in) throws XSLTException {
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			return new XSLT(tf.newTransformer(new StreamSource(in)));
		} catch (TransformerConfigurationException e) {
			throw toXSLTSyntaxException(e);
		}
	}
	
	/**
	 * Load an XSLT transformer from a character input stream.
	 * 
	 * @param reader a character input stream.
	 * @return a new XSLT instance.
	 * @throws XSLTException if XSLT parsing error caused. 
	 */
	public static XSLT load(Reader reader) throws XSLTException {
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			return new XSLT(tf.newTransformer(new StreamSource(reader)));
		} catch (TransformerConfigurationException e) {
			throw toXSLTSyntaxException(e);
		}
	}
	
	/**
	 * Load an XSLT transformer from a DOM document.
	 * 
	 * @param doc a DOM document
	 * @return a new XSLT instance.
	 * @throws XSLTException if XSLT parsing error caused. 
	 */
	public static XSLT load(Document doc) throws XSLTException {
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			URIResolver resolver = new URIResolverImpl(doc.getBaseURI());
			tf.setURIResolver(resolver);
			Transformer t = tf.newTransformer(new DOMSource(doc));
			t.setURIResolver(resolver);
			return new XSLT(t);
		} catch (TransformerConfigurationException e) {
			throw toXSLTSyntaxException(e);
		}
	}
	
	/**
	 * Load an XSLT transformer from a DOM document.
	 * 
	 * @param doc a DOM document
	 * @return a new XSLT instance.
	 * @throws XSLTException if XSLT syntax error caused. 
	 */
	public static XSLT load(XML xml) throws XSLTException {
		return load(xml.get());
	}
	
	private final Transformer transformer;
	
	public XSLT(Transformer transformer) {
		this.transformer = transformer;
	}
	
	public Transformer get() {
		return transformer;
	}
	
	public XML transform(XML xml) {
		DOMResult result = new DOMResult();
		try {
			transformer.transform(new DOMSource(xml.get()), result);
		} catch (TransformerException e) {
			throw new IllegalStateException(e);
		}
		return new XML(xml.xmlContext, (Document)result.getNode());
	}
	
	static XSLTException toXSLTSyntaxException(TransformerConfigurationException e) {
		int line = (e.getLocator() != null) ? e.getLocator().getLineNumber() : -1;
		int column = (e.getLocator() != null) ? e.getLocator().getColumnNumber() : -1;
		String message = e.getMessage();
		
		throw new XSLTException(line, column, message, e);	
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
