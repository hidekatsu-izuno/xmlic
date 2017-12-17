package net.arnx.xmlic.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import net.arnx.xmlic.XMLException;
import net.arnx.xmlic.XMLException.Detail;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlicErrorHandler implements ErrorHandler, ErrorListener {
	private List<XMLException.Detail> warnings = new ArrayList<XMLException.Detail>(1);
	private List<XMLException.Detail> errors = new ArrayList<XMLException.Detail>(1);
	
	@Override
	public void warning(SAXParseException e) throws SAXException {
		int line = e.getLineNumber();
		int column = e.getColumnNumber();
		warnings.add(new XMLException.Detail(line, column, e.getMessage(), e));
	}
	
	@Override
	public void error(SAXParseException e) throws SAXException {
		int line = e.getLineNumber();
		int column = e.getColumnNumber();
		errors.add(new XMLException.Detail(line, column, e.getMessage(), e));
	}
	
	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		throw e;
	}

	@Override
	public void warning(TransformerException e) throws TransformerException {
		int line = (e.getLocator() != null) ? e.getLocator().getLineNumber() : -1;
		int column = (e.getLocator() != null) ? e.getLocator().getColumnNumber() : -1;
		warnings.add(new XMLException.Detail(line, column, e.getMessage(), e));
	}
	
	@Override
	public void error(TransformerException e) throws TransformerException {
		int line = (e.getLocator() != null) ? e.getLocator().getLineNumber() : -1;
		int column = (e.getLocator() != null) ? e.getLocator().getColumnNumber() : -1;
		errors.add(new XMLException.Detail(line, column, e.getMessage(), e));
	}
	
	@Override
	public void fatalError(TransformerException e) throws TransformerException {
		throw e;
	}
	
	public Collection<Detail> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
	
	public Collection<Detail> getErrors() {
		return Collections.unmodifiableCollection(errors);
	}
}
