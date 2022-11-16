/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.dpm.matrix.client.regular;

import io.dpm.matrix.MatrixIDImpl;
import io.dpm.matrix.client.MatrixClientContext;
import io.dpm.matrix.hs.MatrixHomeserverImpl;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class MatrixHttpClientSyncWiremockTest extends AMatrixHttpClientSyncTest {

    private final String syncPath = "/_matrix/client/r0/sync";

    private String getJson() {
        try {
            InputStream is = new FileInputStream("src/test/resources/json/client/syncInitial.json");
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Override
    public void getInitialSync() throws Exception {
        stubFor(get(urlPathEqualTo(syncPath)).willReturn(aResponse().withStatus(200).withBody(getJson())));

        MatrixHomeserverImpl hs = new MatrixHomeserverImpl(domain, baseUrl);
        MatrixClientContext context = new MatrixClientContext(hs, MatrixIDImpl.asValid("@user:localhost"), "test");
        MatrixHttpClient client = new MatrixHttpClient(context);
        setClient(client);

        super.getInitialSync();

        verify(getRequestedFor(urlPathEqualTo(syncPath)));
    }

    @Test
    @Override
    public void getSeveralSync() throws Exception {
        stubFor(get(urlPathEqualTo(syncPath)).willReturn(aResponse().withStatus(200).withBody(getJson())));
        stubFor(get(urlPathEqualTo(syncPath)).withQueryParam("access_token", equalTo("test"))
                .withQueryParam("timeout", equalTo("0")).willReturn(aResponse().withStatus(200).withBody(getJson())));

        MatrixHomeserverImpl hs = new MatrixHomeserverImpl(domain, baseUrl);
        MatrixClientContext context = new MatrixClientContext(hs, MatrixIDImpl.asValid("@user:localhost"), "test");
        MatrixHttpClient client = new MatrixHttpClient(context);
        setClient(client);

        super.getSeveralSync();

        verify(getRequestedFor(urlPathEqualTo(syncPath)));
        verify(getRequestedFor(urlPathEqualTo(syncPath)).withQueryParam("timeout", equalTo("0")));
    }

}
