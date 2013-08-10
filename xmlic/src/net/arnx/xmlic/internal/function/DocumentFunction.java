package net.arnx.xmlic.internal.function;

import java.net.URI;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionCallException;
import net.arnx.xmlic.internal.org.jaxen.Navigator;
import net.arnx.xmlic.internal.org.jaxen.UnresolvableException;
import net.arnx.xmlic.internal.org.jaxen.function.StringFunction;
import net.arnx.xmlic.internal.util.XmlicContext;

public class DocumentFunction implements Function {
	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {
		if (args == null || args.size() != 1) {
			throw new FunctionCallException("document() requires one argument.");
		}
		
		XmlicContext xcontext;
		try {
			xcontext = (XmlicContext)context.getVariableValue(null, null, XmlicContext.VARIABLE_NAME);
		} catch (UnresolvableException e) {
			throw new FunctionCallException(e);
		}
		
		Navigator nav = context.getNavigator();
		Document doc = (Document)nav.getDocumentNode(xcontext.getCurrentNode());
		
		try {
			URI uri = new URI(StringFunction.evaluate(args.get(0), nav));
			if (!uri.isAbsolute()) {
				if (doc.getBaseURI() != null) {
					uri = new URI(doc.getBaseURI()).resolve(uri);
				} else {
					throw new FunctionCallException("base url is missing.");
				}
			}
			
			DocumentBuilder builder = XmlicContext.getDocumentBuilder();
			return builder.parse(uri.toASCIIString());
		} catch (Exception e) {
			throw new FunctionCallException(e);
		}
	}
}
