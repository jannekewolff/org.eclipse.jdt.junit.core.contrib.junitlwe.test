package junit.lwe;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(TP1.class);
		suite.addTestSuite(TP2.class);
		//$JUnit-END$
		return suite;
	}
}
