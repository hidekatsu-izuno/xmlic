package net.arnx.xmlic;

/**
 * This interface uses for the callback iteration.
 * 
 * @param <T> iterated value's type
 */
public interface Visitor<T> {
	/**
	 * Callback if current value visited.
	 * 
	 * @param current current value
	 * @param status current callback status
	 */
	public void visit(T current, Status status);
}
