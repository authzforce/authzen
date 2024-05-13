/*
 * Copyright 2012-2024 THALES.
 *
 * This file is part of AuthzForce CE.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ow2.authzforce.pdp.ext.openid.authzen.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.ow2.authzforce.core.pdp.api.DecisionRequestPreprocessor;
import org.ow2.authzforce.core.pdp.api.DecisionResultPostprocessor;
import org.ow2.authzforce.core.pdp.api.io.PdpEngineInoutAdapter;
import org.ow2.authzforce.core.pdp.impl.PdpEngineConfiguration;
import org.ow2.authzforce.core.pdp.impl.io.PdpEngineAdapters;
import org.ow2.authzforce.core.pdp.io.xacml.json.BaseXacmlJsonResultPostprocessor;
import org.ow2.authzforce.core.pdp.io.xacml.json.IndividualXacmlJsonRequest;
import org.ow2.authzforce.core.pdp.io.xacml.json.SingleDecisionXacmlJsonRequestPreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.springframework.util.ResourceUtils;

/**
 * Tests for OpenID AuthZen Access Evaluation API conformance
 */
public final class AuthZenAccessEvaluationTest
{
	/**
	 * Filename of request to send to the PDP..
	 */
	public static final String REQUEST_FILENAME = "request.json";

	/**
	 * Filename of the expected response from the PDP..
	 */
	public static final String EXPECTED_RESPONSE_FILENAME = "response.json";

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthZenAccessEvaluationTest.class);

	private final PdpEngineInoutAdapter<JSONObject, JSONObject> pdp;

	/**
	 * Constructor
	 * @param pdpConfigRelativePath   Relative path to PDP Configuration file (relative to Maven project root)
	 */
	@Parameters({ "pdp_config_path" })
	public AuthZenAccessEvaluationTest(String pdpConfigRelativePath) throws IOException
	{
		final Path pdpConfigPath = Paths.get(pdpConfigRelativePath);

		final PdpEngineConfiguration pdpEngineConf = PdpEngineConfiguration.getInstance(pdpConfigPath.toString());

		// Although not used in the tests, we set the XACML/JSON (Profile) in-out adapters as default
		final DecisionResultPostprocessor<IndividualXacmlJsonRequest, JSONObject> defaultResultPostproc = new BaseXacmlJsonResultPostprocessor(pdpEngineConf.getClientRequestErrorVerbosityLevel());
		final DecisionRequestPreprocessor<JSONObject, IndividualXacmlJsonRequest> defaultReqPreproc = SingleDecisionXacmlJsonRequestPreprocessor.LaxVariantFactory.INSTANCE.getInstance(
				pdpEngineConf.getAttributeValueFactoryRegistry(), pdpEngineConf.isStrictAttributeIssuerMatchEnabled(), pdpEngineConf.isXPathEnabled(),
				defaultResultPostproc.getFeatures());

		pdp = PdpEngineAdapters.newInoutAdapter(JSONObject.class, JSONObject.class, pdpEngineConf, defaultReqPreproc, defaultResultPostproc);
	}

	@DataProvider(name = "testDirProvider")
	public Iterator<Object[]> provideTestDirectories(ITestContext testContext) throws Exception
	{
		final String testRootDirRelativePath = testContext.getCurrentXmlTest().getParameter("test_root_dir");
		final URL testRootDirLocation = ResourceUtils.getURL(testRootDirRelativePath);
		final Path testRootDirPath = Paths.get(testRootDirLocation.toURI());

		final Collection<Object[]> testData = new ArrayList<>();
		/*
		 * Each sub-directory of the root directory contains data for a specific test: AuthZen Access Evaluation Request and expected Response (request.json, response.json).
		 */
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(testRootDirPath, Files::isDirectory))
		{
			for (final Path testDirPath : stream)
			{
				// specific test's resources directory location and request filter ID, used as parameters to test(...)
				testData.add(new Object[] { testDirPath });
			}
		}

		return testData.iterator();
	}

	@Test(dataProvider = "testDirProvider")
	public void test(final Path testDirectoryPath) throws IOException
	{
		LOGGER.debug("******************************");
		LOGGER.debug("Starting PDP test in directory '{}'", testDirectoryPath);

		final Path reqFilepath = testDirectoryPath.resolve(REQUEST_FILENAME);
		final JSONObject request;
		try (InputStream inputStream = new FileInputStream(reqFilepath.toFile()))
		{
			request = new JSONObject(new JSONTokener(inputStream));
		}

		//FIXME: JSON schema validation (no schema defined in the standard yet)

		LOGGER.debug("Request sent to the PDP: {}", request);

		final Path expectedResponseFilepath = testDirectoryPath.resolve(EXPECTED_RESPONSE_FILENAME);
		final JSONObject expectedResponse;
		try (InputStream inputStream = new FileInputStream(expectedResponseFilepath.toFile()))
		{
			expectedResponse = new JSONObject(new JSONTokener(inputStream));
		}

		final JSONObject actualResponse = pdp.evaluate(request);
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug("Response received from the PDP:  {}", actualResponse);
		}

		Assert.assertTrue(actualResponse.similar(expectedResponse), "Test failed in directory '" + testDirectoryPath + "'");
	}

}
