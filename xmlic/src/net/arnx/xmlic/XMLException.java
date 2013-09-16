package net.arnx.xmlic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
		private int line;
		private int column;
		private String message;
		private Throwable cause;
		
		public Detail(int line, int column, String message, Throwable cause) {
			this.line = line;
			this.column = column;
			this.message = message;
			this.cause = cause;
		}
		
		public int getLineNumber() {
			return line;
		}
		
		public int getColumnNumber() {
			return column;
		}
		
		public String getMessage() {
			return message;
		}
		
		public Throwable getCause() {
			return cause;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (line >= 0) {
				sb.append("[").append(line);
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
