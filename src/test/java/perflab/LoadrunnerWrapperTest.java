package perflab;

import perflab.LoadrunnerWrapper;
import junit.framework.TestCase;

public class LoadrunnerWrapperTest extends TestCase {
	LoadrunnerWrapper l;
	protected void setUp() throws Exception {		
		super.setUp();
		l = new LoadrunnerWrapper();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testParseSummaryFile() {
		assertNotNull("Test file missing", getClass().getResource("/summary.html"));
		l.parseSummaryFile("C:\\GitHub\\loadrunner-wrapper\\src\\test\\resources\\summary.html", "C:\\GitHub\\loadrunner-wrapper\\src\\test\\resources\\summary.csv");	
	}
	
}
