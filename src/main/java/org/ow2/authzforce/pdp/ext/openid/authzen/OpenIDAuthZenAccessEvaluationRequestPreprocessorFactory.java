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
package org.ow2.authzforce.pdp.ext.openid.authzen;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.authzforce.core.pdp.api.AttributeFqn;
import org.ow2.authzforce.core.pdp.api.AttributeFqns;
import org.ow2.authzforce.core.pdp.api.DecisionRequestPreprocessor;
import org.ow2.authzforce.core.pdp.api.ImmutableDecisionRequest;
import org.ow2.authzforce.core.pdp.api.value.AttributeBag;
import org.ow2.authzforce.core.pdp.api.value.AttributeValue;
import org.ow2.authzforce.core.pdp.api.value.AttributeValueFactoryRegistry;
import org.ow2.authzforce.core.pdp.api.value.Bags;
import org.ow2.authzforce.core.pdp.api.value.StandardAttributeValueFactories;
import org.ow2.authzforce.core.pdp.api.value.StringParseableValue;
import org.ow2.authzforce.core.pdp.io.xacml.json.IndividualXacmlJsonRequest;
import org.ow2.authzforce.xacml.identifiers.XacmlAttributeCategory;

/**
 * OpenID AuthZEN Access Evaluation API - Request Preprocessor.
 * <p>
 *     Links:
 *     <ul>
 *         <li><a href="https://openid.net/wg/authzen/">OpenID AuthZEN Working Group</a></li>
 *         <li><a href="https://github.com/openid/authzen/blob/main/api/authorization-api-1_0.md#access-evaluations-api">Access Evaluation API</a></li>
 *         <li><a href="https://www.postman.com/axiomatics/workspace/authzen-sample-requests/">AuthZEN Sample Request/Response Collection</a></li>
 *     </ul>
 *
 */
public final class OpenIDAuthZenAccessEvaluationRequestPreprocessorFactory implements DecisionRequestPreprocessor.Factory<JSONObject, IndividualXacmlJsonRequest> {
	//private static final Logger LOGGER = LoggerFactory.getLogger(OpenIDAuthZenAccessEvaluationRequestPreprocessorFactory.class);

	/**
	 * Request preprocessor ID, as returned by {@link #getId()}
	 */
	public static final String ID = "urn:ow2:authzforce:feature:pdp:request-preproc:openid-authzen";

	/**
	 * Constructor
	 */
	public OpenIDAuthZenAccessEvaluationRequestPreprocessorFactory() {
		// nothing
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public Class<JSONObject> getInputRequestType() {
		return JSONObject.class;
	}

	@Override
	public Class<IndividualXacmlJsonRequest> getOutputRequestType() {
		return IndividualXacmlJsonRequest.class;

	}

	@Override
	public DecisionRequestPreprocessor<JSONObject, IndividualXacmlJsonRequest> getInstance(final AttributeValueFactoryRegistry datatypeFactoryRegistry, boolean strictAttributeIssuerMatch, boolean requireContentForXPath, final Set<String> extraPdpEngineFeatures) {
		return REQUEST_PREPROCESSOR;
	}

	private static final DecisionRequestPreprocessor<JSONObject, IndividualXacmlJsonRequest> REQUEST_PREPROCESSOR = new DecisionRequestPreprocessor<>() {

		private static final UnsupportedOperationException UNSUPPORTED_MULTI_DECISION_REQUEST_EXCEPTION = new UnsupportedOperationException("Sending more than one decision request with 'queries' array is not supported");

		private static final Map<XacmlAttributeCategory, String> XACML_TO_AUTHZEN_CATEGORY_MAP = new EnumMap<>(Map.of(XacmlAttributeCategory.XACML_1_0_ACCESS_SUBJECT, "subject", XacmlAttributeCategory.XACML_3_0_ACTION, "action", XacmlAttributeCategory.XACML_3_0_RESOURCE, "resource", XacmlAttributeCategory.XACML_3_0_ENVIRONMENT, "context"));

		private static final IllegalArgumentException NULL_REQUEST_ARGUMENT_EXCEPTION = new IllegalArgumentException("Null request arg");

		private static final IllegalArgumentException NULL_ATTRIBUTE_VALUE_EXCEPTION = new IllegalArgumentException("Invalid JSON value as attribute value: null");

		private static StringParseableValue.Factory<?> getAttributeValueFactory(final Object simpleJsonValue) {
			if (simpleJsonValue instanceof String) {
				return StandardAttributeValueFactories.STRING;
			}

			if (simpleJsonValue instanceof Boolean) {
				return StandardAttributeValueFactories.BOOLEAN;
			}

			if (simpleJsonValue instanceof Integer) {
				return StandardAttributeValueFactories.MEDIUM_INTEGER;
			}

			if (simpleJsonValue instanceof Long) {
				return StandardAttributeValueFactories.LONG_INTEGER;
			}

			if (simpleJsonValue instanceof BigInteger) {
				return StandardAttributeValueFactories.BIG_INTEGER;
			}

			if (simpleJsonValue instanceof Float || simpleJsonValue instanceof Double) {
				return StandardAttributeValueFactories.DOUBLE;
			}

			if (simpleJsonValue == null) {
				throw NULL_ATTRIBUTE_VALUE_EXCEPTION;
			}

			throw new IllegalArgumentException("Invalid type of attribute value: " + simpleJsonValue.getClass());
		}

		private static <AV extends AttributeValue> AttributeBag<AV> newAttributeBag(final StringParseableValue.Factory<AV> attributeValueFactory, final Serializable simpleJsonValue) {
			return Bags.singletonAttributeBag(attributeValueFactory.getDatatype(), attributeValueFactory.getInstance(simpleJsonValue));
		}

		private static <AV extends AttributeValue> AttributeBag<AV> newAttributeBag(final StringParseableValue.Factory<AV> attributeValueFactory, final JSONArray jsonValues) {
			final Collection<AV> attributeValues = new ArrayList<>(jsonValues.length());
			for (final Object jsonVal : jsonValues) {
				if (jsonVal instanceof Serializable) {
					attributeValues.add(attributeValueFactory.getInstance((Serializable) jsonVal));
				}
				else {
					throw new IllegalArgumentException("Invalid type of attribute value in JSON array (not Serializable): " + jsonVal.getClass());
				}
			}

			return Bags.newAttributeBag(attributeValueFactory.getDatatype(), attributeValues);
		}

		@Override
		public Class<JSONObject> getInputRequestType() {
			return JSONObject.class;
		}

		@Override
		public Class<IndividualXacmlJsonRequest> getOutputRequestType() {
			return IndividualXacmlJsonRequest.class;
		}

		@Override
		public List<IndividualXacmlJsonRequest> process(JSONObject request, Map<String, String> namespaceURIsByPrefix)
		{
			if (request == null) {
				throw NULL_REQUEST_ARGUMENT_EXCEPTION;
			}

		/*
		Example of OpenID AuthZEN (JSON) request:

		{
			"subject": {
				"identity": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
			},
			"action": {
				"name": "can_read_user"
			},
			"resource": {
				"type": "user",
				"userID": "beth@the-smiths.com"
			},
			context: {}
		}
		 */

		/*
		TODO: JSON schema validation. OpenID AuthZen does not specify any JSON schema for now.
		 */

			if(request.has("queries")) {
				// Access Evaluations (multiple) Request
				throw UNSUPPORTED_MULTI_DECISION_REQUEST_EXCEPTION;
			}

			final Map<AttributeFqn, AttributeBag<?>> namedAttributes = new HashMap<>();
			XACML_TO_AUTHZEN_CATEGORY_MAP.forEach((xacmlCat, authzenCat) ->
			{
				final JSONObject authzenCatAttributes = request.optJSONObject(authzenCat);
				if (authzenCatAttributes != null) {
					authzenCatAttributes.keySet().forEach(attId ->
					{
						final AttributeFqn attName = AttributeFqns.newInstance(xacmlCat.value(), Optional.empty(), attId);
						final AttributeBag<?> attBag;
						final Object authzenAttVal = authzenCatAttributes.get(attId);
						if (authzenAttVal instanceof JSONArray authzenAttValArray) {
							// Multi-valued attribute
							if (!authzenAttValArray.isEmpty()) {
								try {
									final StringParseableValue.Factory<?> attValFactory = getAttributeValueFactory(authzenAttValArray.get(0));
									attBag = newAttributeBag(attValFactory, (JSONArray) authzenAttVal);
								}
								catch (IllegalArgumentException e) {
									throw new IllegalArgumentException("Invalid JSON value for attribute '" + attId + "'", e);
								}

								namedAttributes.put(attName, attBag);
							}
						}
						else if (authzenAttVal instanceof Serializable singleAuthzenAttVal) {
							// Single-valued atttribute
							try {
								final StringParseableValue.Factory<?> attValFactory = getAttributeValueFactory(singleAuthzenAttVal);
								attBag = newAttributeBag(attValFactory, singleAuthzenAttVal);
							}
							catch (IllegalArgumentException e) {
								throw new IllegalArgumentException("Invalid JSON value for attribute '" + attId + "'", e);
							}

							namedAttributes.put(attName, attBag);
						}
						else {
							throw new IllegalArgumentException("Invalid type of JSON value for attribute '" + attId + "'");
						}
					});
				}
			});

			final IndividualXacmlJsonRequest xacmlJsonRequest = new IndividualXacmlJsonRequest(ImmutableDecisionRequest.getInstance(namedAttributes, null, false), null);
			return List.of(xacmlJsonRequest);
		}
	};
}
