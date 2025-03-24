/*
 * Copyright 2012-2025 THALES.
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
package org.ow2.authzforce.authzen.pdp.ext;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.DecisionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.Status;
import org.json.JSONObject;
import org.ow2.authzforce.core.pdp.api.DecisionResult;
import org.ow2.authzforce.core.pdp.api.DecisionResultPostprocessor;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.io.xacml.json.IndividualXacmlJsonRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenID AuthZEN Access Evaluation API - Result Postprocessor.
 * <p>
 *     Links:
 *     <ul>
 *         <li><a href="https://openid.net/wg/authzen/">OpenID AuthZEN Working Group</a></li>
 *         <li><a href="https://github.com/openid/authzen/blob/main/api/authorization-api-1_0.md#access-evaluations-api">Access Evaluation API</a></li>
 *         <li><a href="https://www.postman.com/axiomatics/workspace/authzen-sample-requests/">AuthZEN Sample Request/Response Collection</a></li>
 *     </ul>
 *
 */
public final class OpenIDAuthZenAccessEvaluationResultPostprocessorFactory implements DecisionResultPostprocessor.Factory<IndividualXacmlJsonRequest, JSONObject> {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenIDAuthZenAccessEvaluationResultPostprocessorFactory.class);

	/**
	 * Extension ID, as returned by {@link #getId()}
	 */
	public static final String ID = "urn:ow2:authzforce:feature:pdp:result-postproc:openid-authzen";

	/**
	 * Constructor
	 */
	public OpenIDAuthZenAccessEvaluationResultPostprocessorFactory() {
		// nothing
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public Class<JSONObject> getResponseType() {
		return JSONObject.class;
	}

	@Override
	public Class<IndividualXacmlJsonRequest> getRequestType() {
		return IndividualXacmlJsonRequest.class;

	}

	@Override
	public DecisionResultPostprocessor<IndividualXacmlJsonRequest, JSONObject> getInstance(final int clientRequestErrorVerbosityLevel) {
		return RESULT_POSTPROCESSOR;
	}

	private static final DecisionResultPostprocessor<IndividualXacmlJsonRequest, JSONObject> RESULT_POSTPROCESSOR = new DecisionResultPostprocessor<>() {
		@Override
		public Class<IndividualXacmlJsonRequest> getRequestType() {
			return IndividualXacmlJsonRequest.class;
		}

		@Override
		public Class<JSONObject> getResponseType() {
			return JSONObject.class;
		}

		@Override
		public JSONObject process(final Collection<Entry<IndividualXacmlJsonRequest, ? extends DecisionResult>> resultsByRequest)
		{
			/*
			Multi-decision request is not supported in OpenIDAuthZenAccessEvaluationRequestPreprocessorFactory class.
			So no support for multiple results here.
			 */
			//FIXME: what should we return for decisions Indeterminate and NotApplicable? Here we chose to return false, but this behavior is not standardized.
			final DecisionResult result = resultsByRequest.iterator().next().getValue();
			return new JSONObject(Map.of("decision", result.getDecision() == DecisionType.PERMIT));
		}

		@Override
		public JSONObject processInternalError(final IndeterminateEvaluationException error)
		{
			LOGGER.error("Internal evaluation error", error);
			// Not standardized
			return new JSONObject(Map.of("error", error.getTopLevelStatus().getStatusMessage()));
		}

		@Override
		public JSONObject processClientError(final IndeterminateEvaluationException error)
		{
			LOGGER.info("Client error", error);
			final Status finalStatus = error.getTopLevelStatus();
			return new JSONObject(Map.of("error", finalStatus.getStatusMessage()));
		}
	};
}
