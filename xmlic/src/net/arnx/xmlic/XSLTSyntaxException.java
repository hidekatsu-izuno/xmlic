package net.arnx.xmlic;

public class XSLTSyntaxException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private int line;
	private int column;
	
	public XSLTSyntaxException(int line, int column, String message, Throwable t) {
		super(message, t);
		this.line = line;
		this.column = column;
	}
	
	public int getLineNumber() {
		return line;
	}
	
	public int getColumnNumber() {
		return column;
	}
}
