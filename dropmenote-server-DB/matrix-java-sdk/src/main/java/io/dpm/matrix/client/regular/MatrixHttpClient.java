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

package io.dpm.matrix.client.regular;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.dpm.matrix.MatrixIDImpl;
import io.dpm.matrix.api.MatrixContent;
import io.dpm.matrix.api.MatrixID;
import io.dpm.matrix.api.MatrixUser;
import io.dpm.matrix.client.*;
import io.dpm.matrix.client.api.MatrixClient;
import io.dpm.matrix.client.api.PushRule;
import io.dpm.matrix.client.api.SyncData;
import io.dpm.matrix.json.*;
import io.dpm.matrix.client.*;
import io.dpm.matrix.client.api.*;
import io.dpm.matrix.client.api.SyncOptions;
import io.dpm.matrix.hs.api.MatrixRoom;
import io.dpm.matrix.json.*;
import io.dpm.matrix.room.RoomAlias;
import io.dpm.matrix.room.RoomAliasLookupImpl;
import io.dpm.matrix.room.api.RoomAliasLookup;
import io.dpm.matrix.room.api.RoomCreationOptions;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java8.util.Optional;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;


import okhttp3.*;

public class MatrixHttpClient extends AMatrixHttpClient implements MatrixClient {

    private Logger LOG = LoggerFactory.getLogger(MatrixHttpClient.class);

    public MatrixHttpClient(String domain) {
        super(domain);
    }

    public MatrixHttpClient(URL hsBaseUrl) {
        super(hsBaseUrl);
    }

    public MatrixHttpClient(MatrixClientContext context) {
        super(context);
    }

    public MatrixHttpClient(MatrixClientContext context, OkHttpClient.Builder client) {
        super(context, client);
    }

    public MatrixHttpClient(MatrixClientContext context, OkHttpClient.Builder client, MatrixClientDefaults defaults) {
        super(context, client, defaults);
    }

    public MatrixHttpClient(MatrixClientContext context, OkHttpClient client) {
        super(context, client);
    }

    protected MatrixID getIdentity(String token) {
        URL path = getClientPath("account", "whoami");
        String body = executeAuthenticated(new Request.Builder().get().url(path), token);
        return MatrixIDImpl.from(GsonUtil.getStringOrThrow(GsonUtil.parseObj(body), "user_id")).acceptable();
    }

    @Override
    public MatrixID getWhoAmI() {
        URL path = getClientPath("account", "whoami");
        String body = executeAuthenticated(new Request.Builder().get().url(path));
        return MatrixIDImpl.from(GsonUtil.getStringOrThrow(GsonUtil.parseObj(body), "user_id")).acceptable();
    }

    @Override
    public void setDisplayName(String name) {
        URL path = getClientPath("profile", getUserId(), "displayname");
        execute(new Request.Builder().put(getJsonBody(new UserDisplaynameSetBody(name))).url(path));
    }

    @Override
    public RoomAliasLookup lookup(RoomAlias alias) {
        URL path = getClientPath("directory", "room", alias.getId());
        String resBody = execute(new Request.Builder().get().url(path));
        RoomAliasLookupJson lookup = GsonUtil.get().fromJson(resBody, RoomAliasLookupJson.class);
        return new RoomAliasLookupImpl(lookup.getRoomId(), alias.getId(), lookup.getServers());
    }

    @Override
    public MatrixRoom createRoom(RoomCreationOptions options) {
        URL path = getClientPath("createRoom");
        String resBody = executeAuthenticated(
                new Request.Builder().post(getJsonBody(new RoomCreationRequestJson(options))).url(path));
        String roomId = GsonUtil.get().fromJson(resBody, RoomCreationResponseJson.class).getRoomId();
        return getRoom(roomId);
    }

    @Override
    public MatrixRoom getRoom(String roomId) {
        return new MatrixHttpRoom(getContext(), roomId);
    }

    @Override
    public List<MatrixRoom> getJoinedRooms() {
        URL path = getClientPath("joined_rooms");
        JsonObject resBody = GsonUtil.parseObj(executeAuthenticated(new Request.Builder().get().url(path)));
        return StreamSupport.stream(GsonUtil.asList(resBody, "joined_rooms", String.class)).map(this::getRoom)
                .collect(Collectors.toList());
    }

    @Override
    public MatrixRoom joinRoom(String roomIdOrAlias) {
        URL path = getClientPath("join", roomIdOrAlias);
        String resBody = executeAuthenticated(new Request.Builder().post(getJsonBody(new JsonObject())).url(path));
        String roomId = GsonUtil.get().fromJson(resBody, RoomCreationResponseJson.class).getRoomId();
        return getRoom(roomId);
    }

    @Override
    public MatrixUser getUser(MatrixID mxId) {
        return new MatrixHttpUser(getContext(), mxId);
    }

    @Override
    public Optional<String> getDeviceId() {
        return Optional.ofNullable(context.getDeviceId());
    }

    protected void updateContext(String resBody) {
        LoginResponse response = gson.fromJson(resBody, LoginResponse.class);
        context.setToken(response.getAccessToken());
        context.setDeviceId(response.getDeviceId());
        context.setUser(MatrixIDImpl.asAcceptable(response.getUserId()));
    }

    @Override
    public void register(MatrixPasswordCredentials credentials, String sharedSecret, boolean admin) {
        // As per synapse registration script:
        // https://github.com/matrix-org/synapse/blob/master/scripts/register_new_matrix_user#L28

        String value = credentials.getLocalPart() + "\0" + credentials.getPassword() + "\0"
                + (admin ? "admin" : "notadmin");
        String mac = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, sharedSecret).hmacHex(value);
        JsonObject body = new JsonObject();
        body.addProperty("user", credentials.getLocalPart());
        body.addProperty("password", credentials.getPassword());
        body.addProperty("mac", mac);
        body.addProperty("type", "org.matrix.login.shared_secret");
        body.addProperty("admin", false);
        URL url = getPath("client", "api", "v1", "register");
        updateContext(execute(new Request.Builder().post(getJsonBody(body)).url(url)));
    }

    @Override
    public void setAccessToken(String accessToken) {
        context.setUser(getIdentity(accessToken));
        context.setToken(accessToken);
    }

    @Override
    public void login(MatrixPasswordCredentials credentials) {
        URL url = getClientPath("login");

        LoginPostBody data = new LoginPostBody(credentials.getLocalPart(), credentials.getPassword());
        getDeviceId().ifPresent(data::setDeviceId);
        Optional.ofNullable(context.getInitialDeviceName()).ifPresent(data::setInitialDeviceDisplayName);

        updateContext(execute(new Request.Builder().post(getJsonBody(data)).url(url)));
    }

    @Override
    public void logout() {
        URL path = getClientPath("logout");
        executeAuthenticated(new Request.Builder().post(getJsonBody(new JsonObject())).url(path));
        context.setToken(null);
        context.setUser(null);
        context.setDeviceId(null);
    }

    @Override
    public SyncData sync(SyncOptions options) {
        long start = System.currentTimeMillis();
        HttpUrl.Builder path = getClientPathBuilder("sync");
        path.addQueryParameter("timeout", options.getTimeout().map(Long::intValue).orElse(30000).toString());
        options.getSince().ifPresent(since -> path.addQueryParameter("since", since));
        options.getFilter().ifPresent(filter -> path.addQueryParameter("filter", filter));
        options.withFullState().ifPresent(state -> path.addQueryParameter("full_state", state ? "true" : "false"));
        options.getSetPresence().ifPresent(presence -> path.addQueryParameter("presence", presence));

        String body = executeAuthenticated(new Request.Builder().get().url(path.build().url()));
        long request = System.currentTimeMillis();
        LOG.debug("Sync: network request took {} ms", (request - start));
        SyncDataJson data = new SyncDataJson(GsonUtil.parseObj(body));
        long parsing = System.currentTimeMillis();
        LOG.debug("Sync: parsing took {} ms", (parsing - request));
        return data;
    }

    @Override
    public MatrixContent getMedia(String mxUri) throws IllegalArgumentException {
        return getMedia(URI.create(mxUri));
    }

    @Override
    public MatrixContent getMedia(URI mxUri) throws IllegalArgumentException {
        return new MatrixHttpContent(context, mxUri);
    }

    private String putMedia(Request.Builder builder, String filename) {
        HttpUrl.Builder b = getMediaPathBuilder("upload");
        if (StringUtils.isNotEmpty(filename)) b.addQueryParameter("filename", filename);

        String body = executeAuthenticated(builder.url(b.build()));
        return GsonUtil.getStringOrThrow(GsonUtil.parseObj(body), "content_uri");
    }

    @Override
    public String putMedia(byte[] data, String type) {
        return putMedia(data, type, null);
    }

    @Override
    public String putMedia(byte[] data, String type, String filename) {
        return putMedia(new Request.Builder().post(RequestBody.create(MediaType.parse(type), data)), filename);
    }

    @Override
    public String putMedia(File data, String type) {
        return putMedia(data, type, null);
    }

    @Override
    public String putMedia(File data, String type, String filename) {
        return putMedia(new Request.Builder().post(RequestBody.create(MediaType.parse(type), data)), filename);
    }

    @Override
    public List<JsonObject> getPushers() {
        URL url = getClientPath("pushers");
        JsonObject response = GsonUtil.parseObj(executeAuthenticated(new Request.Builder().get().url(url)));
        return GsonUtil.findArray(response, "pushers").map(array -> GsonUtil.asList(array, JsonObject.class))
                .orElse(Collections.emptyList());
    }

    @Override
    public void setPusher(JsonObject pusher) {
        URL url = getClientPath("pushers", "set");
        executeAuthenticated(new Request.Builder().url(url).post(getJsonBody(pusher)));
    }

    @Override
    public void deletePusher(String pushKey) {
        JsonObject pusher = new JsonObject();
        pusher.add("kind", JsonNull.INSTANCE);
        pusher.addProperty("pushkey", pushKey);
        setPusher(pusher);
    }

    @Override
    public io.dpm.matrix.client.api.GlobalPushRulesSet getPushRules() {
        URL url = getClientPath("pushrules", "global", "");
        JsonObject response = GsonUtil.parseObj(executeAuthenticated(new Request.Builder().url(url).get()));
        return new GlobalPushRulesSet(response);
    }

    @Override
    public PushRule getPushRule(String scope, String kind, String id) {
        return new MatrixHttpPushRule(context, scope, kind, id);
    }

}
