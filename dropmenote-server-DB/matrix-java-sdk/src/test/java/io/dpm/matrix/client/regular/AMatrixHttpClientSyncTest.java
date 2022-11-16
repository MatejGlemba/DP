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

import io.dpm.matrix.client.MatrixHttpTest;
import io.dpm.matrix.client.api.SyncData;

import org.junit.platform.commons.util.StringUtils;

import static junit.framework.TestCase.*;

import static org.junit.Assert.assertNotNull;

public abstract class AMatrixHttpClientSyncTest extends MatrixHttpTest {

    private MatrixHttpClient client;

    protected void setClient(MatrixHttpClient client) {
        this.client = client;
    }

    private void validateSyncData(SyncData data) {
        assertNotNull(data);
        assertTrue(StringUtils.isNotBlank(data.nextBatchToken()));
        assertNotNull(data.getAccountData());
        assertNotNull(data.getRooms());
        assertNotNull(data.getRooms().getInvited());
        assertNotNull(data.getRooms().getJoined());
        assertNotNull(data.getRooms().getLeft());
    }

    public void getInitialSync() throws Exception {
        SyncData data = client.sync(SyncOptions.build().get());
        validateSyncData(data);
        assertFalse(data.getAccountData().getEvents().isEmpty());
    }

    public void getSeveralSync() throws Exception {
        SyncData data = client.sync(SyncOptions.build().get());
        data = client.sync(SyncOptions.build().setSince(data.nextBatchToken()).setTimeout(0).get());
        validateSyncData(data);
    }

}
