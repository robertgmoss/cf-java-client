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

package org.cloudfoundry.spring.client.v2.services;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.jobs.JobEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v2.services.DeleteServiceRequest;
import org.cloudfoundry.client.v2.services.DeleteServiceResponse;
import org.cloudfoundry.client.v2.services.GetServiceRequest;
import org.cloudfoundry.client.v2.services.GetServiceResponse;
import org.cloudfoundry.client.v2.services.ListServiceServicePlansRequest;
import org.cloudfoundry.client.v2.services.ListServiceServicePlansResponse;
import org.cloudfoundry.client.v2.services.ListServicesRequest;
import org.cloudfoundry.client.v2.services.ListServicesResponse;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.cloudfoundry.spring.AbstractApiTest;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


public final class SpringServicesTest {

    public static final class Delete extends AbstractApiTest<DeleteServiceRequest, DeleteServiceResponse> {

        private final SpringServices services = new SpringServices(this.restTemplate, this.root, PROCESSOR_GROUP);

        @Override
        protected DeleteServiceRequest getInvalidRequest() {
            return DeleteServiceRequest.builder().build();
        }

        @Override
        protected RequestContext getRequestContext() {
            return new RequestContext()
                .method(DELETE).path("/v2/services/test-service-id?purge=true")
                .status(NO_CONTENT);
        }

        @Override
        protected DeleteServiceResponse getResponse() {
            return null;
        }

        @Override
        protected DeleteServiceRequest getValidRequest() throws Exception {
            return DeleteServiceRequest.builder()
                .purge(true)
                .serviceId("test-service-id")
                .build();
        }

        @Override
        protected Mono<DeleteServiceResponse> invoke(DeleteServiceRequest request) {
            return this.services.delete(request);
        }

    }

    public static final class DeleteAsync extends AbstractApiTest<DeleteServiceRequest, DeleteServiceResponse> {

        private final SpringServices services = new SpringServices(this.restTemplate, this.root, PROCESSOR_GROUP);

        @Override
        protected DeleteServiceRequest getInvalidRequest() {
            return DeleteServiceRequest.builder().build();
        }

        @Override
        protected RequestContext getRequestContext() {
            return new RequestContext()
                .method(DELETE).path("/v2/services/test-service-id?async=true")
                .status(ACCEPTED)
                .responsePayload("fixtures/client/v2/services/DELETE_{id}_async_response.json");
        }

        @Override
        protected DeleteServiceResponse getResponse() {
            return DeleteServiceResponse.builder()
                .metadata(Resource.Metadata.builder()
                    .id("2d9707ba-6f0b-4aef-a3de-fe9bdcf0c9d1")
                    .createdAt("2016-02-02T17:16:31Z")
                    .url("/v2/jobs/2d9707ba-6f0b-4aef-a3de-fe9bdcf0c9d1")
                    .build())
                .entity(JobEntity.builder()
                    .id("2d9707ba-6f0b-4aef-a3de-fe9bdcf0c9d1")
                    .status("queued")
                    .build())
                .build();
        }

        @Override
        protected DeleteServiceRequest getValidRequest() throws Exception {
            return DeleteServiceRequest.builder()
                .async(true)
                .serviceId("test-service-id")
                .build();
        }

        @Override
        protected Mono<DeleteServiceResponse> invoke(DeleteServiceRequest request) {
            return this.services.delete(request);
        }

    }

    public static final class Get extends AbstractApiTest<GetServiceRequest, GetServiceResponse> {

        private final SpringServices services = new SpringServices(this.restTemplate, this.root, PROCESSOR_GROUP);

        @Override
        protected GetServiceRequest getInvalidRequest() {
            return GetServiceRequest.builder()
                .build();
        }

        @Override
        protected RequestContext getRequestContext() {
            return new RequestContext()
                .method(GET).path("/v2/services/test-service-id")
                .status(OK)
                .responsePayload("fixtures/client/v2/services/GET_{id}_response.json");
        }

        @Override
        protected GetServiceResponse getResponse() {
            return GetServiceResponse.builder()
                .metadata(Resource.Metadata.builder()
                    .id("58eb36ad-0636-428b-b4ed-afc14e48d926")
                    .url("/v2/services/58eb36ad-0636-428b-b4ed-afc14e48d926")
                    .createdAt("2015-07-27T22:43:35Z")
                    .build())
                .entity(ServiceEntity.builder()
                    .label("label-86")
                    .description("desc-219")
                    .active(true)
                    .bindable(true)
                    .uniqueId("8fbdd3bc-3eee-4b03-97a3-57929484649b")
                    .serviceBrokerId("fe6e3f23-7b92-4855-aaa7-56f515d678c5")
                    .planUpdateable(false)
                    .servicePlansUrl("/v2/services/58eb36ad-0636-428b-b4ed-afc14e48d926/service_plans")
                    .build())
                .build();
        }

        @Override
        protected GetServiceRequest getValidRequest() throws Exception {
            return GetServiceRequest.builder()
                .serviceId("test-service-id")
                .build();
        }

        @Override
        protected Mono<GetServiceResponse> invoke(GetServiceRequest request) {
            return this.services.get(request);
        }

    }

    public static final class List extends AbstractApiTest<ListServicesRequest, ListServicesResponse> {

        private final SpringServices services = new SpringServices(this.restTemplate, this.root, PROCESSOR_GROUP);

        @Override
        protected ListServicesRequest getInvalidRequest() {
            return null;
        }

        @Override
        protected RequestContext getRequestContext() {
            return new RequestContext()
                .method(GET).path("/v2/services?q=label%20IN%20test-label&page=-1")
                .status(OK)
                .responsePayload("fixtures/client/v2/services/GET_response.json");
        }

        @Override
        protected ListServicesResponse getResponse() {
            return ListServicesResponse.builder()
                .totalResults(1)
                .totalPages(1)
                .resource(ServiceResource.builder()
                    .metadata(Resource.Metadata.builder()
                        .id("69b84c38-e786-4270-9cca-59d02a700798")
                        .url("/v2/services/69b84c38-e786-4270-9cca-59d02a700798")
                        .createdAt("2015-07-27T22:43:35Z")
                        .build())
                    .entity(ServiceEntity.builder()
                        .label("label-87")
                        .description("desc-220")
                        .active(true)
                        .bindable(true)
                        .uniqueId("e46b095e-aa85-4ffb-98d9-0bc94b84d45c")
                        .serviceBrokerId("5c323c18-e26c-45ff-a4f9-6a8916912a22")
                        .planUpdateable(false)
                        .servicePlansUrl("/v2/services/69b84c38-e786-4270-9cca-59d02a700798/service_plans")
                        .build())
                    .build())
                .build();
        }

        @Override
        protected ListServicesRequest getValidRequest() throws Exception {
            return ListServicesRequest.builder()
                .label("test-label")
                .page(-1)
                .build();
        }

        @Override
        protected Mono<ListServicesResponse> invoke(ListServicesRequest request) {
            return this.services.list(request);
        }

    }

    public static final class ListServicePlans extends AbstractApiTest<ListServiceServicePlansRequest, ListServiceServicePlansResponse> {

        private final SpringServices services = new SpringServices(this.restTemplate, this.root, PROCESSOR_GROUP);

        @Override
        protected ListServiceServicePlansRequest getInvalidRequest() {
            return ListServiceServicePlansRequest.builder().build();
        }

        @Override
        protected RequestContext getRequestContext() {
            return new RequestContext()
                .method(GET).path("/v2/services/f1b0edbe-fac4-4512-9071-8b26045413bb/service_plans?page=-1")
                .status(OK)
                .responsePayload("fixtures/client/v2/services/GET_{id}_service_plans_response.json");
        }

        @Override
        protected ListServiceServicePlansResponse getResponse() {
            return ListServiceServicePlansResponse.builder()
                .totalResults(1)
                .totalPages(1)
                .resource(ServicePlanResource.builder()
                    .metadata(Resource.Metadata.builder()
                        .createdAt("2015-07-27T22:43:35Z")
                        .id("51067400-d79f-4ca5-9400-1f36f5dd09e7")
                        .url("/v2/service_plans/51067400-d79f-4ca5-9400-1f36f5dd09e7")
                        .build())
                    .entity(ServicePlanEntity.builder()
                        .name("name-2409")
                        .free(false)
                        .description("desc-218")
                        .serviceId("f1b0edbe-fac4-4512-9071-8b26045413bb")
                        .uniqueId("48fb5a34-1c14-4da5-944e-a14fa1ba5325")
                        .publiclyVisible(true)
                        .active(true)
                        .serviceUrl("/v2/services/f1b0edbe-fac4-4512-9071-8b26045413bb")
                        .serviceInstancesUrl("/v2/service_plans/51067400-d79f-4ca5-9400-1f36f5dd09e7/service_instances")
                        .build())
                    .build())
                .build();
        }

        @Override
        protected ListServiceServicePlansRequest getValidRequest() throws Exception {
            return ListServiceServicePlansRequest.builder()
                .serviceId("f1b0edbe-fac4-4512-9071-8b26045413bb")
                .page(-1)
                .build();
        }

        @Override
        protected Mono<ListServiceServicePlansResponse> invoke(ListServiceServicePlansRequest request) {
            return this.services.listServicePlans(request);
        }
    }

}
