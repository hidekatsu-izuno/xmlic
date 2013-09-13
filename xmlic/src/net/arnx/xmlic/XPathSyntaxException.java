package net.arnx.xmlic;

public class XPathSyntaxException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final String xpath;
	private final int position;
	
	public XPathSyntaxException(String xpath, int pos, String message, Throwable t) {
		super(message, t);
		this.xpath = xpath;
		this.position = pos;
	}
	
	/**
	 * Gets a path.
	 * 
	 * @return a path
	 */
	public String getXPath() {
		return xpath;
	}
	
	/**
	 * Gets a position.
	 * 
	 * @return a position
	 */
	public int getPosition() {
		return position;
	}
}
