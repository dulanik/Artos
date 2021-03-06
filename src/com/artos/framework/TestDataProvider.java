/*******************************************************************************
 * Copyright (C) 2018-2019 Arpit Shah and Artos Contributors
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

import java.lang.reflect.Method;

public class TestDataProvider {

	String dataProviderName;
	Class<?> classOfTheMethod;
	Method method;
	boolean staticMethod;

	/**
	 * @param method method which has {@code DataProvider} annotation
	 * @param dataProviderName name given to {@code DataProvider}
	 * @param classOfTheMethod class object where method belongs to
	 * @param staticMethod true if method is static
	 */
	public TestDataProvider(Method method, String dataProviderName, Class<?> classOfTheMethod, boolean staticMethod) {
		super();
		this.method = method;
		this.dataProviderName = dataProviderName;
		this.classOfTheMethod = classOfTheMethod;
		this.staticMethod = staticMethod;
	}

	public String getDataProviderName() {
		return dataProviderName;
	}

	public Class<?> getClassOfTheMethod() {
		return classOfTheMethod;
	}

	public Method getMethod() {
		return method;
	}

	public boolean isStaticMethod() {
		return staticMethod;
	}
}
