package net.arnx.xmlic;

public interface Filter<T> {
	public boolean accept(Context<T> current);
}
