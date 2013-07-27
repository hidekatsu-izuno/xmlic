package net.arnx.xmlic.internal.function;

import java.util.List;

import org.w3c.dom.Node;

import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionCallException;

public class CurrentFunction implements Function {
	private final Node node;
	
	public CurrentFunction(Node node) {
		this.node = node;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {
		if (args != null && args.size() != 0) {
			throw new FunctionCallException("current() requires no argument.");
		}
		
		return node;
	}
}
