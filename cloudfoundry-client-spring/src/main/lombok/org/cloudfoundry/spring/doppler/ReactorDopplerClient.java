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
import reactor.io.buffer.Buffer;
import reactor.io.ipc.ChannelFlux;
import reactor.io.netty.config.ClientSocketOptions;
import reactor.io.netty.config.SslOptions;
import reactor.io.netty.http.HttpClient;
import reactor.io.netty.http.NettyHttpClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The Reactor-based implementation of {@link DopplerClient}
 */
@ToString
public final class ReactorDopplerClient implements DopplerClient {

    private final ConnectionContext connectionContext; // TODO: Remove

    private final HttpClient<Buffer, Buffer> httpClient;

    @Builder
    ReactorDopplerClient(@NonNull SpringCloudFoundryClient cloudFoundryClient) {
        this(getHttpClient(cloudFoundryClient.getConnectionContext()), cloudFoundryClient.getConnectionContext());
    }

    ReactorDopplerClient(HttpClient<Buffer, Buffer> httpClient, ConnectionContext connectionContext) {
        this.httpClient = httpClient;
        this.connectionContext = connectionContext;
    }

    @SuppressWarnings("rawtypes")  // TODO: Remove
    @Override
    public Flux<ContainerMetric> containerMetrics(ContainerMetricsRequest request) {
        return this.httpClient
            .get("https://doppler.run.pivotal.io/apps/" + request.getApplicationId() + "/containermetrics", channel -> channel
                .header("Authorization", getToken(this.connectionContext))
                .writeHeaders())
            .log("stream.afterGet")
            .doOnSuccess(hc -> {
                hc.responseHeaders().entries().stream()
                    .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
            })
            .flatMap(ChannelFlux::input)
            .log("stream.afterInput")
            .map(Buffer::asString)
            .log("stream.afterString")
            .cast(ContainerMetric.class);
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

    private static HttpClient<Buffer, Buffer> getHttpClient(ConnectionContext connectionContext) {
        URI root = getRoot(connectionContext.getCloudFoundryClient(), connectionContext.getSslCertificateTruster());
        InetSocketAddress connectAddress = new InetSocketAddress(root.getHost(), root.getPort());

        return new NettyHttpClient(Timer.create(), connectAddress, new ClientSocketOptions(), new SslOptions())
//            .preprocessor(channel -> connectionContext.getCloudFoundryClient()
//                .getAccessToken()
//                .map(accessToken -> channel.addHeader("Authorization", accessToken))
//                .get())
            ;
    }

    private static URI getRoot(CloudFoundryClient cloudFoundryClient, SslCertificateTruster sslCertificateTruster) {
        URI uri = requestInfo(cloudFoundryClient)
            .map(GetInfoResponse::getDopplerLoggingEndpoint)
            .map(URI::create)
            .get(Duration.ofSeconds(5));

        sslCertificateTruster.trust(uri.getHost(), uri.getPort(), 5, SECONDS);
        return uri;
    }

    private static String getToken(ConnectionContext connectionContext) {
        return connectionContext.getCloudFoundryClient().getAccessToken()
            .map(t -> String.format("bearer %s", t))
            .get();
    }

    private static Mono<GetInfoResponse> requestInfo(CloudFoundryClient cloudFoundryClient) {
        return cloudFoundryClient.info()
            .get(GetInfoRequest.builder()
                .build());
    }

}
