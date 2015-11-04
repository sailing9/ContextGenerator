package util;

/**
 * This class is a tool for timing utility. Example:
 * 
 * <p><blockquote><pre>
 * 
 * TimeStat stat = new TimeStat();
 * stat.start();
 * //<i>Do your things here</i>
 * long timeCost = stat.getElapsedTimeMills();  // <i>get elapsed time in milliseconds</i>
 * //<i>double timeCost = stat.getElapsedTimeSeconds() ; // get elapsed time in seconds</i>
 * 
 * //<i> reuse the TimeStat object
 * stat.start();
 * ...
 * </pre></blockquote><p>
 * @author hmc
 *
 */

public class TimeStat {
	
	/** a long value to keep the starting time */
	private long start;
	
	/** 
	 * Default constructor 
	 */
	public TimeStat() {
	}
	
	/** 
	 * Reset 
	 *
	 */
	public void reset() {
		start = System.currentTimeMillis();
	}
	
	/**
	 * Start to timing
	 *
	 */
	public void start() {
		reset();
	}
	
	/**
	 * Get time elapsed in milliseconds
	 * @return Time elapsed in milliseconds
	 */
	public long getElapsedTimeMillis() {
		return System.currentTimeMillis() - start;
	}
	
	/**
	 * Get time elapsed in seconds
	 * @return Time elapsed in seconds
	 */
	public float getElapsedTimeSeconds() {
		return (System.currentTimeMillis() - start) / 1000.0f;
	}
	
	/**
	 * Print time status in milliseconds
	 *
	 */
	public void printTimeMillisStat() {
		System.out.println("Time elapsed is " + getElapsedTimeMillis() + "ms");
	}

	/**
	 * Print time status in seconds
	 *
	 */
	public void printTimeSecondStat() {
		System.out.println("Time elapsed is " + getElapsedTimeSeconds() + "s");
	}
}
