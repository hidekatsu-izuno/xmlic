package net.arnx.xmlic;

/**
 * This interface uses for filtering value.
 * 
 * @param <T> filtering value type.
 */
public interface Filter<T> {
	/**
	 * Filters value.
	 * 
	 * @param value filtering value.
	 * @param status current callback status.
	 * @return filtered value.
	 */
	public T filter(T value, Status status);
}
