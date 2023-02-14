/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2017 Kamax Sarl
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

package io.dpm.matrix.client;

import com.google.gson.JsonObject;

import io.dpm.matrix.client.regular.Presence;
import io.dpm.matrix.api.MatrixContent;
import io.dpm.matrix.api.MatrixID;
import io.dpm.matrix.api.MatrixUser;
import io.dpm.matrix.json.GsonUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java8.util.Optional;


import okhttp3.Request;

public class MatrixHttpUser extends AMatrixHttpClient implements MatrixUser {

    private Logger LOG = LoggerFactory.getLogger(MatrixHttpUser.class);

    private MatrixID mxId;

    public MatrixHttpUser(MatrixClientContext context, MatrixID mxId) {
        super(context);

        this.mxId = mxId;
    }

    @Override
    public MatrixID getId() {
        return mxId;
    }

    @Override
    public Optional<String> getName() {
        URL path = getClientPath("profile", mxId.getId(), "displayname");

        MatrixHttpRequest request = new MatrixHttpRequest(new Request.Builder().get().url(path));
        request.addIgnoredErrorCode(404);
        String body = executeAuthenticated(request);
        return extractAsStringFromBody(body, "displayname");
    }

    @Override
    public void setName(String name) {
        URL path = getClientPath("profile", mxId.getId(), "displayname");
        JsonObject body = GsonUtil.makeObj("displayname", name);
        executeAuthenticated(new Request.Builder().put(getJsonBody(body)).url(path));
    }

    @Override
    public Optional<String> getAvatarUrl() {
        URL path = getClientPath("profile", mxId.getId(), "avatar_url");

        MatrixHttpRequest request = new MatrixHttpRequest(new Request.Builder().get().url(path));
        request.addIgnoredErrorCode(404);
        String body = executeAuthenticated(request);
        return extractAsStringFromBody(body, "avatar_url");
    }

    @Override
    public void setAvatar(String avatarRef) {
        URL path = getClientPath("profile", mxId.getId(), "avatar_url");
        JsonObject body = GsonUtil.makeObj("avatar_url", avatarRef);
        executeAuthenticated(new Request.Builder().put(getJsonBody(body)).url(path));
    }

    @Override
    public void setAvatar(URI avatarUri) {
        setAvatar(avatarUri.toString());
    }

    @Override
    public Optional<MatrixContent> getAvatar() {
        return getAvatarUrl().flatMap(uri -> {
            try {
                return Optional.of(new MatrixHttpContent(getContext(), new URI(uri)));
            } catch (URISyntaxException e) {
                LOG.debug("{} is not a valid URI for avatar, returning empty", uri);
                return Optional.empty();
            }
        });
    }

    @Override
    public Optional<io.dpm.matrix.client.api.Presence> getPresence() {
        URL path = getClientPath("presence", mxId.getId(), "status");

        MatrixHttpRequest request = new MatrixHttpRequest(new Request.Builder().get().url(path));
        request.addIgnoredErrorCode(404);
        String body = execute(request);
        if (StringUtils.isBlank(body)) {
            return Optional.empty();
        }

        return Optional.of(new Presence(GsonUtil.parseObj(body)));
    }

}
