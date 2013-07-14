package net.arnx.xmlic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import net.arnx.xmlic.DOMFactory.XPathNSResolverImpl;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.xpath.XPathExpression;
import org.w3c.dom.xpath.XPathResult;

public class Nodes extends ArrayList<Node> {
	private static final long serialVersionUID = 1L;
	
	private static enum SelectMode {
		FIRST,
		UNTIL,
		ALL
	}
	
	private XML owner;
	private Nodes back;
	
	Nodes(XML owner, Nodes back, int size) {
		super(size);
		this.owner = owner;
		this.back = back;
	}
	
	Nodes(Nodes back, int size) {
		this(back.getOwner(), back, size);
	}
	
	Nodes(Nodes back, Node node) {
		this(back.getOwner(), back, 1);
		add(node);
	}
	
	Nodes(Nodes back, Nodes nodes) {
		this(back.getOwner(), back, nodes.size());
		for (Node node : nodes) {
			add(node.cloneNode(true));
		}
	}
	
	public XML getOwner() {
		return owner;
	}
	
	@Override
	public Node get(int index) {
		if (index < -size() || index >= size()) {
			return null;
		}
		return super.get((index < 0) ? size() + index : index);
	}
	
	public String namespaceURI() {
		if (isEmpty() || !(get(0) instanceof Element)) return null;
		return ((Element)get(0)).getNamespaceURI();
	}
	
	public Nodes namespaceURI(String uri) {
		if (isEmpty()) return this;
		
		for (Node self : this) {
			String name = self.getLocalName();
			if (uri != null && !self.isDefaultNamespace(uri)) {
				String prefix = self.lookupPrefix(uri);
				if (prefix == null && getOwner().nsResolver instanceof XPathNSResolverImpl) {
					prefix = ((XPathNSResolverImpl)getOwner().nsResolver).lookupPrefix(uri);
				}
				if (prefix != null) name = prefix + ":" + name;
			}
			getOwner().doc.renameNode(self, uri, name);
		}
		
		return this;
	}
	
	public String prefix() {
		if (isEmpty() || !(get(0) instanceof Element)) return null;
		return ((Element)get(0)).getPrefix();
	}
	
	public String localName() {
		if (isEmpty() || !(get(0) instanceof Element)) return null;
		return ((Element)get(0)).getLocalName();
	}
	
	public String name() {
		if (isEmpty() || !(get(0) instanceof Element)) return null;
		return ((Element)get(0)).getTagName();
	}
	
	public Nodes rename(String name) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (isEmpty()) return this;
		
		String uri = null;
		String localName = null;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			uri = getOwner().nsResolver.lookupNamespaceURI(name.substring(0, index));
			if (uri == null) localName = name;
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return null;
		
		for (Node self : this) {
			getOwner().doc.renameNode(self, uri, name);
		}
		
		return this;
	}
	
	public String attr(String name) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (isEmpty() || !(get(0) instanceof Element)) return null;
		
		String uri = null;
		String localName = null;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			uri = getOwner().nsResolver.lookupNamespaceURI(name.substring(0, index));
			if (uri == null) localName = name;
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return null;

		if (uri != null) {
			return ((Element)get(0)).getAttributeNS(uri, localName);
		} else {
			return ((Element)get(0)).getAttribute(name);
		}
	}
	
	public Nodes attr(String name, String value) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (isEmpty()) return this;
		if (value == null) value = "";
		
		String uri = null;
		String localName = null;

		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			uri = getOwner().nsResolver.lookupNamespaceURI(name.substring(0, index));
			if (uri == null) localName = name;
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return this;
		
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
			
			if (uri != null) {
				((Element)self).setAttributeNS(uri, name, value);
			} else {
				((Element)self).setAttribute(name, value);
			}
		}
		return this;
	}
	
	public Nodes removeAttr(String name) {
		if (name == null) return this;
		if (isEmpty()) return this;
		
		String uri = null;
		String localName = null;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			uri = getOwner().nsResolver.lookupNamespaceURI(name.substring(0, index));
			if (uri == null) localName = name;
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return this;
		
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
						
			if (uri != null) {
				((Element)self).removeAttributeNS(uri, localName);
			} else {
				((Element)self).removeAttribute(localName);
			}
		}
		return this;
	}
	
	public Nodes eq(int index) {
		Node self = get(index);
		if (self != null) {
			return new Nodes(this, self);
		} else {
			return new Nodes(this, 0);
		}
	}
	
	public boolean is(String filter) {
		if (filter == null || isEmpty()) return false;
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		for (Node node : this) {
			result = (XPathResult)expr.evaluate(node, XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) return true;
		}
		return false;
	}
	
	public int index(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) return -1;
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		for (int i = 0; i < size(); i++) {
			result = (XPathResult)expr.evaluate(get(i), XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) return i;
		}
		return -1;
	}
	
	public int index(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return -1;
		if (isEmpty()) return -1;
		
		return index(nodes.get(0));
	}
	
	public int index(Node node) {
		for (int i = 0; i < size(); i++) {
			if (get(i) == node) return i;
		}
		return -1;
	}
	
	public Nodes has(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(child::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) nodes.add(self);
		}
		unique(nodes);
		return nodes;
	}
	
	@Override
	public void add(int index, Node node) {
		if (isExternalNode(node)) {
			node = getOwner().doc.importNode(node, true);
		}
		super.add(index, node);
	}
	
	@Override
	public boolean add(Node node) {
		if (isExternalNode(node)) {
			node = getOwner().doc.importNode(node, true);
		}
		return super.add(node);
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends Node> c) {
		boolean result = super.addAll(index, c);
		if (result) {
			for (int i = index; i < index + c.size(); i++) {
				Node node = super.get(i);
				if (isExternalNode(node)) {
					super.set(i, getOwner().doc.importNode(node, true));
				}
			}
		}
		return result;
	}
	
	@Override
	public boolean addAll(Collection<? extends Node> c) {
		boolean result = super.addAll(c);
		if (result) {
			for (int i = 0; i < c.size(); i++) {
				Node node = super.get(i);
				if (isExternalNode(node)) {
					super.set(i, getOwner().doc.importNode(node, true));
				}
			}
		}
		return result;
	}
	
	@Override
	public Node set(int index, Node node) {
		if (isExternalNode(node)) {
			node = getOwner().doc.importNode(node, true);
		}
		return super.set(index, node);
	}
	
	public Nodes add(Nodes nodes) {
		if (nodes == null|| isEmpty()) {
			return new Nodes(this, this);
		}
		
		Nodes results = new Nodes(this, size() + nodes.size());
		results.addAll(this);
		results.addAll(nodes);
		unique(results);
		return results;
	}
	
	public Nodes add(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return new Nodes(this, this);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression(xpath, owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size() * 2);
		results.addAll(this);
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.UNORDERED_NODE_ITERATOR_TYPE, result);
			Node node;
			while ((node = result.iterateNext()) != null) {
				if (node.getNodeType() != Node.ELEMENT_NODE) continue;
				results.add(node);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes addBack() {
		if (back == null || back.isEmpty() || back == this) {
			return new Nodes(this, this);
		}
		
		Nodes results = new Nodes(this, size() + back.size());
		results.addAll(this);
		results.addAll(back);
		unique(results);
		return results;
	}
	
	public Nodes addBack(String filter) {
		if (filter == null || back == null || back.isEmpty() || back == this) {
			return new Nodes(this, this);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size() * 2);
		results.addAll(this);
		for (Node node : back) {
			result = (XPathResult)expr.evaluate(node, XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) results.add(node);
		}
		unique(results);
		return results;
	}
	
	public Nodes end() {
		if (back == null) {
			return new Nodes(getOwner(), null, 0);
		}
		return back;
	}
	
	public Nodes find(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression(xpath, owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.UNORDERED_NODE_ITERATOR_TYPE, result);
			Node node;
			while ((node = result.iterateNext()) != null) {
				if (node.getNodeType() != Node.ELEMENT_NODE) continue;
				results.add(node);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes filter(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) results.add(self);
		}
		unique(results);
		return results;
	}
	
	public Nodes filter(Filter<Node> filter) {
		if (filter == null || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			if (filter.accept(self)) results.add(self);
		}
		unique(results);
		return results;
	}
	
	public Nodes not(String filter) {
		if (filter == null || filter.isEmpty()) {
			return new Nodes(this, this);
		} else if (isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.BOOLEAN_TYPE, result);
			if (!result.getBooleanValue()) results.add(self);
		}
		unique(results);
		return results;
	}
	
	public Nodes parent() {
		return parentsInternal(SelectMode.FIRST);
	}
	
	public Nodes parent(String filter) {
		return parentsInternal(filter, SelectMode.FIRST);
	}
	
	public Nodes parentsUntil(String filter) {
		return parentsInternal(filter, SelectMode.UNTIL);
	}
	
	public Nodes parents() {
		return parentsInternal(SelectMode.ALL);
	}
	
	public Nodes parents(String filter) {
		return parentsInternal(filter, SelectMode.ALL);
	}
	
	private Nodes parentsInternal(SelectMode mode) {
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node parent = self;
			while ((parent = parent.getParentNode()) != null) {
				if (parent.getNodeType() != Node.ELEMENT_NODE) break;
				results.add(parent);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	private Nodes parentsInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node parent = self;
			while ((parent = parent.getParentNode()) != null) {
				if (parent.getNodeType() != Node.ELEMENT_NODE) break;
				
				result = (XPathResult)expr.evaluate(parent, XPathResult.BOOLEAN_TYPE, result);
				if (mode == SelectMode.UNTIL) {
					results.add(parent);
					if (result.getBooleanValue()) break;
				} else if (result.getBooleanValue()) {
					results.add(parent);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	public Nodes closest(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node current = self;
			do {
				if (current.getNodeType() != Node.ELEMENT_NODE) break;
				
				result = (XPathResult)expr.evaluate(current, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) {
					results.add(current);
					break;
				}
			} while ((current = current.getParentNode()) != null);
		}
		unique(results);
		return results;
	}
	
	public Nodes children() {
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(child);
			}
		}
		return results;
	}
	
	public Nodes children(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				
				result = (XPathResult)expr.evaluate(child, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) results.add(child);
			}
		}
		return results;
	}
	
	public Nodes contents() {
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				
				results.add(child);
			}
		}
		return results;
	}
	
	public Nodes contents(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				
				result = (XPathResult)expr.evaluate(child, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) results.add(child);
			}
		}
		return results;
	}
	
	public Nodes first() {
		return eq(0);
	}
	
	public Nodes last() {
		return eq(-1);
	}
	
	public Nodes prev() {
		return prevInternal(SelectMode.FIRST);
	}
	
	public Nodes prev(String filter) {
		return prevInternal(filter, SelectMode.FIRST);
	}
	
	public Nodes prevUntil(String filter) {
		return prevInternal(filter, SelectMode.UNTIL);
	}
	
	public Nodes prevAll() {
		return prevInternal(SelectMode.ALL);
	}
	
	public Nodes prevAll(String filter) {
		return prevInternal(filter, SelectMode.ALL);
	}
	
	private Nodes prevInternal(SelectMode mode) {
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				results.add(prev);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	private Nodes prevInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;

				result = (XPathResult)expr.evaluate(prev, XPathResult.BOOLEAN_TYPE, result);
				if (mode == SelectMode.UNTIL) {
					results.add(prev);
					if (result.getBooleanValue()) break;
				} else if (result.getBooleanValue()) {
					results.add(prev);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	public Nodes next() {
		return nextInternal(SelectMode.FIRST);
	}
	
	public Nodes next(String filter) {
		return nextInternal(filter, SelectMode.FIRST);
	}
	
	public Nodes nextUntil(String filter) {
		return nextInternal(filter, SelectMode.UNTIL);
	}
	
	public Nodes nextAll() {
		return nextInternal(SelectMode.ALL);
	}
	
	public Nodes nextAll(String filter) {
		return nextInternal(filter, SelectMode.ALL);
	}
	
	private Nodes nextInternal(SelectMode mode) {
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(next);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
		}
		return results;
	}
	
	private Nodes nextInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;
				
				result = (XPathResult)expr.evaluate(next, XPathResult.BOOLEAN_TYPE, result);
				if (mode == SelectMode.UNTIL) {
					results.add(next);
					if (result.getBooleanValue()) break;
				} else if (result.getBooleanValue()) {
					results.add(next);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
		}
		return results;
	}
	
	public Nodes siblings() {
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(prev);
			}
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(next);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes siblings(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.nsResolver);
		XPathResult result = null;
		
		Nodes results = new Nodes(this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				
				result = (XPathResult)expr.evaluate(prev, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) results.add(prev);
			}
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;

				result = (XPathResult)expr.evaluate(next, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) results.add(next);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes slice(int start) {
		return slice(start, size());
	}
	
	public Nodes slice(int start, int end) {
		if (start < 0) start = size() + start;
		if (end < 0) end = size() + end;
		
		if (start < 0 || start >= size() || end <= 0 || end > size()) {
			return new Nodes(this, 0);
		}
		
		Nodes results = new Nodes(this, end-start);
		int pos = 0;
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			if (pos >= start && pos < end) results.add(self);
			pos++;
		}
		unique(results);
		return results;
	}
	
	public Nodes prepend(String xml) {
		if (xml == null || xml.isEmpty()) return this;
		
		return prepend(getOwner().parse(xml));
	}
	
	public Nodes prepend(Nodes nodes) {
		if (nodes == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node ref = self.getFirstChild();
			for (Node node : nodes) {
				if (node == null) continue;
				if (node.getParentNode() != null) {
					node = node.cloneNode(true);
				}
				if (ref != null) {
					self.insertBefore(node, ref);
				} else {
					self.appendChild(node);
				}
			}
		}
		return this;
	}
	
	public Nodes prependTo(String xpath) {
		return prependTo(getOwner().find(xpath));
	}
	
	public Nodes prependTo(Nodes nodes) {
		if (nodes == null) return new Nodes(this, 0);
		
		Nodes results = new Nodes(this, nodes.size());
		for (Node node : nodes) {
			if (node == null) continue;
			if (node.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node ref = node.getFirstChild();
			for (Node self : this) {
				if (self == null) continue;
				if (self.getParentNode() != null) {
					self = self.cloneNode(true);
				}
				results.add(self);
				if (ref != null) {
					node.insertBefore(self, ref);
				} else {
					node.appendChild(self);
				}
			}
		}
		return results;
	}
	
	public Nodes append(String xml) {
		if (xml == null || xml.isEmpty()) return this;
		
		return append(getOwner().parse(xml));
	}
	
	public Nodes append(Nodes nodes) {
		if (nodes.isEmpty()) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;

			for (Node node : nodes) {
				if (node == null) continue;
				if (node.getParentNode() != null) {
					node = node.cloneNode(true);
				}
				self.appendChild(node);
			}
		}
		return this;
	}
	
	public Nodes appendTo(String xpath) {
		return appendTo(getOwner().find(xpath));
	}
	
	public Nodes appendTo(Nodes nodes) {
		if (nodes == null) return new Nodes(this, 0);
		
		Nodes result = new Nodes(this, nodes.size());
		for (Node node : nodes) {
			if (node == null) continue;
			if (node.getNodeType() != Node.ELEMENT_NODE) continue;

			for (Node self : this) {
				if (self == null) continue;
				if (self.getParentNode() != null) {
					self = self.cloneNode(true);
				}
				result.add(self);
				node.appendChild(self);
			}
		}
		return result;
	}
	
	public Nodes before(String xml) {
		return before(getOwner().parse(xml));
	}
	
	public Nodes before(Nodes nodes) {
		if (nodes == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			for (Node node : nodes) {
				if (node == null) continue;
				if (node.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (node.getParentNode() != null) {
					node = node.cloneNode(true);
				}
				parent.insertBefore(node, self);
			}
		}
		return this;
	}
	
	public Nodes insertBefore(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return this;
		}
		
		getOwner().find(xpath).before(this);
		return this;
	}
	
	public Nodes insertBefore(Nodes nodes) {
		if (nodes == null || nodes.isEmpty() || isEmpty()) {
			return this;
		}
		
		nodes.before(this);
		return this;
	}
	
	public Nodes after(String xml) {
		return after(getOwner().parse(xml));
	}
	
	public Nodes after(Nodes nodes) {
		if (nodes == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;

			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node next = self.getNextSibling();
			for (Node node : nodes) {
				if (node.getParentNode() != null) {
					node = node.cloneNode(true);
				}
				if (next != null) {
					parent.insertBefore(node, next);
				} else {
					parent.appendChild(node);
				}
			}
		}
		return this;
	}
	
	public Nodes insertAfter(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return this;
		}
		
		getOwner().find(xpath).after(this);
		return this;
	}
	
	public Nodes insertAfter(Nodes nodes) {
		if (nodes == null || nodes.isEmpty() || isEmpty()) {
			return this;
		}
		
		nodes.after(this);
		return this;
	}
	
	public Nodes wrap(String xml) {
		return wrap(getOwner().parse(xml));
	}
	
	public Nodes wrap(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return this;
		if (nodes.get(0) == null || nodes.get(0).getNodeType() != Node.ELEMENT_NODE) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node root = nodes.get(0).cloneNode(true);
			Node leaf = getFirstLeaf(root);
			leaf.appendChild(parent.replaceChild(root, self));
		}
		return this;
	}
	
	public Nodes wrapInner(String xml) {
		return wrapInner(getOwner().parse(xml));
	}
	
	public Nodes wrapInner(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return this;
		if (nodes.get(0) == null || nodes.get(0).getNodeType() != Node.ELEMENT_NODE) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node root = nodes.get(0).cloneNode(true);
			Node leaf = getFirstLeaf(root);
			while (self.hasChildNodes()) {
				leaf.appendChild(self.getFirstChild());
			}
			self.appendChild(root);
		}
		return this;
	}
	
	public Nodes wrapAll(String xml) {
		return wrapAll(getOwner().parse(xml));
	}
	
	public Nodes wrapAll(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return this;
		if (nodes.get(0) == null || nodes.get(0).getNodeType() != Node.ELEMENT_NODE) return this;
		if (isEmpty() || get(0) == null) return this;
		
		Node root = nodes.get(0);
		if (root.getParentNode() != null) {
			root = root.cloneNode(true);
		}
		Node leaf = getFirstLeaf(root);
		
		Node parent = get(0).getParentNode();
		if (parent == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;	
			
			if (root.getParentNode() == null) {
				leaf.appendChild(parent.replaceChild(root, self));
			} else {
				leaf.appendChild(self);
			}
		}
		return this;
	}
	
	public Nodes unwrap() {
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;

			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node next = self.getNextSibling();
			parent.removeChild(self);
			while (self.hasChildNodes()) {
				Node child = self.getFirstChild();
				if (next != null) {
					parent.insertBefore(child, next);
				} else {
					parent.appendChild(child);
				}
			}
		}
		return this;
	}
	
	public Nodes replaceWith(String xml) {
		return replaceWith(getOwner().parse(xml));
	}
	
	public Nodes replaceWith(Nodes nodes) {
		if (nodes == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node next = self.getNextSibling();
			parent.removeChild(self);
			
			for (Node node : nodes) {
				if (node.getParentNode() != null) {
					node = node.cloneNode(true);
				}
				if (next != null) {
					parent.insertBefore(node, next);
				} else {
					parent.appendChild(node);
				}
			}
		}
		return this;
	}
	
	public Nodes replaceAll(String xpath) {
		return replaceAll(getOwner().find(xpath));
	}
	
	public Nodes replaceAll(Nodes nodes) {
		if (nodes == null) return new Nodes(back, 0);
		
		Nodes results = new Nodes(back, size());
		for (Node node : nodes) {
			if (node == null) continue;
			if (node.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = node.getParentNode();
			if (parent == null) continue;
			
			Node next = node.getNextSibling();
			parent.removeChild(node);
			for (Node self : this) {
				if (self.getParentNode() != null) {
					self = self.cloneNode(true);
				}
				if (next != null) {
					parent.insertBefore(self, next);
				} else {
					parent.appendChild(self);
				}
				results.add(self);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes empty() {
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			while (self.hasChildNodes()) {
				self.removeChild(self.getLastChild());
			}
		}
		return this;
	}

	public Nodes remove() {
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			parent.removeChild(self);
		}
		return this;
	}
	
	public Nodes remove(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return this;
		}
			
		XPathExpression expr = DOMFactory.createXPathExpression(xpath, owner.nsResolver);
		XPathResult result = null;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			result = (XPathResult)expr.evaluate(self, XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE, result);
			for (int i = 0; i < result.getSnapshotLength(); i++) {
				Node node = result.snapshotItem(i);
				Node parent = node.getParentNode();
				if (parent == null) continue;
				
				parent.removeChild(node);
			}
		}
		return this;
	}
	
	public Nodes clone() {
		Nodes clone = new Nodes(getOwner(), back, size());
		clone.addAll(this);
		return clone;
	}
	
	public String xml() {
		if (isEmpty()) return "";
		
		LSSerializer serializer = DOMFactory.createLSSerializer();
		DOMConfiguration conf = serializer.getDomConfig();
		conf.setParameter("format-pretty-print", false);
		conf.setParameter("xml-declaration", false);
		NodeList nodes = this.get(0).getChildNodes();
		if (nodes.getLength() == 0) return "";

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nodes.getLength(); i++) {
			sb.append(serializer.writeToString(nodes.item(i)));
		}
		return sb.toString();
	}
	
	public Nodes xml(String xml) {
		empty().append(xml);
		return this;
	}
	
	public String text() {
		if (isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();
		for (Node self : this) {
			if (self instanceof Document) {
				self = ((Document)self).getDocumentElement();
			}
			
			if (self instanceof Element) {
				sb.append(self.getTextContent());
			}
		}
		return sb.toString();
	}
	
	public Nodes text(String text) {
		for (Node self : this) {
			self.setTextContent(text);
		}
		return this;
	}
	
	@Override
	public String toString() {
		if (isEmpty()) return "";
		
		LSSerializer serializer = DOMFactory.createLSSerializer();
		DOMConfiguration conf = serializer.getDomConfig();
		conf.setParameter("format-pretty-print", false);
		conf.setParameter("xml-declaration", false);
		StringBuilder sb = new StringBuilder();
		for (Node node : this) {
			sb.append(serializer.writeToString(node));
		}
		return sb.toString();
	}
	
	private boolean isExternalNode(Node node) {
		if (node == null) return false;
		if (node instanceof Document) return false;
		if (node.getOwnerDocument() == getOwner().doc) return false;
		
		return true;
	}
	
	private static Node getFirstLeaf(Node node) {
		if (!node.hasChildNodes()) return node;
		
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child == null) continue;
			
			if (child.hasChildNodes()) {
				return getFirstLeaf(child);
			} else {
				return child;
			}
		}
		return null;
	}
	
	public static void unique(final Nodes nodes) {
		if (nodes.size() < 2) return;
		
		TreeSet<Node> uniqueSet = new TreeSet<Node>(new Comparator<Node>() {
			@Override
			public int compare(Node a, Node b) {
				if (a == b) return 0;
				if (a == null) return -1;
				if (b == null) return 1;
				
				short compare = a.compareDocumentPosition(b);
				if (compare != 0) {
					if ((compare & Node.DOCUMENT_POSITION_DISCONNECTED) != 0
							|| (!DOMFactory.isSortDetachedSupported() &&  b.compareDocumentPosition(a) == compare)) {
						if (a instanceof Document || contains(nodes.getOwner().doc, a)) {
							return -1;
						}
						if (b instanceof Document || contains(nodes.getOwner().doc, b)) {
							return 1;
						}
						return 0;
					}
					return (compare & Node.DOCUMENT_POSITION_FOLLOWING) != 0 ? -1 : 1;
				}
				return 0;
			}
		});
		uniqueSet.addAll(nodes);
		nodes.clear();
		nodes.addAll(uniqueSet);
	}
	
	private static boolean contains(Node a, Node b) {
		Node bup = (b != null) ? b.getParentNode() : null;
		return (a == bup || (bup != null && bup instanceof Element 
				&& (a.compareDocumentPosition(bup) & Node.DOCUMENT_POSITION_CONTAINED_BY) != 0));
	}
}
