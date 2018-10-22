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
package com.artos.framework;

import java.util.ArrayList;
import java.util.List;

import com.artos.framework.Enums.TestStatus;

/**
 * This class wraps test object with other necessary information which is helpful during test execution
 * 
 * 
 *
 */
public class TestObjectWrapper {

	// TestTracking variables
	long testStartTime;
	long testFinishTime;
	/*
	 * This will be used to store all parameterised tests status. If test case is not parameterised test case then only one status will be stored
	 */
	List<TestStatus> testOutcomeList = new ArrayList<>();

	// TestCase
	Class<?> testClassObject = null;
	boolean skipTest = false;
	int testsequence = 0;
	List<String> labelList = new ArrayList<>();
	List<String> groupList = new ArrayList<>();
	String dataProviderName = "";

	// TestPlan
	String testPlanDescription = "";
	String testPlanPreparedBy = "";
	String testPlanPreparationDate = "";
	String testreviewedBy = "";
	String testReviewDate = "";
	String testPlanBDD = "";

	// KnowToFail
	boolean KTF = false;
	String bugTrackingNumber = "";

	// ExpectedException
	List<Class<? extends Throwable>> expectedExceptionList = null;
	String exceptionContains = "";
	Boolean enforce = true;

	/**
	 * Default constructor
	 * 
	 * @param cls test class object
	 * @param skipTest skip property as specified in annotation
	 * @param testsequence test sequence as specified in annotation
	 */
	public TestObjectWrapper(Class<?> cls, boolean skipTest, int testsequence, String dataProviderName) {
		super();

		this.testClassObject = cls;

		this.skipTest = skipTest;
		this.testsequence = testsequence;
		this.dataProviderName = dataProviderName;
	}

	public String getTestPlanDescription() {
		return testPlanDescription;
	}

	public void setTestPlanDescription(String testPlanDescription) {
		this.testPlanDescription = testPlanDescription;
	}

	public String getTestPlanPreparedBy() {
		return testPlanPreparedBy;
	}

	public void setTestPlanPreparedBy(String testPlanPreparedBy) {
		this.testPlanPreparedBy = testPlanPreparedBy;
	}

	public String getTestPlanPreparationDate() {
		return testPlanPreparationDate;
	}

	public void setTestPlanPreparationDate(String testPlanPreparationDate) {
		this.testPlanPreparationDate = testPlanPreparationDate;
	}

	public String getTestreviewedBy() {
		return testreviewedBy;
	}

	public void setTestreviewedBy(String testreviewedBy) {
		this.testreviewedBy = testreviewedBy;
	}

	public String getTestReviewDate() {
		return testReviewDate;
	}

	public void setTestReviewDate(String testReviewDate) {
		this.testReviewDate = testReviewDate;
	}

	public String getTestPlanBDD() {
		return testPlanBDD;
	}

	public void setTestPlanBDD(String testPlanBDD) {
		this.testPlanBDD = testPlanBDD;
	}

	public Class<?> getTestClassObject() {
		return testClassObject;
	}

	public void setTestClassObject(Class<?> testClassObject) {
		this.testClassObject = testClassObject;
	}

	public boolean isSkipTest() {
		return skipTest;
	}

	public void setSkipTest(boolean skipTest) {
		this.skipTest = skipTest;
	}

	public int getTestsequence() {
		return testsequence;
	}

	public void setTestsequence(int testsequence) {
		this.testsequence = testsequence;
	}

	public boolean isKTF() {
		return KTF;
	}

	public void setKTF(boolean kTF) {
		KTF = kTF;
	}

	public String getBugTrackingNumber() {
		return bugTrackingNumber;
	}

	public void setBugTrackingNumber(String bugTrackingNumber) {
		this.bugTrackingNumber = bugTrackingNumber;
	}

	public List<String> getGroupList() {
		return groupList;
	}

	public void setGroupList(List<String> groupList) {
		this.groupList = groupList;
	}

	public List<String> getLabelList() {
		return labelList;
	}

	public void setLabelList(List<String> labelList) {
		this.labelList = labelList;
	}

	public List<Class<? extends Throwable>> getExpectedExceptionList() {
		return expectedExceptionList;
	}

	public void setExpectedExceptionList(List<Class<? extends Throwable>> expectedExceptionList) {
		this.expectedExceptionList = expectedExceptionList;
	}

	public String getExceptionContains() {
		return exceptionContains;
	}

	public void setExceptionContains(String exceptionContains) {
		this.exceptionContains = exceptionContains;
	}

	public Boolean isEnforceException() {
		return enforce;
	}

	public void setEnforceException(Boolean enforce) {
		this.enforce = enforce;
	}

	public String getDataProviderName() {
		return dataProviderName;
	}

	public void setDataProviderName(String dataProviderName) {
		this.dataProviderName = dataProviderName;
	}

	public long getTestStartTime() {
		return testStartTime;
	}

	public void setTestStartTime(long testStartTime) {
		this.testStartTime = testStartTime;
	}

	public long getTestFinishTime() {
		return testFinishTime;
	}

	public void setTestFinishTime(long testFinishTime) {
		this.testFinishTime = testFinishTime;
	}

	public List<TestStatus> getTestOutcomeList() {
		return testOutcomeList;
	}

	public void setTestOutcomeList(List<TestStatus> testOutcomeList) {
		this.testOutcomeList = testOutcomeList;
	}

}
