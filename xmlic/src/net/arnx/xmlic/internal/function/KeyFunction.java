package net.arnx.xmlic.internal.function;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionCallException;
import net.arnx.xmlic.internal.org.jaxen.Navigator;
import net.arnx.xmlic.internal.org.jaxen.UnresolvableException;
import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.org.jaxen.function.StringFunction;
import net.arnx.xmlic.internal.org.jaxen.saxpath.SAXPathException;
import net.arnx.xmlic.internal.util.XmlicContext;
import net.arnx.xmlic.internal.util.XmlicContext.Key;
import net.arnx.xmlic.internal.util.XmlicXPath;

public class KeyFunction implements Function {
	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {
		if (args == null || args.size() != 2) {
			throw new FunctionCallException("key() requires two argument.");
		} else if (!(args.get(0) instanceof String)) {
			throw new FunctionCallException("invalid argument: key(string, node)");
		}
		
		XmlicContext xcontext;
		try {
			xcontext = (XmlicContext)context.getVariableValue(null, null, XmlicContext.VARIABLE_NAME);
		} catch (UnresolvableException e) {
			throw new FunctionCallException(e);
		}
		
		Navigator nav = context.getNavigator();
		Object doc = nav.getDocumentNode(xcontext.getCurrentNode());
		
		Key key = xcontext.getKey((String)args.get(0));
		String value = StringFunction.evaluate(args.get(1), nav);
		if (value == null) {
			return Collections.EMPTY_LIST;
		}
		
		try {
			
			List result = new XmlicXPath(xcontext, key.match, true).selectNodes(doc);
			XPath use = new XmlicXPath(xcontext, key.use, false);
			Iterator i = result.iterator();
			while (i.hasNext()) {
				Object node = i.next();
				if (!value.equals(use.stringValueOf(node))) {
					i.remove();
				}
			}
			return result;
		} catch (SAXPathException e) {
			throw new FunctionCallException(e);
		}
	}
}
