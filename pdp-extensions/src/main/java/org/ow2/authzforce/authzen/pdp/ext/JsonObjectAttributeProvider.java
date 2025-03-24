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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import org.json.JSONObject;
import org.ow2.authzforce.authzen.pdp.ext.xmlns.JsonObjectAttributeProviderDescriptor;
import org.ow2.authzforce.core.pdp.api.*;
import org.ow2.authzforce.core.pdp.api.value.*;
import org.ow2.authzforce.xacml.identifiers.XacmlStatusCode;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AttributeProvider that provide attribute values converted from properties of a JSON object nested under a given key in a root JSON object.
 * <p>
 * For example, with a root JSON object as follows:
 * <pre>
 *     {
 * 			"CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs": { "id":
 * 			"rick@the-citadel.com", "name": "Rick Sanchez", "email": "rick@the-citadel.com",
 * 			"roles": ["admin", "evil_genius"], "picture":
 * 			"https://www.topaz.sh/assets/templates/citadel/img/Rick%20Sanchez.jpg" },
 *
 * 			"CiRmZDM2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs": { "id":
 * 			"beth@the-smiths.com", "name": "Beth Smith", "email": "beth@the-smiths.com", "roles":
 * 			["viewer"], "picture":
 * 			"https://www.topaz.sh/assets/templates/citadel/img/Beth%20Smith.jpg" },
 * 		    ...
 * 		    }
 * </pre>
 * and a given key "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs" and requested attribute 'roles' (regardless of the Category),
 * the attribute provider returns the bag of strings ["admin", "evil_genius"].
 */
public final class JsonObjectAttributeProvider extends BaseNamedAttributeProvider {

    //private final AttributeSource attSource;
    private final Set<AttributeDesignatorType> supportedDesignatorTypes;
    //private final AttributeValueFactoryRegistry attValFactories;
    //private final NamedAttributeProvider depAttrProvider;
    private final AttributeFqn keyAttFqn;
    private final Map<String, Map<String, Object>> jsonObjectMap;

    /**
     * Instantiates the Attribute Provider
     *
     * @param instanceID instance ID (to be used as unique identifier for this instance in the logs for example);
     * @throws IllegalArgumentException if instanceId null
     */
    private JsonObjectAttributeProvider(String instanceID, /*AttributeValueFactoryRegistry attributeValueFactoryRegistry, final NamedAttributeProvider dependencyAttributeProvider,*/ final Map<String, Map<String, Object>> jsonObjectMap, final AttributeFqn keyAttributeFqn) throws IllegalArgumentException {
        super(instanceID);
        //this.attSource = AttributeSources.newCustomSource(this.getInstanceID());
        //this.attValFactories = attributeValueFactoryRegistry;
        //this.depAttrProvider = dependencyAttributeProvider;

        // Collect all property names in nested JSON object and use them to generated the set of provided attribute names (same category as keyAttributeFqn)
        final Set<String> nestedJsonObjectKeys = new HashSet<>();
        jsonObjectMap.forEach( (k,v) -> nestedJsonObjectKeys.addAll(v.keySet()));

        // TODO: support other attribute datatypes than string (so far, only string values are used in Authzen interop scenarios)
        this.supportedDesignatorTypes = nestedJsonObjectKeys.stream().map( propName -> new AttributeDesignatorType(keyAttributeFqn.getCategory(), propName, StandardDatatypes.STRING.getId(), null, false)).collect(Collectors.toUnmodifiableSet());

        this.jsonObjectMap = jsonObjectMap;
        this.keyAttFqn = keyAttributeFqn;
    }

    @Override
    public void close() throws IOException {
        // nothing to close
    }

    @SuppressFBWarnings(value="EI_EXPOSE_REP", justification="unmodifiable set")
    @Override
    public Set<AttributeDesignatorType> getProvidedAttributes() {
        return supportedDesignatorTypes;
    }

    @Override
    public <AV extends AttributeValue> AttributeBag<AV> get(AttributeFqn attributeFQN, Datatype<AV> datatype, EvaluationContext individualDecisionContext, Optional<EvaluationContext> mdpContext) throws IndeterminateEvaluationException {
        if(!datatype.equals(StandardDatatypes.STRING)) {
            // TODO: support other datatypes
            throw new IndeterminateEvaluationException("Unsupported datatype requested: "+datatype+" (!= STRING)", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        if(attributeFQN.getIssuer().isPresent()) {
            throw new IndeterminateEvaluationException("Invalid attribute Issuer requested (!= null)", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        if(!attributeFQN.getCategory().equals(this.keyAttFqn.getCategory())) {
            throw new IndeterminateEvaluationException("Invalid Category of attribute requested (!= '"+this.keyAttFqn.getCategory()+"')", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        final AttributeBag<StringValue> keyBag = individualDecisionContext.getNamedAttributeValue(this.keyAttFqn, StandardDatatypes.STRING);
        if(keyBag.isEmpty()) {
            // TODO: use this.depAttrProvider to get the attribute as fallback
            throw new IndeterminateEvaluationException("Required attribute "+keyAttFqn+" (used as key in JSON object) is missing in evaluation context", XacmlStatusCode.MISSING_ATTRIBUTE.value());
        }

        final String key = keyBag.iterator().next().getUnderlyingValue();
        final Map<String, Object> nestedJsonObj = this.jsonObjectMap.get(key);
        if(!nestedJsonObj.containsKey(attributeFQN.getId())) {
            throw new IndeterminateEvaluationException("Invalid AttributeId requested: '"+attributeFQN.getId()+"'. Does not match any property of the JSON object nested under key '"+key+"'", XacmlStatusCode.MISSING_ATTRIBUTE.value());
        }

        final Object jsonVal = nestedJsonObj.get(attributeFQN.getId());
        if(jsonVal instanceof Map) {
            throw new IndeterminateEvaluationException("The value of the requested property '"+attributeFQN.getId()+"' of the JSON object nested under key '"+key+"' is not supported (JSON object)", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        // If JSON array
       if(jsonVal instanceof List) {
           final Collection<AV> attVals = new ArrayList<>();
           for(final Object jsonArrayItem: (List<Object>) jsonVal) {
               final AV attVal = datatype.cast(new StringValue(jsonArrayItem.toString()));
               attVals.add(attVal);
           }
           return Bags.newAttributeBag(datatype, attVals);
       }

       final AV attVal = datatype.cast(new StringValue(jsonVal.toString()));
       return Bags.singletonAttributeBag(datatype, attVal);
    }

    private static class DepAwareAttProviderFactory implements DependencyAwareFactory
    {
        private final String providerId;
        private final AttributeFqn keyAttFqn;
        private final Map<String, Map<String, Object>> jsonObjectMap;

        private DepAwareAttProviderFactory(final String providerId, final Map<String, Map<String, Object>> jsonObjectMap, final AttributeFqn keyAttributeFqn) {
            assert providerId != null && keyAttributeFqn != null && jsonObjectMap != null;
            this.providerId = providerId;
            this.jsonObjectMap = jsonObjectMap;
            this.keyAttFqn = keyAttributeFqn;
        }

        @Override
        public Set<AttributeDesignatorType> getDependencies()
        {
            // no dependency
            return null;
        }

        @Override
        public CloseableNamedAttributeProvider getInstance(final AttributeValueFactoryRegistry attributeValueFactories, final NamedAttributeProvider depAttrProvider)
        {
            return new JsonObjectAttributeProvider(this.providerId, /*attributeValueFactories, depAttrProvider,*/ jsonObjectMap, keyAttFqn);
        }
    }

    /**
     * {@link JsonObjectAttributeProvider} factory
     *
     */
    public static class Factory extends CloseableNamedAttributeProvider.FactoryBuilder<JsonObjectAttributeProviderDescriptor>
    {

        @Override
        public Class<JsonObjectAttributeProviderDescriptor> getJaxbClass()
        {
            return JsonObjectAttributeProviderDescriptor.class;
        }

        @Override
        public DependencyAwareFactory getInstance(final JsonObjectAttributeProviderDescriptor conf, final EnvironmentProperties environmentProperties)
        {
            final String jsonFileLocation = environmentProperties.replacePlaceholders(conf.getJsonFileLocation());
            final JSONObject jsonObject;
            try {
                final File jsonFile = ResourceUtils.getFile(jsonFileLocation);
                final String jsonObjectAsStr = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
                jsonObject = new JSONObject(jsonObjectAsStr);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Attribute Provider '" + conf.getId() + "' - Invalid configuration: 'jsonFileLocation' is invalid (not found)", e);
            } catch (IOException e) {
                throw new IllegalArgumentException("Attribute Provider '" + conf.getId() + "' - Invalid configuration: 'jsonFileLocation' is invalid (cannot be read as text file with UTF-8 encoding)", e);
            }

            final Map<String, Object> inputMap = jsonObject.toMap();
            final Map<String, Map<String, Object>> mutMap = new HashMap<>();
            inputMap.forEach( (k,v) -> {
                // Value must be a JSON object
                if(!(v instanceof Map)) {
                    throw new IllegalArgumentException("Attribute Provider '" + conf.getId() + "' - Invalid configuration: 'jsonFileLocation' points to a JSON file with invalid key '"+k+"' (value is not a JSON object as expected)");
                }

                mutMap.put(k, (Map<String, Object>) v);
            });

            final AttributeDesignatorType keyAttDesignator = conf.getKey();
            if(!keyAttDesignator.getDataType().equals(StandardDatatypes.STRING.getId())) {
                throw new IllegalArgumentException("Attribute Provider '" + conf.getId() + "' - Invalid configuration: 'key' has invalid Datatype. Expected: " + StandardDatatypes.STRING);
            }

            final AttributeFqn keyAttFqn = AttributeFqns.newInstance(keyAttDesignator);
            return new DepAwareAttProviderFactory(conf.getId(), Map.copyOf(mutMap), keyAttFqn);
        }

    }
}
