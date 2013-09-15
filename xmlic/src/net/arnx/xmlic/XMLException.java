package net.arnx.xmlic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.xml.sax.SAXParseException;

public class XMLException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private Collection<Detail> warnings;
	private Collection<Detail> errors;
	
	public XMLException(Throwable cause, Collection<Detail> warnings, Collection<Detail> errors) {
		super(cause.getMessage(), cause);
		this.warnings = Collections.unmodifiableCollection(new ArrayList<Detail>(warnings));
		this.errors = Collections.unmodifiableCollection(new ArrayList<Detail>(errors));
	}
	
	public Collection<Detail> getWarnings() {
		return warnings;
	}
	
	public Collection<Detail> getErrors() {
		return errors;
	}
	
	public static class Detail {
		private SAXParseException cause;
		
		Detail(SAXParseException cause) {
			this.cause = cause;
		}
		
		public int getLineNumber() {
			return cause.getLineNumber();
		}
		
		public int getColumnNumber() {
			return cause.getColumnNumber();
		}
		
		public String getMessage() {
			return cause.getMessage();
		}
		
		public Throwable getCause() {
			return cause;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			int line = getLineNumber();
			if (line >= 0) {
				sb.append("[").append(line);
				int column = getColumnNumber();
				if (column >= 0) {
					sb.append(",").append(column);
				}
				sb.append("] ");
			}
			sb.append(getMessage());
			return sb.toString();
		}
	}
}
