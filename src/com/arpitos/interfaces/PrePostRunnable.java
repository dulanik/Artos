package com.arpitos.interfaces;

import com.arpitos.annotation.AfterTest;
import com.arpitos.annotation.AfterTestsuite;
import com.arpitos.annotation.BeforeTest;
import com.arpitos.annotation.BeforeTestsuite;
import com.arpitos.framework.infra.TestContext;

/**
 * Used for classes which implements Pre Post methods for test cases
 * 
 * @author ArpitS
 *
 */
public interface PrePostRunnable {

	/**
	 * Runs prior to each test case execution
	 * 
	 * @param context
	 *            Test Context
	 * @throws Exception
	 *             In case of pre execution failed
	 */
	@BeforeTest
	default public void beforeTest(TestContext context) throws Exception {

	}

	/**
	 * Runs post each test case execution
	 * 
	 * @param context
	 *            Test context
	 * @throws Exception
	 *             In case of test of post execution failed
	 */
	@AfterTest
	default public void afterTest(TestContext context) throws Exception {

	}

	/**
	 * Runs prior to test suit execution. Only run once
	 * 
	 * @param context
	 *            Test context
	 * @throws Exception
	 *             In case of initialisation failed
	 */
	@BeforeTestsuite
	default public void beforeTestsuite(TestContext context) throws Exception {

	}

	/**
	 * Runs at the end of test suit execution. Only run once
	 * 
	 * @param context
	 *            Test context
	 * @throws Exception
	 *             In case of cleanup failed
	 */
	@AfterTestsuite
	default public void afterTestsuite(TestContext context) throws Exception {

	}

}
