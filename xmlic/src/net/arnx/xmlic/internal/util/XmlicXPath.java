package net.arnx.xmlic.internal.util;

import java.io.Serializable;
import java.util.List;

import net.arnx.xmlic.XPathSyntaxException;
import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.FunctionContext;
import net.arnx.xmlic.internal.org.jaxen.JaxenException;
import net.arnx.xmlic.internal.org.jaxen.JaxenHandler;
import net.arnx.xmlic.internal.org.jaxen.NamespaceContext;
import net.arnx.xmlic.internal.org.jaxen.Navigator;
import net.arnx.xmlic.internal.org.jaxen.VariableContext;
import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.org.jaxen.dom.DocumentNavigator;
import net.arnx.xmlic.internal.org.jaxen.expr.XPathExpr;
import net.arnx.xmlic.internal.org.jaxen.function.BooleanFunction;
import net.arnx.xmlic.internal.org.jaxen.function.NumberFunction;
import net.arnx.xmlic.internal.org.jaxen.function.StringFunction;
import net.arnx.xmlic.internal.org.jaxen.saxpath.SAXPathException;
import net.arnx.xmlic.internal.org.jaxen.saxpath.XPathReader;
import net.arnx.xmlic.internal.org.jaxen.saxpath.helpers.XPathReaderFactory;
import net.arnx.xmlic.internal.org.jaxen.util.SingletonList;

public class XmlicXPath implements XPath, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final XMLContext xcontext;

	private final XPathExpr xpath;

	private Navigator navigator;
	
	public XmlicXPath(XMLContext xcontext, String xpathExpr) {
		this.xcontext = xcontext;
		this.navigator = new DocumentNavigator() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public XPath parseXPath(String xpath) throws SAXPathException {
				return new XmlicXPath(XmlicXPath.this.xcontext, xpath);
			}
		};
		
		try {
			XPathReader reader = XPathReaderFactory.createReader();
			JaxenHandler handler = new JaxenHandler();
			reader.setXPathHandler( handler );
			reader.parse( xpathExpr );
			this.xpath = handler.getXPathExpr();
		} catch (net.arnx.xmlic.internal.org.jaxen.saxpath.XPathSyntaxException e) {
			throw new XPathSyntaxException(e.getXPath(), e.getPosition(), e.getMultilineMessage(), e);
		} catch (SAXPathException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Object evaluate(Object context) throws JaxenException {
		List<?> answer = selectNodes(context);
		
		if (answer != null && answer.size() == 1) {
			Object first = answer.get(0);
			if (first instanceof String
				 || first instanceof Number
				 || first instanceof Boolean) {
				return first;
			}
		}
		return answer;
	}

	@Override
	public List<?> selectNodes(Object node) throws JaxenException {
        return xpath.asList(getContext(node));
	}

	@Override
	public Object selectSingleNode(Object node) throws JaxenException {
        List<?> results = selectNodes(node);
        return !results.isEmpty() ? results.get(0) : null;
	}
	
	@Override
	public String valueOf(Object node) throws JaxenException {
		return stringValueOf(node);
	}

	@Override
	public String stringValueOf(Object node) throws JaxenException {
		Context context = getContext(node);
        List<?> result = xpath.asList(context);
       	return StringFunction.evaluate(result, context.getNavigator());
	}

	@Override
	public boolean booleanValueOf(Object node) throws JaxenException {
        Context context = getContext(node);
        List<?> result = xpath.asList(context);
    	return BooleanFunction.evaluate(result, context.getNavigator()).booleanValue();
	}

	@Override
	public Number numberValueOf(Object node) throws JaxenException {
		Context context = getContext(node);
		List<?> result = xpath.asList(context);
		return NumberFunction.evaluate(result, context.getNavigator());
	}

	@Override
	public void addNamespace(String prefix, String uri) throws JaxenException {
		xcontext.addNamespace(prefix, uri);
	}

	@Override
	public void setNamespaceContext(NamespaceContext namespaceContext) {
		// no handle
	}

	@Override
	public void setFunctionContext(FunctionContext functionContext) {
		// no handle
	}

	@Override
	public void setVariableContext(VariableContext variableContext) {
		// no handle
	}

	@Override
	public NamespaceContext getNamespaceContext() {
		return xcontext.getContextSupport().getNamespaceContext();
	}

	@Override
	public FunctionContext getFunctionContext() {
		return xcontext.getContextSupport().getFunctionContext();
	}

	@Override
	public VariableContext getVariableContext() {
		return xcontext.getContextSupport().getVariableContext();
	}

	@Override
	public Navigator getNavigator() {
		return navigator;
	}
	
    protected Context getContext(Object node) {
        if (node instanceof Context) {
            return (Context)node;
        }

        Context context = new Context(xcontext.getContextSupport());
        if (node instanceof List) {
            context.setNodeSet((List<?>)node);
        } else {
            List<?> list = new SingletonList(node);
            context.setNodeSet(list);
        }
        return context;
    }
}
