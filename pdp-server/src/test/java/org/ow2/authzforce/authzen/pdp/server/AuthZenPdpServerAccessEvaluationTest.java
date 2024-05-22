/*
 * Copyright (C) 2012-2024 THALES.
 *
 * This file is part of AuthzForce CE.
 *
 * AuthzForce CE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuthzForce CE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuthzForce CE.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ow2.authzforce.authzen.pdp.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.cxf.jaxrs.client.WebClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ow2.authzforce.jaxrs.util.JsonRiJaxrsProvider;
import org.ow2.authzforce.xacml.json.model.LimitsCheckingJSONObject;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for CXF/JAX-RS-based REST profile implementation using XACML JSON Profile for payloads
 * 
 */
@SpringBootTest(classes = AuthZenPdpSpringBootApp.class, properties = { "spring.config.location=target/test-classes/server/application.yml",
        "cfg.dir=target/test-classes/server" }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class AuthZenPdpServerAccessEvaluationTest
{
	private static final int MAX_JSON_STRING_LENGTH = 100;

	/*
	 * Max number of child elements - key-value pairs or items - in JSONObject/JSONArray
	 */
	private static final int MAX_JSON_CHILDREN_COUNT = 100;

	private static final int MAX_JSON_DEPTH = 10;

	private static final String[] TEST_ROOT_DIRS = {"../pdp-extensions/src/test/resources/AuthZEN_Identiverse_2024_Interop/Morty", "../pdp-extensions/src/test/resources/AuthZEN_Identiverse_2024_Interop/Rick", "../pdp-extensions/src/test/resources/AuthZEN_Identiverse_2024_Interop/Summer_Smith"};

	@BeforeAll
	public static void setup()
	{
		System.setProperty("javax.xml.accessExternalSchema", "http,https,file");
		// TODO: copy policies directory to maven target dir
		// maybe not needed
	}

	@LocalServerPort
	private int port;

	private static Stream<Arguments> getTestDirectories() throws Exception {
		File[] test0Dirs = new File("..").listFiles();
		return Stream.of(TEST_ROOT_DIRS).flatMap(testRootDir -> Stream.of(new File(testRootDir).listFiles()).filter(f -> f.isDirectory()).map(Arguments::of));
	}

	@ParameterizedTest
	@MethodSource("getTestDirectories")
	public void testPdpRequest(final File testDir) throws IOException
	{
		// Request body
		final File reqFile = new File(testDir, "request.json");
		try (InputStream reqIn = new FileInputStream(reqFile))
		{
			final JSONObject jsonRequest = new LimitsCheckingJSONObject(new InputStreamReader(reqIn, StandardCharsets.UTF_8), MAX_JSON_STRING_LENGTH, MAX_JSON_CHILDREN_COUNT, MAX_JSON_DEPTH);
			// TODO: JSON schema validation when the spec provides such schema
			// expected response
			final File expectedRespFile = new File(testDir, "response.json");
			try (final InputStream respIn = new FileInputStream(expectedRespFile))
			{
				final JSONObject expectedResponse = new LimitsCheckingJSONObject(new InputStreamReader(respIn, StandardCharsets.UTF_8), MAX_JSON_STRING_LENGTH, MAX_JSON_CHILDREN_COUNT, MAX_JSON_DEPTH);
				// TODO: JSON schema validation when the spec provides such schema
				// send request
				final WebClient client = WebClient.create("http://localhost:" + port + "/access/v1/evaluation", Collections.singletonList(new JsonRiJaxrsProvider()));
				final JSONObject actualResponse = client.path("/").type("application/json").accept("application/json").post(jsonRequest, JSONObject.class);

				// check response
				assertTrue(expectedResponse.similar(actualResponse));
			}
		}
	}

}
