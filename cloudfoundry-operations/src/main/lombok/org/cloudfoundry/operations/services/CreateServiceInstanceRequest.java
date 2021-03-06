/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.operations.services;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.cloudfoundry.Validatable;
import org.cloudfoundry.ValidationResult;

import java.util.List;
import java.util.Map;

/**
 * The request options for the create service instance operation
 */
@Data
public final class CreateServiceInstanceRequest implements Validatable {

    /**
     * The parameters of the service instance
     *
     * @param parameters the parameters
     * @return the parameters
     */
    private final Map<String, Object> parameters;

    /**
     * The name of the service plan to use
     *
     * @param planName the name of the service plan
     * @return the name of the service plan
     */
    private final String planName;

    /**
     * The name of the service instance to create
     *
     * @param serviceInstanceName the name of the service instance
     * @return the name of the service instance
     */
    private final String serviceInstanceName;

    /**
     * The name of the service
     *
     * @param serviceName the name of the service
     * @return the name of the service
     */
    private final String serviceName;

    /**
     * The tags
     *
     * @param tags the tags
     * @return the tags
     */
    private final List<String> tags;

    @Builder
    CreateServiceInstanceRequest(@Singular Map<String, Object> parameters,
                                 String planName,
                                 String serviceName,
                                 String serviceInstanceName,
                                 @Singular List<String> tags) {
        this.parameters = parameters;
        this.planName = planName;
        this.serviceName = serviceName;
        this.serviceInstanceName = serviceInstanceName;
        this.tags = tags;
    }

    @Override
    public ValidationResult isValid() {
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();

        if (this.planName == null) {
            builder.message("service plan name must be specified");
        }

        if (this.serviceName == null) {
            builder.message("service name must be specified");
        }

        if (this.serviceInstanceName == null) {
            builder.message("service instance name must be specified");
        }

        return builder.build();
    }

}
