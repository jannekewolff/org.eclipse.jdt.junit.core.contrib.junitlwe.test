package junit.lwe.submodule;

import junit.framework.TestCase;

import org.junit.Test;

public class TP3 extends TestCase {
	@Test
	public void testSetStr3() {
		B b = new B();
		b.setStr("set");
		assertEquals(b.getStr(), "get");
	}
}
