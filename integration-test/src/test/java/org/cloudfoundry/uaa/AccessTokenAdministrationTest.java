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

package org.cloudfoundry.uaa;

import org.cloudfoundry.AbstractIntegrationTest;
import org.cloudfoundry.uaa.accesstokenadministration.GetTokenKeyRequest;
import org.cloudfoundry.uaa.accesstokenadministration.GetTokenKeyResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class AccessTokenAdministrationTest extends AbstractIntegrationTest {

    @Autowired
    private UaaClient uaaClient;

    @Test
    public void getTokenKey() {
        this.uaaClient.accessTokenAdministration()
            .getTokenKey(GetTokenKeyRequest.builder()
                .build())
            .subscribe(this.<GetTokenKeyResponse>testSubscriber()
                .assertThat(response -> {
                    assertEquals("sig", response.getUse());
                    assertNotNull(response.getValue());
                }));
    }

}
