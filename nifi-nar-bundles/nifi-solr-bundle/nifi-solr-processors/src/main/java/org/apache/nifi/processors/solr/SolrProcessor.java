/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.nifi.processors.solr;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A base class for processors that interact with Apache Solr.
 *
 */
public abstract class SolrProcessor extends AbstractProcessor {

    public static final AllowableValue SOLR_TYPE_CLOUD = new AllowableValue(
            "Cloud", "Cloud", "A SolrCloud instance.");

    public static final AllowableValue SOLR_TYPE_STANDARD = new AllowableValue(
            "Standard", "Standard", "A stand-alone Solr instance.");

    public static final PropertyDescriptor SOLR_TYPE = new PropertyDescriptor
            .Builder().name("Solr Type")
            .description("The type of Solr instance, Cloud or Standard.")
            .required(true)
            .allowableValues(SOLR_TYPE_CLOUD, SOLR_TYPE_STANDARD)
            .defaultValue(SOLR_TYPE_STANDARD.getValue())
            .build();

    public static final PropertyDescriptor SOLR_LOCATION = new PropertyDescriptor
            .Builder().name("Solr Location")
            .description("The Solr url for a Solr Type of Standard (ex: http://localhost:8984/solr/gettingstarted), " +
                    "or the ZooKeeper hosts for a Solr Type of Cloud (ex: localhost:9983).")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor COLLECTION = new PropertyDescriptor
            .Builder().name("Collection")
            .description("The Solr collection name, only used with a Solr Type of Cloud")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    private volatile SolrClient solrClient;

    @OnScheduled
    public final void onScheduled(final ProcessContext context) throws IOException {
        this.solrClient = createSolrClient(context);
    }

    /**
     * Create a SolrClient based on the type of Solr specified.
     *
     * @param context
     *          The context
     * @return an HttpSolrClient or CloudSolrClient
     */
    protected SolrClient createSolrClient(final ProcessContext context) {
        if (SOLR_TYPE_STANDARD.equals(context.getProperty(SOLR_TYPE).getValue())) {
            return new HttpSolrClient(context.getProperty(SOLR_LOCATION).getValue());
        } else {
            CloudSolrClient cloudSolrClient = new CloudSolrClient(
                    context.getProperty(SOLR_LOCATION).getValue());
            cloudSolrClient.setDefaultCollection(
                    context.getProperty(COLLECTION).evaluateAttributeExpressions().getValue());
            return cloudSolrClient;
        }
    }

    /**
     * Returns the {@link org.apache.solr.client.solrj.SolrClient} that was created by the
     * {@link #createSolrClient(org.apache.nifi.processor.ProcessContext)} method
     *
     * @return an HttpSolrClient or CloudSolrClient
     */
    protected final SolrClient getSolrClient() {
        return solrClient;
    }

    @Override
    protected final Collection<ValidationResult> customValidate(ValidationContext context) {
        final List<ValidationResult> problems = new ArrayList<>();

        if (SOLR_TYPE_CLOUD.equals(context.getProperty(SOLR_TYPE).getValue())) {
            final String collection = context.getProperty(COLLECTION).getValue();
            if (collection == null || collection.trim().isEmpty()) {
                problems.add(new ValidationResult.Builder()
                        .subject(COLLECTION.getName())
                        .input(collection).valid(false)
                        .explanation("A collection must specified for Solr Type of Cloud")
                        .build());
            }
        }

        Collection<ValidationResult> otherProblems = this.additionalCustomValidation(context);
        if (otherProblems != null) {
            problems.addAll(otherProblems);
        }

        return problems;
    }

    /**
     * Allows additional custom validation to be done. This will be called from
     * the parent's customValidation method.
     *
     * @param context
     *            The context
     * @return Validation results indicating problems
     */
    protected Collection<ValidationResult> additionalCustomValidation(ValidationContext context) {
        return new ArrayList<>();
    }

}
