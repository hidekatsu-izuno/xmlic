package net.arnx.xmlic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.xml.transform.TransformerException;

public class XSLTException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private Collection<Detail> warnings;
	private Collection<Detail> errors;
	
	public XSLTException(Throwable t, Collection<Detail> warnings, Collection<Detail> errors) {
		super(t.getMessage(), t);
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
		private TransformerException cause;
		
		Detail(TransformerException cause) {
			this.cause = cause;
		}
		
		public int getLineNumber() {
			return (cause.getLocator() != null) ? cause.getLocator().getLineNumber() : -1;
		}
		
		public int getColumnNumber() {
			return (cause.getLocator() != null) ? cause.getLocator().getColumnNumber() : -1;
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
