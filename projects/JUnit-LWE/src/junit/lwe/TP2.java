package junit.lwe;

import junit.framework.TestCase;

import org.junit.Test;

public class TP2 extends TestCase {
	@Test
	public void testSetStr2() {
		A a = new A();
		a.setStr("set");
		assertEquals(a.getStr(), "get");
	}
}
