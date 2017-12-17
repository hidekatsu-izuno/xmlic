package net.arnx.xmlic;

/**
 * This interface uses for mapping values.
 * 
 * @param <S> mapping value type.
 * @param <R> mapped value type.
 */
public interface Mapper<S, R> {
	
	/**
	 * Mapping values.
	 * 
	 * @param value mapping value.
	 * @param status current callback status.
	 * @return mapped value.
	 */
	public R map(S value, Status status);
}
