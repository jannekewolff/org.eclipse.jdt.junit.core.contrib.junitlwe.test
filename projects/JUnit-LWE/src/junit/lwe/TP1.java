package junit.lwe;

import junit.framework.TestCase;

import org.junit.Test;

public class TP1 extends TestCase {
	@Test
	public void testGetStr1() {
		A a= new A();
		a.setStr("get");
		assertEquals(a.getStr(), "get");
	}

	@Test
	public void testSetStr1() {
		A a= new A();
		a.setStr("set");
		assertEquals(a.getStr(), "get");
	}
}
