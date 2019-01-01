/*******************************************************************************
 * Copyright (C) 2018 Arpit Shah
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.artos.framework.infra;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.artos.framework.Enums.TestStatus;
import com.artos.framework.FWStaticStore;
import com.artos.framework.GUITestSelector;
import com.artos.framework.TestDataProvider;
import com.artos.framework.TestObjectWrapper;
import com.artos.framework.listener.ExtentReportListener;
import com.artos.framework.listener.TestExecutionEventListener;
import com.artos.framework.xml.TestScriptParser;
import com.artos.interfaces.PrePostRunnable;
import com.artos.interfaces.TestExecutable;
import com.artos.interfaces.TestProgress;
import com.artos.interfaces.TestRunnable;
import com.artos.utils.Transform;
import com.artos.utils.UtilsFramework;

/**
 * This class is responsible for running test cases in user defined manner
 */
public class ArtosRunner {

	TestContext context;
	List<TestProgress> listenerList = new ArrayList<TestProgress>();

	// ==================================================================================
	// Constructor (Starting point of framework)
	// ==================================================================================

	/**
	 * Constructor responsible for storing TestContext and class which contains main() method. Upon initialisation TestExecutionEventListener is
	 * registered so test decoration can be printed.
	 * 
	 * @param context TestContext object
	 * @see TestContext
	 * @see TestExecutionEventListener
	 */
	public ArtosRunner(TestContext context) {
		this.context = context;

		// Register default listener
		TestExecutionEventListener testListener = new TestExecutionEventListener(context);
		registerListener(testListener);
		context.registerListener(testListener);

		// Register extent reporting listener
		if (FWStaticStore.frameworkConfig.isEnableExtentReport()) {
			ExtentReportListener extentListener = new ExtentReportListener(context);
			registerListener(extentListener);
			context.registerListener(extentListener);
		}

	}

	// ==================================================================================
	// Runner Method
	// ==================================================================================

	/**
	 * Runner for the framework
	 * 
	 * @param testList List of tests to run. All test must be {@code TestExecutable} type
	 * @param groupList Group list which is required for test case filtering
	 * @throws Exception Exception will be thrown if test execution failed
	 */
	public void run(List<TestExecutable> testList, List<String> groupList) throws Exception {
		// Transform TestList into TestObjectWrapper Object list
		List<TestObjectWrapper> transformedTestList = new TransformToTestObjectWrapper(context, testList, groupList).getListOfTransformedTestCases();
		if (FWStaticStore.frameworkConfig.isGenerateTestScript()) {
			new TestScriptParser().createExecScriptFromObjWrapper(transformedTestList);
		}

		if (FWStaticStore.frameworkConfig.isEnableGUITestSelector()) {
			TestRunnable runObj = new TestRunnable() {
				@Override
				public void executeTest(TestContext context, List<TestObjectWrapper> transformedTestList) throws Exception {
					runTest(transformedTestList);
				}
			};
			new GUITestSelector(context, (List<TestObjectWrapper>) transformedTestList, runObj);
		} else {
			runTest(transformedTestList);
		}
	}

	/**
	 * This method executes test cases
	 * 
	 * @param transformedTestList test object list
	 * @throws Exception
	 */
	private void runTest(List<TestObjectWrapper> transformedTestList) throws Exception {

		LogWrapper logger = context.getLogger();

		// TODO : Parallel running test case can not work with current
		// architecture so should not enable this feature until solution is
		// found
		boolean enableParallelTestRunning = false;
		if (enableParallelTestRunning) {
			runParallelThread(transformedTestList, context);
		} else {
			runSingleThread(transformedTestList, context);
		}

		// Print Test results
		notifyTestSuiteSummaryPrinting("");

		// @formatter:off
		logger.info(
				"PASS:" + context.getCurrentPassCount() + 
				" FAIL:" + context.getCurrentFailCount() + 
				" SKIP:" + context.getCurrentSkipCount() + 
				" KTF:" + context.getCurrentKTFCount() + 
				" EXECUTED:" + context.getTotalTestCount() 
				// Total does not make sense because parameterised test cases are considered as a test case
				/*+ " TOTAL:" + transformedTestList.size()*/
				);
		// @formatter:on

		// Print Test suite Start and Finish time
		String timeStamp = new Transform().MilliSecondsToFormattedDate("dd-MM-yyyy hh:mm:ss", context.getTestSuiteStartTime());
		context.getLogger().getGeneralLogger().info("\nTest start time : {}", timeStamp);
		context.getLogger().getSummaryLogger().info("\nTest start time : {}", timeStamp);
		timeStamp = new Transform().MilliSecondsToFormattedDate("dd-MM-yyyy hh:mm:ss", context.getTestSuiteFinishTime());
		context.getLogger().getGeneralLogger().info("Test finish time : {}", timeStamp);
		context.getLogger().getSummaryLogger().info("Test finish time : {}", timeStamp);

		// Print Test suite summary
		logger.info("Test duration : " + String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(context.getTestSuiteTimeDuration()),
				TimeUnit.MILLISECONDS.toSeconds(context.getTestSuiteTimeDuration())
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(context.getTestSuiteTimeDuration()))));

		// HighLight Failed Test Cases
		highlightFailure(transformedTestList);

		// to release a thread lock
		context.getThreadLatch().countDown();
	}

	private void highlightFailure(List<TestObjectWrapper> transformedTestList) {
		notifyTestSuiteFailureHighlight("");

		if (context.getCurrentFailCount() > 0) {
			System.err.println("********************************************************");
			System.err.println("                 FAILED TEST CASES (" + context.getCurrentFailCount() + ")");
			System.err.println("\n********************************************************");

			int errorcount = 0;
			for (TestObjectWrapper t : transformedTestList) {

				/*
				 * If stopOnFail=true then test cases after first failure will not be executed which means TestOutcomeList will be empty
				 */
				if (t.getTestOutcomeList().isEmpty()) {
					continue;
				}

				// If test case is without date provider
				if ("".equals(t.getDataProviderName()) && t.getTestOutcomeList().get(0) == TestStatus.FAIL) {
					errorcount++;
					System.err.println(String.format("%-4s%s", errorcount, t.getTestClassObject().getName()));

					// If test case is with data provider
				} else if (!"".equals(t.getDataProviderName())) {
					for (int j = 0; j < t.getTestOutcomeList().size(); j++) {
						if (t.getTestOutcomeList().get(j) == TestStatus.FAIL) {
							errorcount++;
							System.err.println(String.format("%-4s%s", errorcount, t.getTestClassObject().getName()) + " : DataProvider[" + j + "]");
						}
					}
				}
			}

			System.err.println("********************************************************\n********************************************************");
		}
	}

	private void runSingleThread(List<TestObjectWrapper> testList, TestContext context)
			throws InstantiationException, IllegalAccessException, Exception {
		// ********************************************************************************************
		// TestSuite Start
		// ********************************************************************************************
		notifyTestSuiteExecutionStarted(context.getPrePostRunnableObj().getName());
		context.setTestSuiteStartTime(System.currentTimeMillis());

		try {

			// Create an instance of Main class
			PrePostRunnable prePostCycleInstance = (PrePostRunnable) context.getPrePostRunnableObj().newInstance();

			// Run prior to each test suite
			notifyBeforeTestSuiteMethodStarted(context.getPrePostRunnableObj().getName());
			prePostCycleInstance.beforeTestSuite(context);
			notifyBeforeTestSuiteMethodFinished(context.getPrePostRunnableObj().getName());

			for (int index = 0; index < context.getTotalLoopCount(); index++) {
				notifyTestExecutionLoopCount(index);
				// --------------------------------------------------------------------------------------------
				for (TestObjectWrapper t : testList) {

					// If stop on fail is selected then stop test execution
					if (FWStaticStore.frameworkConfig.isStopOnFail()) {
						if (context.getCurrentFailCount() > 0) {
							break;
						}
					}

					// Run Pre Method prior to any test Execution
					notifyBeforeTestMethodStarted(t);
					prePostCycleInstance.beforeTest(context);
					notifyBeforeTestMethodFinished(t);

					notifyTestExecutionStarted(t);
					// if data provider name is not specified then only execute test once
					if (null == t.getDataProviderName() || "".equals(t.getDataProviderName())) {
						runIndividualTest(t);
					} else {
						runParameterizedTest(t);
					}
					notifyTestExecutionFinished(t);

					// Run Post Method prior to any test Execution
					notifyAfterTestMethodStarted(t);
					prePostCycleInstance.afterTest(context);
					notifyAfterTestMethodFinished(t);

				}
				// --------------------------------------------------------------------------------------------
			}

			// Run at the end of each test suit
			notifyAfterTestSuiteMethodStarted(context.getPrePostRunnableObj().getName());
			prePostCycleInstance.afterTestSuite(context);
			notifyAfterTestSuiteMethodFinished(context.getPrePostRunnableObj().getName());

		} catch (Throwable e) {
			// Handle if any exception in pre-post runnable
			UtilsFramework.writePrintStackTrace(context, e);
			notifyTestSuiteException(e.getMessage());
		}

		// Set Test Finish Time
		context.setTestSuiteFinishTime(System.currentTimeMillis());
		notifyTestSuiteExecutionFinished(context.getPrePostRunnableObj().getName());
		// ********************************************************************************************
		// TestSuite Finish
		// ********************************************************************************************
	}

	/**
	 * Responsible for execution individual test cases
	 * 
	 * @param t TestCase in format {@code TestObjectWrapper}
	 */
	private void runIndividualTest(TestObjectWrapper t) {
		// ********************************************************************************************
		// TestCase Start
		// ********************************************************************************************
		try {
			t.setTestStartTime(System.currentTimeMillis());

			// Set Default Known to fail information
			context.setKnownToFail(t.isKTF(), t.getBugTrackingNumber());

			// If test timeout is defined then monitor thread for timeout
			if (0 != t.getTestTimeout()) {
				runTestWithTimeout(t);
			} else {
				runSimpleTest(t);
			}

			postTestValidation(t);
		} catch (Throwable e) {
			processTestException(t, e);
			notifyTestException(e.getMessage());
		} finally {
			t.setTestFinishTime(System.currentTimeMillis());
			context.generateTestSummary(t);
		}
		// ********************************************************************************************
		// TestCase Finish
		// ********************************************************************************************
	}

	/**
	 * Responsible for executing data provider method which upon successful execution returns an array of parameters. TestCase will be re-run using
	 * all parameters available in the parameter array. If data provider method returns empty array or null then test case will be executed only once
	 * with null arguments.
	 * 
	 * @param t TestCase in format {@code TestObjectWrapper}
	 */
	private void runParameterizedTest(TestObjectWrapper t) {
		Object[][] data;
		TestDataProvider dataProviderObj;

		try {
			// get dataProvider specified for this test case (data provider name is always
			// stored in upper case)
			dataProviderObj = context.getDataProviderMap().get(t.getDataProviderName().toUpperCase());

			// If specified data provider is not found in the list then throw exception
			// (Remember : Data provider name is case in-sensitive)
			if (null == dataProviderObj) {
				throw new InvalidObjectException("DataProvider not found (or private) : " + (t.getDataProviderName()));
			}

			if (dataProviderObj.isStaticMethod()) {
				data = (Object[][]) dataProviderObj.getMethod().invoke(null, context);
			} else {
				/* NonStatic data provider method needs an instance */
				data = (Object[][]) dataProviderObj.getMethod().invoke(dataProviderObj.getClassOfTheMethod().newInstance(), context);
			}

			// If data provider method returns null or empty object then execute test with
			// null parameter
			if (null == data || data.length == 0) {
				executeChildTest(t, new String[][] { {} }, 0);
			} else {
				for (int i = 0; i < data.length; i++) {
					executeChildTest(t, data, i);
				}
			}
		} catch (Exception e) {
			// Print Exception
			UtilsFramework.writePrintStackTrace(context, e);
			notifyTestSuiteException(e.getMessage());

			// Mark current test as fail due to exception during data provider processing
			context.setTestStatus(TestStatus.FAIL, e.getMessage());
			context.generateTestSummary(t);
		}
	}

	/**
	 * Responsible for execution of a test case.
	 * 
	 * @param t TestCase in format {@code TestObjectWrapper}
	 * @throws Exception Exception during test execution
	 */
	private void runSimpleTest(TestObjectWrapper t) throws Exception {
		// --------------------------------------------------------------------------------------------
		// Run execute Method (This is if test suite is designed as single test per test cases)
		((TestExecutable) t.getTestClassObject().newInstance()).execute(context);
		// Run Unit tests (This is if test suite have unit tests)
		new RunnerTestUnits(context).runSingleThreadUnits(t);
		// --------------------------------------------------------------------------------------------
	}

	/**
	 * Responsible for executing test case with thread timeout. If test case execution is not finished within expected time then test will be
	 * considered failed.
	 * 
	 * @param t TestCase in format {@code TestObjectWrapper} object
	 * @throws Throwable Exception during test execution
	 */
	private void runTestWithTimeout(TestObjectWrapper t) throws Throwable {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				runSimpleTest(t);
				return "TEST CASE FINISHED WITHIN TIME";
			}
		});

		try {
			// System.out.println(future.get(t.getTestTimeout(), TimeUnit.MILLISECONDS));
			future.get(t.getTestTimeout(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			context.setTestStatus(TestStatus.FAIL, "TEST CASE TIMED OUT");
		} catch (ExecutionException e) {
			future.cancel(true);
			if (null == e.getCause()) {
				// If no cause is listed then throw parent exception
				throw e;
			} else {
				// If cause is listed then only throw cause
				throw e.getCause();
			}
		}
	}

	/**
	 * Responsible for execution of test cases (Considered as child test case) with given parameter. Parameterised object array index and value(s)
	 * class type(s) will be printed prior to test execution for user's benefit.
	 * 
	 * @param t TestCase in format {@code TestObjectWrapper}
	 * @param data Array of parameters
	 * @param arrayIndex Parameter array index
	 */
	private void executeChildTest(TestObjectWrapper t, Object[][] data, int arrayIndex) {
		String userInfo = "DataProvider(" + arrayIndex + ")  : ";
		if (data[arrayIndex].length == 2) {
			context.setParameterisedObject1(null == data[arrayIndex][0] ? null : data[arrayIndex][0]);
			context.setParameterisedObject2(null == data[arrayIndex][1] ? null : data[arrayIndex][1]);
			String firstType = (context.getParameterisedObject1().getClass().getSimpleName());
			String secondType = (context.getParameterisedObject2().getClass().getSimpleName());
			userInfo += "[" + firstType + "][" + secondType + "]";

		} else if (data[arrayIndex].length == 1) {
			context.setParameterisedObject1(null == data[arrayIndex][0] ? null : data[arrayIndex][0]);
			context.setParameterisedObject2(null);
			String firstType = (context.getParameterisedObject1().getClass().getSimpleName());
			userInfo += "[" + firstType + "][]";
		} else {
			context.setParameterisedObject1(null);
			context.setParameterisedObject2(null);
			userInfo += "[][]";
		}
		context.getLogger().info(userInfo);

		// ********************************************************************************************
		// Parameterised Child TestCase Start
		// ********************************************************************************************

		notifyChildTestExecutionStarted(t, userInfo);

		runIndividualTest(t);

		notifyChildTestExecutionFinished(t);

		// ********************************************************************************************
		// Parameterised Child TestCase Finish
		// ********************************************************************************************
	}

	/**
	 * Responsible for post validation after test case execution is successfully completed. If expected throwable(s)/exception(s) are defined by user
	 * using {@code ExpectedException} and test case status is PASS or FAIL then test case should be marked failed for not throwing expected
	 * throwable/exception.
	 * 
	 * <PRE>
	 * If test status is marked as SKIP or KTF then do not fail test case based on ExpectedException conditions. 
	 * If test is marked as SKIP then user should have taken decision based on condition checking so test framework does not need to overrule decision.
	 * If test is marked as KTF then user already knows that this test scenario is known to fail, so forcefully failing will dilute the meaning of having known to fail status.
	 * </PRE>
	 * 
	 * @param t {@code TestObjectWrapper} object
	 */
	private void postTestValidation(TestObjectWrapper t) {
		if (context.getCurrentTestStatus() == TestStatus.PASS || context.getCurrentTestStatus() == TestStatus.FAIL) {
			if (null != t.getExpectedExceptionList() && !t.getExpectedExceptionList().isEmpty() && t.isEnforceException()) {
				// Exception annotation was specified but did not occur
				context.setTestStatus(TestStatus.FAIL, "Exception was specified but did not occur");
			}
		}
	}

	/**
	 * Responsible for processing throwable/exception thrown by test cases during execution time. If {@code ExpectedException} annotation defines
	 * expected throwable/exception and received throwable/exception does not match any of the defined throwable(s)/Exception(s) then test will be
	 * marked as FAIL.
	 * 
	 * @param t test case in format {@code TestObjectWrapper}
	 * @param e {@code Throwable} or {@code Exception}
	 */
	private void processTestException(TestObjectWrapper t, Throwable e) {
		// If user has not specified expected exception then fail the test
		if (null != t.getExpectedExceptionList() && !t.getExpectedExceptionList().isEmpty()) {

			boolean exceptionMatchFound = false;
			for (Class<? extends Throwable> exceptionClass : t.getExpectedExceptionList()) {
				if (e.getClass() == exceptionClass) {
					/* Exception matches as specified by user */
					context.setTestStatus(TestStatus.PASS, "Exception is as expected : " + e.getClass().getName() + " : " + e.getMessage());

					/* If regular expression then validate exception message with regular expression */
					/* If regular expression does not match then do string compare */
					if (null != t.getExceptionContains() && !"".equals(t.getExceptionContains())) {
						if (e.getMessage().matches(t.getExceptionContains())) {
							context.setTestStatus(TestStatus.PASS, "Exception message matches regex : " + t.getExceptionContains());
						} else if (e.getMessage().equals(t.getExceptionContains())) {
							context.setTestStatus(TestStatus.PASS, "Exception message matches string : " + t.getExceptionContains());
						} else {
							context.setTestStatus(TestStatus.FAIL, "Exception message does not match regex : \nRegularExpression : "
									+ t.getExceptionContains() + "\nException Message : " + e.getMessage());
						}
					}

					exceptionMatchFound = true;
					break;
				}
			}
			if (!exceptionMatchFound) {
				String expectedExceptions = "";
				for (Class<? extends Throwable> exceptionClass : t.getExpectedExceptionList()) {
					expectedExceptions += exceptionClass.getName() + " ";
				}
				context.setTestStatus(TestStatus.FAIL,
						"Exception is not as expected : \nExpected : " + expectedExceptions + "\nReturned : " + e.getClass().getName());
				UtilsFramework.writePrintStackTrace(context, e);
			}
		} else {
			context.setTestStatus(TestStatus.FAIL, e.getMessage());
			UtilsFramework.writePrintStackTrace(context, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void runParallelThread(List<TestObjectWrapper> testList, TestContext context)
			throws InstantiationException, IllegalAccessException, Exception {
		// ********************************************************************************************
		// Test Start
		// ********************************************************************************************
		notifyTestSuiteExecutionStarted(context.getPrePostRunnableObj().getName());
		context.setTestSuiteStartTime(System.currentTimeMillis());

		// Create an instance of Main class
		PrePostRunnable prePostCycleInstance = (PrePostRunnable) context.getPrePostRunnableObj().newInstance();

		// Run prior to each test suit
		prePostCycleInstance.beforeTestSuite(context);
		for (int index = 0; index < context.getTotalLoopCount(); index++) {
			notifyTestExecutionLoopCount(index);
			// --------------------------------------------------------------------------------------------
			ExecutorService service = Executors.newFixedThreadPool(1000);
			List<Future<Runnable>> futures = new ArrayList<>();

			for (TestObjectWrapper t : testList) {

				Future<?> f = service.submit(new runTestInParallel(context, t, prePostCycleInstance));
				futures.add((Future<Runnable>) f);

			}

			// wait for all tasks to complete before continuing
			for (Future<Runnable> f : futures) {
				f.get();
			}

			// shut down the executor service so that this thread can exit
			service.shutdownNow();
			// --------------------------------------------------------------------------------------------
		}
		// Run at the end of each test suit
		prePostCycleInstance.afterTestSuite(context);
		// ********************************************************************************************
		// Test Finish
		// ********************************************************************************************
	}

	// ==================================================================================
	// Register, deRegister and Notify Event Listeners
	// ==================================================================================

	public void registerListener(TestProgress listener) {
		listenerList.add(listener);
	}

	public void deRegisterListener(TestProgress listener) {
		listenerList.remove(listener);
	}

	public void deRegisterAllListener() {
		listenerList.clear();
	}

	void notifyBeforeTestSuiteMethodStarted(String testSuiteName) {
		for (TestProgress listener : listenerList) {
			listener.beforeTestSuiteMethodStarted(testSuiteName);
		}
	}

	void notifyBeforeTestSuiteMethodFinished(String testSuiteName) {
		for (TestProgress listener : listenerList) {
			listener.beforeTestSuiteMethodFinished(testSuiteName);
		}
	}

	void notifyAfterTestSuiteMethodStarted(String testSuiteName) {
		for (TestProgress listener : listenerList) {
			listener.afterTestSuiteMethodStarted(testSuiteName);
		}
	}

	void notifyAfterTestSuiteMethodFinished(String testSuiteName) {
		for (TestProgress listener : listenerList) {
			listener.afterTestSuiteMethodFinished(testSuiteName);
		}
	}

	void notifyTestSuiteExecutionStarted(String testSuiteName) {
		for (TestProgress listener : listenerList) {
			listener.testSuiteExecutionStarted(testSuiteName);
		}
	}

	void notifyTestSuiteExecutionFinished(String testSuiteName) {
		for (TestProgress listener : listenerList) {
			listener.testSuiteExecutionFinished(testSuiteName);
		}
	}

	void notifyBeforeTestMethodStarted(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.beforeTestMethodStarted(t);
		}
	}

	void notifyBeforeTestMethodFinished(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.beforeTestMethodFinished(t);
		}
	}

	void notifyAfterTestMethodStarted(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.afterTestMethodStarted(t);
		}
	}

	void notifyAfterTestMethodFinished(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.afterTestMethodFinished(t);
		}
	}

	void notifyTestExecutionStarted(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.testExecutionStarted(t);
		}
	}

	void notifyTestExecutionFinished(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.testExecutionFinished(t);
		}
	}

	void notifyChildTestExecutionStarted(TestObjectWrapper t, String userInfo) {
		for (TestProgress listener : listenerList) {
			listener.childTestExecutionStarted(t, userInfo);
		}
	}

	void notifyChildTestExecutionFinished(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.childTestExecutionFinished(t);
		}
	}

	void notifyTestExecutionSkipped(TestObjectWrapper t) {
		for (TestProgress listener : listenerList) {
			listener.testExecutionSkipped(t);
		}
	}

	void notifyTestExecutionLoopCount(int count) {
		for (TestProgress listener : listenerList) {
			listener.testExecutionLoopCount(count);
		}
	}

	void notifyTestSuiteException(String description) {
		for (TestProgress listener : listenerList) {
			listener.testSuiteException(description);
		}
	}

	void notifyTestException(String description) {
		for (TestProgress listener : listenerList) {
			listener.testException(description);
		}
	}

	void notifyTestSuiteSummaryPrinting(String description) {
		for (TestProgress listener : listenerList) {
			listener.testSuiteSummaryPrinting(description);
		}
	}

	void notifyTestSuiteFailureHighlight(String description) {
		for (TestProgress listener : listenerList) {
			listener.testSuiteFailureHighlight(description);
		}
	}
}

class runTestInParallel implements Runnable {

	PrePostRunnable prePostCycleInstance;
	TestContext context;
	TestObjectWrapper t;

	public runTestInParallel(TestContext context, TestObjectWrapper test, PrePostRunnable prePostCycleInstance) {
		this.context = context;
		this.t = test;
		this.prePostCycleInstance = prePostCycleInstance;
	}

	@Override
	public void run() {
		try {
			// notifyTestExecutionStarted(t);
			// Run Pre Method prior to any test Execution
			prePostCycleInstance.beforeTest(context);

			runIndividualTest(t);

			// Run Post Method prior to any test Execution
			prePostCycleInstance.afterTest(context);
			// notifyTestExecutionFinished(t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runIndividualTest(TestObjectWrapper t) {
		t.setTestStartTime(System.currentTimeMillis());
		try {
			// Set Default Known to fail information
			context.setKnownToFail(t.isKTF(), t.getBugTrackingNumber());

			// --------------------------------------------------------------------------------------------
			// get test objects new instance and cast it to TestExecutable type
			((TestExecutable) t.getTestClassObject().newInstance()).execute(context);
			// --------------------------------------------------------------------------------------------

		} catch (Exception e) {
			context.setTestStatus(TestStatus.FAIL, e.getMessage());
			UtilsFramework.writePrintStackTrace(context, e);
		} catch (Throwable ex) {
			context.setTestStatus(TestStatus.FAIL, ex.getMessage());
			UtilsFramework.writePrintStackTrace(context, ex);
		} finally {
			t.setTestFinishTime(System.currentTimeMillis());
			context.generateTestSummary(t);
		}
	}
}
