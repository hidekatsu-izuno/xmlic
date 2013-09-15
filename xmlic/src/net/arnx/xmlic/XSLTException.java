package net.arnx.xmlic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
		private int line;
		private int column;
		private String message;
		
		Detail(int line, int column, String message) {
			this.line = line;
			this.column = column;
			this.message = message;
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
			sb.append(message);
			return sb.toString();
		}
	}
}
