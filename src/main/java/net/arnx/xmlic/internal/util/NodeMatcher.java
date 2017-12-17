package net.arnx.xmlic.internal.util;

import org.w3c.dom.Node;

public interface NodeMatcher {
	public enum MatchType {
		ANY_NODE,
		DOCUMENT_NODE,
		DOCUMENT_TYPE_NODE,
		NAMESPACE_NODE,
		ELEMENT_NODE,
		ATTRIBUTE_NODE,
		TEXT_NODE,
		PROCESSING_INSTRUCTION_NODE,
		COMMENT_NODE,
		UNKNOWN_NODE,
		NO_NODE
	}
	
	public MatchType getMatchType();
	public boolean match(Node node);
}
