package net.arnx.xmlic;

/**
 * This interface uses for judging value.
 * 
 * @param <T> judging value type.
 */
public interface Judge<T> {
	
	/**
	 * Judging to accept value.
	 * 
	 * @param value judging value.
	 * @param status current callback status.
	 * @return true if this value is accepted.
	 */
	public boolean accept(T value, Status status);
}
