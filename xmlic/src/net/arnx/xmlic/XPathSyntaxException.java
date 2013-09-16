package net.arnx.xmlic;

/**
 * This type throws if XPath syntax error occurred.
 */
public class XPathSyntaxException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final String xpath;
	private final int position;
	
	/**
	 * Constructs XPathSyntaxException.
	 * 
	 * @param xpath XPath expression
	 * @param position an error occured position
	 * @param message an error message
	 * @param t an error cause
	 */
	public XPathSyntaxException(String xpath, int position, String message, Throwable t) {
		super(message, t);
		this.xpath = xpath;
		this.position = position;
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
