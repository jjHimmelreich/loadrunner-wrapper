package perflab;
/**
 * 
 */


/**
 * @author eHimmelreich
 *
 */
public class LoadRunnerTransaction {
	private String name;
	private float avgRT;
	private float maxRT;
	private float minRT;
	private int passed;
	private int failed;
	private int failedPrecentage;
	
	public LoadRunnerTransaction() {
		this.name = "";
		this.minRT = -1;
		this.maxRT = -1;
		this.passed = -1;
		this.failed = -1;
		this.failedPrecentage = -1;
	}
	
	public LoadRunnerTransaction(String name, float minRT, float avgRT, float maxRT, int passed, int failed, int failedPrecentage) {
		this.name = name;
		this.avgRT = avgRT;
		this.minRT = minRT;
		this.maxRT = maxRT;
		this.passed = passed;
		this.failed = failed;
		this.failedPrecentage = failedPrecentage;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getAvgRT() {
		return avgRT;
	}

	public void setAvgRT(float avgRT) {
		this.avgRT = avgRT;
	}

	public float getMaxRT() {
		return maxRT;
	}

	public void setMaxRT(float maxRT) {
		this.maxRT = maxRT;
	}

	public float getMinRT() {
		return minRT;
	}

	public void setMinRT(float minRT) {
		this.minRT = minRT;
	}

	public int getPassed() {
		return passed;
	}

	public void setPassed(int passed) {
		this.passed = passed;
	}

	public int getFailed() {
		return failed;
	}

	public void setFailed(int failed) {
		this.failed = failed;
	}

	public int getFailedPrecentage() { return failedPrecentage;	}

	public void setFailedPrecentage(int failedPrecentage) {	this.failedPrecentage = failedPrecentage;}
	
}
