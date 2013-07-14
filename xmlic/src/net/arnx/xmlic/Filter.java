package net.arnx.xmlic;

public interface Filter<T> {
	public boolean accept(T t);
}
