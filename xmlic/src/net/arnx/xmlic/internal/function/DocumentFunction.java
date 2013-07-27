package net.arnx.xmlic.internal.function;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionCallException;
import net.arnx.xmlic.internal.org.jaxen.Navigator;
import net.arnx.xmlic.internal.org.jaxen.function.StringFunction;
import net.arnx.xmlic.internal.util.XMLContext;

public class DocumentFunction implements Function {
	private final URI base;
	
	public DocumentFunction(String base) {
		try {
			this.base = (base != null) ? new URI(base) : null;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {
		if (args == null || args.size() != 1) {
			throw new FunctionCallException("document() requires one argument.");
		}
		
		try {
			Navigator nav = context.getNavigator();
			
			URI uri = new URI(StringFunction.evaluate(args.get(0), nav));
			if (!uri.isAbsolute()) {
				if (base != null) {
					uri = base.resolve(uri);
				} else {
					throw new FunctionCallException("base url is missing.");
				}
			}
			
			DocumentBuilder builder = XMLContext.getDocumentBuilder();
			return builder.parse(uri.toASCIIString());
		} catch (Exception e) {
			throw new FunctionCallException(e);
		}
	}
}
