package net.arnx.xmlic;

public interface Filter<T> {
	public T filter(T value, Status status);
}
