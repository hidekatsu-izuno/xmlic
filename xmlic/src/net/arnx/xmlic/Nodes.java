package net.arnx.xmlic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

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
	
	Nodes(Nodes back, int size) {
		super(size);
		if (this instanceof XML) {
			this.back = (XML)this;
			this.owner = (XML)this;
		} else {
			this.back = back;
			this.owner = back.getOwner();
		}
	}
	
	Nodes(Nodes back, Node node) {
		this(back, 1);
		add(node);
	}
	
	Nodes(Nodes back, Nodes nodes) {
		this(back, nodes.size());
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
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
			
			return ((Element)self).getNamespaceURI();
		}
		return null;
	}
	
	public String prefix() {
		for (Node self : this) {
			if (!(self instanceof Element)) continue;

			return ((Element)self).getPrefix();
		}
		return null;
	}
	
	public String localName() {
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
			
			return ((Element)self).getLocalName();
		}
		return null;
	}
	
	public String attr(String name) {
		if (name == null) throw new IllegalArgumentException("name is null");
		
		String uri = null;
		String localName = null;
		
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
			
			if (localName == null) {
				int index = name.indexOf(':');
				if (index != -1) {
					if (index == 0 || index + 1 >= name.length()) return null; 
					localName = name.substring(index + 1);
					if (localName.isEmpty()) return null;
					
					uri = getOwner().getXPathNSResolver().lookupNamespaceURI(name.substring(0, index));
					if (uri == null) return null;
				} else {
					localName = name;
					if (localName.isEmpty()) return null;
				}
			}
			
			if (uri != null) {
				return ((Element)self).getAttributeNS(uri, localName);
			} else {
				return ((Element)self).getAttribute(localName);
			}
		}
		return null;
	}
	
	public Nodes attr(String name, String value) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (value == null) value = "";
		
		String uri = null;
		String localName = null;
		
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
			
			if (localName == null) {
				int index = name.indexOf(':');
				if (index != -1) {
					if (index == 0 || index + 1 >= name.length()) return null; 
					localName = name.substring(index + 1);
					if (localName.isEmpty()) return null;
					
					uri = getOwner().getXPathNSResolver().lookupNamespaceURI(name.substring(0, index));
					if (uri == null) return null;
				} else {
					localName = name;
					if (localName.isEmpty()) return null;
				}
			}
			
			if (uri != null) {
				((Element)self).setAttributeNS(uri, localName, value);
			} else {
				((Element)self).setAttribute(localName, value);
			}
		}
		return this;
	}
	
	public Nodes removeAttr(String name) {
		if (name == null) return this;
		
		String uri = null;
		String localName = null;
		
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
			
			if (localName == null) {
				int index = name.indexOf(':');
				if (index != -1) {
					if (index == 0 || index + 1 >= name.length()) return null; 
					localName = name.substring(index + 1);
					if (localName.isEmpty()) return null;
					
					uri = getOwner().getXPathNSResolver().lookupNamespaceURI(name.substring(0, index));
					if (uri == null) return null;
				} else {
					localName = name;
					if (localName.isEmpty()) return null;
				}
			}
						
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
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		for (Node node : this) {
			result = (XPathResult)expr.evaluate(node, XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) return true;
		}
		return false;
	}
	
	public int index(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) return -1;
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
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
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(child::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) nodes.add(self);
		}
		unique(nodes);
		return nodes;
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
		
		XPathExpression expr = DOMFactory.createXPathExpression(xpath, owner.getXPathNSResolver());
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
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
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
		return back;
	}
	
	public Nodes find(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression(xpath, owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.UNORDERED_NODE_ITERATOR_TYPE, result);
			Node node;
			while ((node = result.iterateNext()) != null) {
				if (node.getNodeType() != Node.ELEMENT_NODE) continue;
				nodes.add(node);
			}
		}
		unique(nodes);
		return nodes;
	}
	
	public Nodes filter(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.BOOLEAN_TYPE, result);
			if (result.getBooleanValue()) nodes.add(self);
		}
		unique(nodes);
		return nodes;
	}
	
	public Nodes filter(Filter<Node> filter) {
		if (filter == null || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			if (filter.accept(self)) nodes.add(self);
		}
		unique(nodes);
		return nodes;
	}
	
	public Nodes not(String filter) {
		if (filter == null || filter.isEmpty()) {
			return new Nodes(this, this);
		} else if (isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			result = (XPathResult)expr.evaluate(self, XPathResult.BOOLEAN_TYPE, result);
			if (!result.getBooleanValue()) nodes.add(self);
		}
		unique(nodes);
		return nodes;
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
		Nodes nodes = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node parent = self;
			while ((parent = parent.getParentNode()) != null) {
				if (parent.getNodeType() != Node.ELEMENT_NODE) break;
				nodes.add(parent);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && nodes.size() > 1) {
			unique(nodes);
			Collections.reverse(nodes);
		}
		return nodes;
	}
	
	private Nodes parentsInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node parent = self;
			while ((parent = parent.getParentNode()) != null) {
				if (parent.getNodeType() != Node.ELEMENT_NODE) break;
				
				result = (XPathResult)expr.evaluate(parent, XPathResult.BOOLEAN_TYPE, result);
				if (mode == SelectMode.UNTIL) {
					nodes.add(parent);
					if (result.getBooleanValue()) break;
				} else if (result.getBooleanValue()) {
					nodes.add(parent);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && nodes.size() > 1) {
			unique(nodes);
			Collections.reverse(nodes);
		}
		return nodes;
	}
	
	public Nodes closest(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node current = self;
			do {
				if (current.getNodeType() != Node.ELEMENT_NODE) break;
				
				result = (XPathResult)expr.evaluate(current, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) {
					nodes.add(current);
					break;
				}
			} while ((current = current.getParentNode()) != null);
		}
		unique(nodes);
		return nodes;
	}
	
	public Nodes children() {
		Nodes nodes = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				
				nodes.add(child);
			}
		}
		return nodes;
	}
	
	public Nodes children(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				
				result = (XPathResult)expr.evaluate(child, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) nodes.add(child);
			}
		}
		return nodes;
	}
	
	public Nodes contents() {
		Nodes nodes = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				
				nodes.add(child);
			}
		}
		return nodes;
	}
	
	public Nodes contents(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				
				result = (XPathResult)expr.evaluate(child, XPathResult.BOOLEAN_TYPE, result);
				if (result.getBooleanValue()) nodes.add(child);
			}
		}
		return nodes;
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
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				nodes.add(prev);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && nodes.size() > 1) {
			unique(nodes);
			Collections.reverse(nodes);
		}
		return nodes;
	}
	
	private Nodes prevInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(this, 0);
		}
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
		XPathResult result = null;
		
		Nodes nodes = new Nodes(this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;

				result = (XPathResult)expr.evaluate(prev, XPathResult.BOOLEAN_TYPE, result);
				if (mode == SelectMode.UNTIL) {
					nodes.add(prev);
					if (result.getBooleanValue()) break;
				} else if (result.getBooleanValue()) {
					nodes.add(prev);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && nodes.size() > 1) {
			unique(nodes);
			Collections.reverse(nodes);
		}
		return nodes;
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
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
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
		
		XPathExpression expr = DOMFactory.createXPathExpression("boolean(count(self::node()[" + filter + "]))", owner.getXPathNSResolver());
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
		
		return prepend(getOwner().createNodes(xml));
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
		
		return append(getOwner().createNodes(xml));
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
		return before(getOwner().createNodes(xml));
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
		getOwner().find(xpath).before(this);
		return this;
	}
	
	public Nodes insertBefore(Nodes nodes) {
		nodes.before(this);
		return this;
	}
	
	public Nodes after(String xml) {
		return after(getOwner().createNodes(xml));
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
		getOwner().find(xpath).after(this);
		return this;
	}
	
	public Nodes insertAfter(Nodes nodes) {
		nodes.after(this);
		return this;
	}
	
	public Nodes wrap(String xml) {
		return wrap(getOwner().createNodes(xml));
	}
	
	public Nodes wrap(Nodes nodes) {
		if (nodes == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node wrapper = nodes.get(0);
			if (wrapper.getParentNode() != null) {
				wrapper = wrapper.cloneNode(true);
			}
			
			Node leaf = wrapper;
			Node next = null;
			while ((next = getFirstElementChild(leaf)) != null) {
				leaf = next;
			}
			leaf.appendChild(parent.replaceChild(self, wrapper));
			break;
		}
		return this;
	}
	
	public Nodes wrapInner(String xml) {
		return wrapInner(getOwner().createNodes(xml));
	}
	
	public Nodes wrapInner(Nodes nodes) {
		if (nodes == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node wrapper = nodes.get(0);
			if (wrapper.getParentNode() != null) {
				wrapper = wrapper.cloneNode(true);
			}
			
			Node leaf = wrapper;
			Node next = null;
			while ((next = getFirstElementChild(leaf)) != null) {
				leaf = next;
			}
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				leaf.appendChild(children.item(i));
			}
			self.appendChild(wrapper);
			break;
		}
		return this;
	}
	
	public Nodes wrapAll(String xml) {
		return wrapAll(getOwner().createNodes(xml));
	}
	
	public Nodes wrapAll(Nodes nodes) {
		if (nodes == null) return this;
		
		Node leaf = null;
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			if (leaf == null) {
				Node wrapper = nodes.get(0);
				if (wrapper.getParentNode() != null) {
					wrapper = wrapper.cloneNode(true);
				}
				
				leaf = wrapper;
				Node next = null;
				while ((next = getFirstElementChild(leaf)) != null) {
					leaf = next;
				}
				leaf.appendChild(parent.replaceChild(self, wrapper));
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
			NodeList children = self.getChildNodes();
			parent.removeChild(self);
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (next != null) {
					self.insertBefore(child, next);
				} else {
					self.appendChild(child);
				}
			}
		}
		return this;
	}
	
	public Nodes replaceWith(String xml) {
		return replaceWith(getOwner().createNodes(xml));
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
			
		XPathExpression expr = DOMFactory.createXPathExpression(xpath, owner.getXPathNSResolver());
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
		if (this instanceof XML) {
			XML xml = (XML)this;
			return new XML((Document)xml.getDocument().cloneNode(true), xml.getXPathNSResolver());
		} else {
			return new Nodes(getOwner(), this);
		}
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
	
	private static Node getFirstElementChild(Node node) {
		if (node.hasChildNodes()) return null;
		
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child == null) continue;
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			
			return child;
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
						if (a instanceof Document || contains(nodes.getOwner().getDocument(), a)) {
							return -1;
						}
						if (b instanceof Document || contains(nodes.getOwner().getDocument(), b)) {
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
