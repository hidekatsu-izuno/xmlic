package net.arnx.xmlic;

public interface Filter<T> {
	public boolean accept(int index, T current);
}
