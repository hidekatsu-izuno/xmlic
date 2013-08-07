package net.arnx.xmlic.internal.function;

import java.util.List;

import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionCallException;
import net.arnx.xmlic.internal.org.jaxen.UnresolvableException;
import net.arnx.xmlic.internal.util.XMLContext;

public class CurrentFunction implements Function {
	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {
		if (args != null && args.size() != 0) {
			throw new FunctionCallException("current() requires no argument.");
		}
		
		XMLContext xcontext;
		try {
			xcontext = (XMLContext)context.getVariableValue(null, null, XMLContext.VARIABLE_NAME);
		} catch (UnresolvableException e) {
			throw new FunctionCallException(e);
		}
		return xcontext.getCurrentNode();
	}
}
