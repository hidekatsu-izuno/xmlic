package net.arnx.xmlic;

import static org.junit.Assert.*;

import java.io.IOException;

import net.arnx.xmlic.Filter;
import net.arnx.xmlic.XML;

import org.junit.Test;
import org.w3c.dom.Node;

public class XMLTest {
	@Test
	public void testToString() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
		assertEquals("<li>t1</li><li>t2</li><li>t3</li><li>t4</li><li>t5</li><li>t6</li><li>t7</li><li>t8</li><li>t9</li>", xml.find("//li").toString());
	}
	
	@Test
	public void testAdd() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t2</li><li>t4</li><li>t7</li>", xml.find("(//li[position()=2])[1]").add("//li[position()=1]").toString());
	}
	
	@Test
	public void testAddBack() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t2</li><li>t4</li><li>t5</li><li>t7</li><li>t8</li>", xml.find("//li[position()=1]").next().addBack().toString());
	}
	
	@Test
	public void testAfter() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").after("<span>after1</span><span>after2</span>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li><span>after1</span><span>after2</span>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li><span>after1</span><span>after2</span>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li><span>after1</span><span>after2</span>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
	}
	
	@Test
	public void testAppend() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1<span>append1</span><span>append2</span></li><li>t4<span>append1</span><span>append2</span></li><li>t7<span>append1</span><span>append2</span></li>", xml.find("//li[position()=1]").append("<span>append1</span><span>append2</span>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1<span>append1</span><span>append2</span></li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4<span>append1</span><span>append2</span></li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7<span>append1</span><span>append2</span></li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
	}
	
	@Test
	public void testAppendTo() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").appendTo("/body").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n<li>t1</li><li>t4</li><li>t7</li></body>", xml.toString());		
	}
	
	@Test
	public void testAttr() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("s11", xml.find("//ul[position()=1]").attr("class"));
		assertEquals("<ul class=\"s11\" id=\"x12\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><ul class=\"s21\" id=\"x12\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul[position()=1]").attr("id", "x12").toString());
	}
	
	@Test
	public void testBefore() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").before("<span>before1</span><span>before2</span>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<span>before1</span><span>before2</span><li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<span>before1</span><span>before2</span><li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<span>before1</span><span>before2</span><li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
	}	
	
	@Test
	public void testChildren() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("", xml.find("//li[position()=1]").children().toString());
		assertEquals("<li>t1</li><li>t2</li><li>t3</li><li>t7</li><li>t8</li><li>t9</li>", xml.find("//ul[position()=1]").children().toString());
		assertEquals("<li>t8</li>", xml.find("//ul[position()=1]").children("text()='t8'").toString());
	}
	
	@Test
	public void testClone() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.clone().toString());	
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").clone().toString());
	}
	
	@Test
	public void testClosest() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("", xml.find("//li[position()=1]").closest("local-name()='p'").toString());
		assertEquals("<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div><div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>", xml.find("//li[position()=1]").closest("local-name()='div'").toString());
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").closest("local-name()='li'").toString());
	}
	
	@Test
	public void testContents() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("t1t4t7", xml.find("//li[position()=1]").contents().toString());
		assertEquals("\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t", xml.find("//ul[position()=1]").contents().toString());
		assertEquals("<li>t8</li>", xml.find("//ul[position()=1]").contents("text()='t8'").toString());
	}
	
	@Test
	public void testEnd() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("", xml.document().end().toString());		
		assertEquals("", xml.document().end().end().toString());		
		assertEquals("<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul[position()=1]").children().end().toString());
	}
	
	@Test
	public void testEq() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li>", xml.find("//li[position()=1]").eq(0).toString());		
		assertEquals("<li>t1</li>", xml.find("//li[position()=1]").eq(-3).toString());		
		assertEquals("<li>t7</li>", xml.find("//li[position()=1]").eq(2).toString());		
		assertEquals("<li>t7</li>", xml.find("//li[position()=1]").eq(-1).toString());		
		assertEquals("", xml.find("//li[position()=1]").eq(3).toString());		
		assertEquals("", xml.find("//li[position()=1]").eq(-4).toString());		
		assertEquals("<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul[position()=1]").eq(-1).toString());
	}
	
	@Test
	public void testFilter() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t4</li>", xml.find("//li[position()=1]").filter("text()='t4'").toString());
		assertEquals("<li>t4</li>", xml.find("//li[position()=1]").filter(new Filter<Node>() {
			@Override
			public boolean accept(Node t) {
				return "t4".equals(t.getTextContent());
			};
		}).toString());
		assertEquals("<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul[position()=1]").filter("@class='s21'").toString());
	}
	
	@Test
	public void testFind() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("body").find("div").find("//li[position()=1]").toString());
		assertEquals("<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>", xml.find("//li[position()=1]").find("(../../ul)[2]").toString());
	}
	
	@Test
	public void testFirst() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li>", xml.find("//li[position()=1]").first().toString());
	}
	
	@Test
	public void testGet() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li>", xml.createNodes(xml.find("//li[position()=1]").get(0)).toString());
		assertEquals("<li>t1</li>", xml.createNodes(xml.find("//li[position()=1]").get(-3)).toString());
		assertEquals("<li>t7</li>", xml.createNodes(xml.find("//li[position()=1]").get(2)).toString());
		assertEquals("<li>t7</li>", xml.createNodes(xml.find("//li[position()=1]").get(-1)).toString());
		assertNull(xml.find("//li[position()=1]").get(3));
		assertNull(xml.find("//li[position()=1]").get(-4));
	}
	
	@Test
	public void testHas() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>", xml.find("//ul").has("text()='t2'").toString());
	}
	
	@Test
	public void testIndex() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals(4, xml.find("//li").index(xml.find("//li[text()='t5']")));
		assertEquals(4, xml.find("//li").index("text()='t5'"));
	}
	
	@Test
	public void testInsertAfter() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").insertAfter("(//ul)[position() <= 2]").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><li>t1</li><li>t4</li><li>t7</li>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul><li>t1</li><li>t4</li><li>t7</li>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
	}	
	
	@Test
	public void testInsertBefore() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").insertBefore("(//ul)[position() <= 2]").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<li>t1</li><li>t4</li><li>t7</li><ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<li>t1</li><li>t4</li><li>t7</li><ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
	}
	
	@Test
	public void testIs() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals(true, xml.find("//li[position()=1]").is("position()=1"));
		assertEquals(false, xml.find("//li[position()=1]").is("position()=2"));
	}
	
	@Test
	public void testLast() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t7</li>", xml.find("//li[position()=1]").last().toString());
	}
	
	@Test
	public void testLocalName() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("li", xml.find("//li[position()=1]").localName());
	}
	
	@Test
	public void testNamespaceURI() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertNull(xml.find("//li[position()=1]").namespaceURI());
	}
	
	@Test
	public void testNext() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t2</li><li>t5</li><li>t8</li>", xml.find("//li[position()=1]").next().toString());
		assertEquals("<li>t8</li>", xml.find("//li[position()=1]").next("text()='t8'").toString());
	}
	
	@Test
	public void testNextAll() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t2</li><li>t3</li><li>t5</li><li>t6</li><li>t8</li><li>t9</li>", xml.find("//li[position()=1]").nextAll().toString());
		assertEquals("<li>t6</li><li>t8</li><li>t9</li>", xml.find("//li[position()=1]").nextAll("translate(text(), 't', '0')>='6'").toString());
	}
	
	@Test
	public void testNextUntil() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t2</li><li>t3</li><li>t5</li><li>t8</li><li>t9</li>", xml.find("//li[position()=1]").nextUntil("text()='t5'").toString());
	}
	
	@Test
	public void testNot() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t7</li>", xml.find("//li[position()=1]").not("text()='t4'").toString());
	}
	
	@Test
	public void testParent() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul><ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//li[position()=1]").parent().toString());
		assertEquals("<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>", xml.find("//li[position()=1]").parent("@class='s12'").toString());
	}
	
	@Test
	public void testParents() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div><body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.find("//ul").first().parents().toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.find("//ul").first().parents("name()='body'").toString());
	}	
	
	@Test
	public void testParentsUntil() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>", xml.find("//li").first().parentsUntil("name()='div'").toString());
	}	
	
	@Test
	public void testPrefix() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertNull(xml.find("//li[position()=1]").prefix());
	}
	
	@Test
	public void testPrepend() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li><span>prepend1</span><span>prepend2</span>t1</li><li><span>prepend1</span><span>prepend2</span>t4</li><li><span>prepend1</span><span>prepend2</span>t7</li>", xml.find("//li[position()=1]").prepend("<span>prepend1</span><span>prepend2</span>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li><span>prepend1</span><span>prepend2</span>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li><span>prepend1</span><span>prepend2</span>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li><span>prepend1</span><span>prepend2</span>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
	}
	
	@Test
	public void testPrependTo() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t4</li><li>t7</li>", xml.find("//li[position()=1]").prependTo("/body").toString());
		assertEquals("<body><li>t1</li><li>t4</li><li>t7</li>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());		
	}
	
	@Test
	public void testPrev() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t2</li><li>t5</li><li>t8</li>", xml.find("//li[position()=3]").prev().toString());
		assertEquals("<li>t8</li>", xml.find("//li[position()=3]").prev("text()='t8'").toString());
	}
	
	@Test
	public void testPrevAll() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t8</li><li>t7</li><li>t5</li><li>t4</li><li>t2</li><li>t1</li>", xml.find("//li[position()=3]").prevAll().toString());
		assertEquals("<li>t8</li><li>t7</li>", xml.find("//li[position()=3]").prevAll("translate(text(), 't', '0')>='6'").toString());
	}
	
	@Test
	public void testPrevUntil() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t8</li><li>t7</li><li>t5</li><li>t2</li><li>t1</li>", xml.find("//li[position()=3]").prevUntil("text()='t5'").toString());
	}
	
	@Test
	public void testRemove() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul><ul class=\"s21\">\n\t\t\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul").remove("li[text()='t7']").toString());
		assertEquals("<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul><ul class=\"s21\">\n\t\t\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul").remove().toString());
		assertEquals("", xml.find("//ul").toString());
	}
	
	@Test
	public void testRemoveAttr() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ul>\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><ul>\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul><ul>\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul").removeAttr("class").toString());
	}
	
	@Test
	public void testReplaceAll() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ol><li>replace</li></ol><ol><li>replace</li></ol><ol><li>replace</li></ol>", xml.createNodes("<ol><li>replace</li></ol>").replaceAll("//ul").toString());
		assertEquals("", xml.createNodes("<ol><li>replace</li></ol>").replaceAll("").toString());
	}
	
	@Test
	public void testReplaceWith() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul><ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul><ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>", xml.find("//ul").replaceWith("<ol><li>replace</li></ol>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ol><li>replace</li></ol>\n\t<ol><li>replace</li></ol>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ol><li>replace</li></ol>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());
		assertEquals("<ol><li>replace</li></ol><ol><li>replace</li></ol><ol><li>replace</li></ol>", xml.find("//ol").replaceWith("").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t\n\t\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());
	}
	
	@Test
	public void testSiblings() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t3</li><li>t4</li><li>t6</li><li>t7</li><li>t9</li>", xml.find("//li[position()=2]").siblings().toString());
		assertEquals("<li>t3</li>", xml.find("//li[position()=2]").siblings("text()='t3'").toString());
	}
		
	@Test
	public void testText() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("\ntop\n\n\t\n\t\tt1\n\t\tt2\n\t\tt3\n\t\n\t\n\t\tt4\n\t\tt5\n\t\tt6\n\t\n\nmiddle\n\n\tprefix\n\t\n\t\tt7\n\t\tt8\n\t\tt9\n\t\n\tsuffix\n\nbottom\n", xml.document().text());		
		assertEquals("t1t2t3t4t5t6t7t8t9", xml.find("//li").text());		
		assertEquals("<li>text</li><li>text</li><li>text</li><li>text</li><li>text</li><li>text</li><li>text</li><li>text</li><li>text</li>", xml.find("//li").text("text").toString());		
	}
		
	@Test
	public void testUnwrap() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<ul class=\"s11\"/><ul class=\"s12\"/><ul class=\"s21\"/>", xml.find("//ul").unwrap().toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t\n\t\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());
	}
	
	@Test
	public void testWrap() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t2</li><li>t3</li><li>t4</li><li>t5</li><li>t6</li><li>t7</li><li>t8</li><li>t9</li>", xml.find("//li").wrap("<div class=\"w1\"><div class=\"w11\"></div></div><div class=\"w2\"></div>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t1</li></div></div>\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t2</li></div></div>\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t3</li></div></div>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t4</li></div></div>\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t5</li></div></div>\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t6</li></div></div>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t7</li></div></div>\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t8</li></div></div>\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t9</li></div></div>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());
	}
	
	@Test
	public void testWrapAll() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li>t1</li><li>t2</li><li>t3</li><li>t4</li><li>t5</li><li>t6</li><li>t7</li><li>t8</li><li>t9</li>", xml.find("//li").wrapAll("<div class=\"w1\"><div class=\"w11\"></div></div><div class=\"w2\"></div>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<div class=\"w1\"><div class=\"w11\"><li>t1</li><li>t2</li><li>t3</li><li>t4</li><li>t5</li><li>t6</li><li>t7</li><li>t8</li><li>t9</li></div></div>\n\t\t\n\t\t\n\t</ul>\n\t<ul class=\"s12\">\n\t\t\n\t\t\n\t\t\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t\n\t\t\n\t\t\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());
	}
	
	@Test
	public void testWrapInner() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("<li><div class=\"w1\"><div class=\"w11\">t1</div></div></li><li><div class=\"w1\"><div class=\"w11\">t2</div></div></li><li><div class=\"w1\"><div class=\"w11\">t3</div></div></li><li><div class=\"w1\"><div class=\"w11\">t4</div></div></li><li><div class=\"w1\"><div class=\"w11\">t5</div></div></li><li><div class=\"w1\"><div class=\"w11\">t6</div></div></li><li><div class=\"w1\"><div class=\"w11\">t7</div></div></li><li><div class=\"w1\"><div class=\"w11\">t8</div></div></li><li><div class=\"w1\"><div class=\"w11\">t9</div></div></li>", xml.find("//li").wrapInner("<div class=\"w1\"><div class=\"w11\"></div></div><div class=\"w2\"></div>").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li><div class=\"w1\"><div class=\"w11\">t1</div></div></li>\n\t\t<li><div class=\"w1\"><div class=\"w11\">t2</div></div></li>\n\t\t<li><div class=\"w1\"><div class=\"w11\">t3</div></div></li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li><div class=\"w1\"><div class=\"w11\">t4</div></div></li>\n\t\t<li><div class=\"w1\"><div class=\"w11\">t5</div></div></li>\n\t\t<li><div class=\"w1\"><div class=\"w11\">t6</div></div></li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li><div class=\"w1\"><div class=\"w11\">t7</div></div></li>\n\t\t<li><div class=\"w1\"><div class=\"w11\">t8</div></div></li>\n\t\t<li><div class=\"w1\"><div class=\"w11\">t9</div></div></li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.toString());
	}
	
	@Test
	public void testXml() throws IOException {
		XML xml = XML.load(getClass().getResource("test.xml"));
		assertEquals("t1", xml.find("//li").xml());
		assertEquals("<li><span>xml</span></li><li><span>xml</span></li><li><span>xml</span></li><li><span>xml</span></li><li><span>xml</span></li><li><span>xml</span></li><li><span>xml</span></li><li><span>xml</span></li><li><span>xml</span></li>", xml.find("//li").xml("<span>xml</span>").toString());
		assertEquals("<li>xml</li><li>xml</li><li>xml</li><li>xml</li><li>xml</li><li>xml</li><li>xml</li><li>xml</li><li>xml</li>", xml.find("//li").xml("xml").toString());
	}

}
