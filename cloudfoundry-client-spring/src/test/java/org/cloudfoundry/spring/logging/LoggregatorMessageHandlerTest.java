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

package org.cloudfoundry.spring.logging;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.cloudfoundry.logging.LogMessage;
import org.cloudfoundry.logging.LoggregatorProtocolBuffers;
import org.cloudfoundry.util.test.TestSubscriber;
import org.junit.Test;

import java.time.Duration;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.cloudfoundry.logging.LogMessage.MessageType.ERR;

public final class LoggregatorMessageHandlerTest {

    private final TestSubscriber<LogMessage> testSubscriber = new TestSubscriber<>();

    private final LoggregatorMessageHandler messageHandler = new LoggregatorMessageHandler(this.testSubscriber);

    @Test
    public void onMessage() throws InterruptedException {
        Date timestamp = new Date();

        LoggregatorProtocolBuffers.LogMessage logMessage = LoggregatorProtocolBuffers.LogMessage.newBuilder()
            .setAppId("test-app-id")
            .addDrainUrls("test-drain-url")
            .setMessage(ByteString.copyFromUtf8("test-message"))
            .setMessageType(LoggregatorProtocolBuffers.LogMessage.MessageType.ERR)
            .setSourceId("test-source-id")
            .setSourceName("test-source-name")
            .setTimestamp(MILLISECONDS.toNanos(timestamp.getTime()))
            .build();

        this.messageHandler.onMessage(logMessage.toByteArray());

        this.testSubscriber.onComplete();
        this.testSubscriber
            .assertEquals(LogMessage.builder()
                .applicationId("test-app-id")
                .drainUrl("test-drain-url")
                .message("test-message")
                .messageType(ERR)
                .sourceId("test-source-id")
                .sourceName("test-source-name")
                .timestamp(timestamp)
                .build())
            .verify(Duration.ofSeconds(5));
    }

    @Test
    public void onMessageError() throws InterruptedException {
        this.messageHandler.onMessage(new byte[0]);

        this.testSubscriber
            .assertError(InvalidProtocolBufferException.class, "Message missing required fields: message, message_type, timestamp, app_id")
            .verify(Duration.ofSeconds(5));
    }

}
