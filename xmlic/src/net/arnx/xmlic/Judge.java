package net.arnx.xmlic;

public interface Judge<T> {
	public boolean accept(T value, Status status);
}
