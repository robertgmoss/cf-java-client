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

package org.cloudfoundry.spring.doppler;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.doppler.ContainerMetric;
import org.cloudfoundry.doppler.ContainerMetricsRequest;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Event;
import org.cloudfoundry.doppler.FirehoseRequest;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.cloudfoundry.doppler.StreamRequest;
import org.cloudfoundry.spring.client.SpringCloudFoundryClient;
import org.cloudfoundry.spring.util.network.ConnectionContext;
import org.cloudfoundry.spring.util.network.SslCertificateTruster;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.timer.Timer;
import reactor.io.net.config.ClientSocketOptions;
import reactor.io.net.config.SslOptions;
import reactor.io.net.http.HttpClient;
import reactor.io.net.impl.netty.http.NettyHttpClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The Reactor-based implementation of {@link DopplerClient}
 */
@ToString
public final class ReactorDopplerClient implements DopplerClient {

    private final HttpClient<?, ?> httpClient;

    @Builder
    ReactorDopplerClient(@NonNull SpringCloudFoundryClient cloudFoundryClient) {
        this(getHttpClient(cloudFoundryClient.getConnectionContext()));
    }

    ReactorDopplerClient(HttpClient<?, ?> httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Flux<ContainerMetric> containerMetrics(ContainerMetricsRequest request) {
        this.httpClient
            .get("/apps/" + request.getApplicationId() + "/containermetrics")
            .toCompletableFuture()
            .join();

        return null;
    }

    @Override
    public Flux<Event> firehose(FirehoseRequest request) {
        return null; // TODO
    }

    @Override
    public Flux<LogMessage> recentLogs(RecentLogsRequest request) {
        return null; // TODO
    }

    @Override
    public Flux<Event> stream(StreamRequest request) {
        return null; // TODO
    }

    private static HttpClient<?, ?> getHttpClient(ConnectionContext connectionContext) {
        URI root = getRoot(connectionContext.getCloudFoundryClient(), connectionContext.getSslCertificateTruster());
        InetSocketAddress connectAddress = new InetSocketAddress(root.getHost(), root.getPort());

        return new NettyHttpClient(Timer.create(), connectAddress, new ClientSocketOptions(), new SslOptions());
    }

    private static URI getRoot(CloudFoundryClient cloudFoundryClient, SslCertificateTruster sslCertificateTruster) {
        URI uri = requestInfo(cloudFoundryClient)
            .map(GetInfoResponse::getDopplerLoggingEndpoint)
            .map(URI::create)
            .get(Duration.ofSeconds(5));

        sslCertificateTruster.trust(uri.getHost(), uri.getPort(), 5, SECONDS);
        return uri;
    }

    private static Mono<GetInfoResponse> requestInfo(CloudFoundryClient cloudFoundryClient) {
        return cloudFoundryClient.info()
            .get(GetInfoRequest.builder()
                .build());
    }

}
