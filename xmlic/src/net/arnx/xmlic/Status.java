package net.arnx.xmlic;

/**
 * Callback function status.
 */
public interface Status {
	/**
	 * Checks if current item is first.
	 * 
	 * @return true if current item is first.
	 */
	public boolean isFirst();
	
	/**
	 * Checks if current item is last.
	 * 
	 * @return true if current item is last.
	 */
	public boolean isLast();
	
	/**
	 * Returns the current index.
	 * 
	 * @return the current index.
	 */
	public int getIndex();
	
	/**
	 * Cancels this iteration.
	 * 
	 * @throws RuntimeException if you call this method.
	 */
	public void cancel() throws RuntimeException;
}
