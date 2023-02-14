app.service("dpnService", ['$http', '$rootScope', '$cookies', 'dpnDialog', 'dpnToast', '$timeout', function ($http, $rootScope, $cookies, dpnDialog, dpnToast, $timeout) {

    var _this = this;

    // counter strike
    var cycleCounter = 0;

    // Cookies configuration
    var cookiesPathValue = null;
    var cookiesExpirationSettings = { expires: new Date(2100, 1, 1, 1, 30, 0, 0), path: cookiesPathValue};

    //Configure path by browser
    // TODO toto treba spravit krajsie!!
    // $cookies.put("cookies_path_test", "Starbug");
    localStorage.setItem('cookies_path_test', "Starbug");
    // if($cookies.get("cookies_path_test") == "Starbug"){
    if (localStorage.getItem("cookies_path_test") == "Starbug") {
        // Default path is OK
    }else{
        cookiesPathValue = "/";
    }   


    // get cookie, if doesnt exist, return empty string
    this.getCookieTokenObject = function() {
        // var cookie = $cookies.get("dpn_token");
        var cookie = localStorage.getItem("dpn_token");
        var cookieReturn = null;

        if(cookie){
            try {
                cookieReturn =  JSON.parse(cookie);
            }catch(e){
                console.warn ("Reset token ");
                cookieReturn = JSON.parse('{"date": 0, "token":""}');
            }
        }else{
            cookieReturn = JSON.parse('{"date": 0, "token":""}');
        }

        // Fixed problem with wrong cookies
        try{
            var token = cookieReturn.token;
            if(cookieReturn == null || token == null){
                return JSON.parse('{"date": 0, "token":""}');
            }
        }catch(e){
            return JSON.parse('{"date": 0, "token":""}');
        }

        return cookieReturn;
    }

    this.clearCookies = function() {
        localStorage.removeItem("dpn_token");
        localStorage.removeItem("dpn_userAlias");
        localStorage.removeItem("dpn_userLogin");
        localStorage.removeItem("dpn_userIcon");
        
        this.revalidateLogoutCover();
    }

    this.revalidateLogoutCover = function(){
        var coverDiv = document.getElementById('notLoggedDivCover');
        
        if(localStorage.getItem("dpn_userLogin") == null){
            coverDiv.style.display = "block";     
        }else{
            coverDiv.style.display = "none";     
        }
    }

    this.showLoadingDiv = function(show){
        var loadingDiv = document.getElementById('loadingDiv');

        if(show){
            loadingDiv.style.visibility = "visible";     
        }else{
            loadingDiv.style.visibility = "hidden"; 
        }
    }

    this.validateAndPutInToCookie = function(newToken){
        var lastTokenTime = 0;
        var newTokenTime = 0;
        // var oldToken = $cookies.get("dpn_token");
        var oldToken = localStorage.getItem("dpn_token");
        if (oldToken != null && oldToken.includes("#STARBUG#")) {
            var oldTokenParts = oldToken.split("#STARBUG#");
            var lastTokenTimeParts = oldTokenParts[1];
            lastTokenTime = Number((lastTokenTimeParts.split("\""))[0]);
        }
        if (newToken != null && newToken.includes("#STARBUG#")) {
            var newTokenParts = newToken.split("#STARBUG#");
            newTokenTime = Number(newTokenParts[1]);
        }
        if (lastTokenTime != 0 && newTokenTime != 0) {
            if (newTokenTime >= lastTokenTime){
                // MUU !!!! chlaani toto nemoze nikdy fungovat, ten ca sa ma pouzivat zo servera. 
                // pre to sme prerabali backend aby sme cas pouzili serverovy a nechapem preco stale pouzivame new date time v metoda repareToken. 
                // potom je uplne jasne ze 3 dni sa tu serieme s tokenom ked sa to nezimplementuje tak ako bolo dohodnute!!!!!
                // $cookies.put("dpn_token", prepareTokenObject(newToken, newTokenTime), cookiesExpirationSettings);
                localStorage.setItem('dpn_token', prepareTokenObject(newToken, newTokenTime));
            } else {
            }

        } else {
            // $cookies.put("dpn_token", prepareTokenObject(newToken, newTokenTime), cookiesExpirationSettings);
            localStorage.setItem('dpn_token', prepareTokenObject(newToken, newTokenTime));
        }
    }

    // LOGIN USER ---------------------------
    this.call_user_login = function (userLoginRequest, call_user_loginCallBackSuccess, call_user_loginCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);
        $http({
            url: configuration_baseUrl + "/ws/user/login",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(userLoginRequest),
            params: {},
            headers: {}
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            //$cookies.put("dpn_token", prepareTokenObject(headers("token")), cookiesExpirationSettings);
            _this.validateAndPutInToCookie(headers("token"));

            // broadcast email+alias+icon value for sidebar controller
            $rootScope.$broadcast('login.email.value', data.userLogin);
            $rootScope.$broadcast('login.alias.value', data.userAlias);
            $rootScope.$broadcast('login.icon.value', data.userIcon);
            // $cookies.put("dpn_userLogin", data.userLogin, cookiesExpirationSettings);
            // $cookies.put("dpn_userAlias", data.userAlias, cookiesExpirationSettings);
            localStorage.setItem("dpn_userIcon", data.userIcon);
            localStorage.setItem("dpn_userLogin", data.userLogin);
            localStorage.setItem("dpn_userAlias", data.userAlias);
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_user_loginCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();

            call_user_loginCallBackError(data, status, headers, config);
        });
    };
    // LOGIN USER end ---------------------------

    // LOGOUT USER ---------------------------
    this.call_user_logout = function (parDeviceId, call_user_logoutCallBackSuccess, call_user_logoutCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        $http({
            url: configuration_baseUrl + "/ws/user/logout",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                deviceId: parDeviceId
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            _this.clearCookies();
            _this.revalidateLogoutCover();

            closeWebSocketNotifications();
            call_user_logoutCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();
            _this.clearCookies();

            closeWebSocketNotifications();
            call_user_logoutCallBackError(data, status, headers, config);
        });
    };
    // LOGOUT USER end ---------------------------

    // FORGOTPASSWORD USER  ---------------------------
    this.call_user_forgotPassword = function (login, call_user_forgotPasswordCallBackSuccess, call_user_forgotPasswordCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_login = login;
        var param_call_user_forgotPasswordCallBackSuccess = call_user_forgotPasswordCallBackSuccess;
        var param_call_user_forgotPasswordCallBackError = call_user_forgotPasswordCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/user/forgotPassword",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                login: login
            },
            headers: {}
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            //$cookies.put("dpn_token", prepareTokenObject(headers("token")), cookiesExpirationSettings);
            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            call_user_forgotPasswordCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();

            call_user_forgotPasswordCallBackError(data, status, headers, config);    
        });
    };
    // FORGOTPASSWORD USER end ---------------------------

    // LOAD USER ---------------------------
    this.call_user_load = function (call_user_loadCallBackSuccess, call_user_loadCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_call_user_loadCallBackSuccess = call_user_loadCallBackSuccess;
        var param_call_user_loadCallBackError = call_user_loadCallBackError;


        $http({
            url: configuration_baseUrl + "/ws/user/load",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            //            , withCredentials: true
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
                // 'content-type': "application/json"
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            //$cookies.put("dpn_token", prepareTokenObject(headers("token")), cookiesExpirationSettings);
            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_user_loadCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){ 
                if (cycleCounter <= 5){
                    _this.call_user_load(param_call_user_loadCallBackSuccess, param_call_user_loadCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_user_loadCallBackError(data, status, headers, config);
        });
    };
    // LOAD USER end ---------------------------

    // REGISTER USER ---------------------------
    this.call_user_register = function (request, call_user_registerCallBackSuccess, call_user_registerCallBackError) {
        this.showLoadingDiv(true);

        var param_request = request;
        var param_call_user_registerCallBackSuccess = call_user_registerCallBackSuccess;
        var param_call_user_registerCallBackError = call_user_registerCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/user/register",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(request),
            params: {},
            headers: {}
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            //$cookies.put("dpn_token", prepareTokenObject(headers("token")), cookiesExpirationSettings);
            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            call_user_registerCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();

            call_user_registerCallBackError(data, status, headers, config);
        });
    };
    // REGISTER USER end ---------------------------

    // UPDATE USER ---------------------------
    this.call_user_update = function (request, call_user_updateCallBackSuccess, call_user_updateCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_request = request;
        var param_call_user_updateCallBackSuccess = call_user_updateCallBackSuccess;
        var param_call_user_updateCallBackError = call_user_updateCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/user/update",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(request),
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            //$cookies.put("dpn_token", prepareTokenObject(headers("token")), cookiesExpirationSettings);
            _this.validateAndPutInToCookie(headers("token"));

            $rootScope.$broadcast('login.email.value', data.userLogin);
            $rootScope.$broadcast('login.alias.value', data.userAlias);
            $rootScope.$broadcast('login.icon.value', data.userIcon);
            // $cookies.put("dpn_userLogin", data.userLogin, cookiesExpirationSettings);
            // $cookies.put("dpn_userAlias", data.userAlias, cookiesExpirationSettings);
            localStorage.setItem("dpn_userIcon", data.userIcon);
            localStorage.setItem("dpn_userLogin", data.userLogin);
            localStorage.setItem("dpn_userAlias", data.userAlias);
           
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_user_updateCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){

                if (cycleCounter <= 5){
                    _this.call_user_update(param_request, param_call_user_updateCallBackSuccess, param_call_user_updateCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }               

            }

            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_user_updateCallBackError(data, status, headers, config);
        });
    };
    // UPDATE USER end ---------------------------

    // UPDATE PSWD RECOVERY USER ---------------------------
    this.call_user_updatePswdRecovery = function (recoveryPswdToken, password, call_user_updatePswdRecoveryCallBackSuccess, call_user_updatePswdRecoveryCallBackError) {
        this.showLoadingDiv(true);

        $http({
            url: configuration_baseUrl + "/ws/user/updatePswdRecovery",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                password: password
            },
            headers: {
                "recovery-pswd-token": recoveryPswdToken
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();

            call_user_updatePswdRecoveryCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();

            call_user_updatePswdRecoveryCallBackError(data, status, headers, config);
        });
    };
    // UPDATE PSWD RECOVERY USER end --------------------------

    // UPDATE PASSWORD USER ---------------------------
    this.call_user_updatePassword = function (password, call_user_updatePassword_callBackSuccess, call_user_updatePassword_callBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_password = password;
        var param_call_user_updatePassword_callBackSuccess = call_user_updatePassword_callBackSuccess;
        var param_call_user_updatePassword_callBackError = call_user_updatePassword_callBackError;

        $http({
            url: configuration_baseUrl + "/ws/user/updatePassword",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                password: password
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_user_updatePassword_callBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_user_updatePassword(param_password, param_call_user_updatePassword_callBackSuccess, param_call_user_updatePassword_callBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_user_updatePassword_callBackError(data, status, headers, config);
        });
    };
    // UPDATE PASSWORD USER end --------------------------


    // VALIDATE RESET PASSWORD TOKEN USER ---------------------------
    this.call_user_validateResetPasswordToken = function (pswToken, call_user_validateResetPasswordTokenCallBackSuccess, call_user_validateResetPasswordTokenCallBackError) {
        this.showLoadingDiv(true);

        var param_pswToken = pswToken;
        var param_call_user_validateResetPasswordTokenCallBackSuccess = call_user_validateResetPasswordTokenCallBackSuccess;
        var param_call_user_validateResetPasswordTokenCallBackError = call_user_validateResetPasswordTokenCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/user/validateResetPasswordToken",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                token: pswToken
            },
            headers: {}
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();

            call_user_validateResetPasswordTokenCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);
            _this.revalidateLogoutCover();

            call_user_validateResetPasswordTokenCallBackError(data, status, headers, config);
        });
    };

    // SEND PUSH PUSH ---------------------------
    // this.call_push_sendPush = function (message, deviceIds, call_push_sendPushCallBackSuccess, call_push_sendPushCallBackError) {
    //     $http({
    //         url: configuration_baseUrl + "/ws/push/sendPush",
    //         dataType: "json",
    //         method: "POST",
    //         contentType: "application/json",
    //         data: {},
    //         params: {
    //             deviceIds: deviceIds,
    //             message: message
    //         },
    //         headers: {
    //             token: this.getCookieTokenObject().token
    //         }
    //     }).success(function (data, status, headers, config) {
    //         console.info("call_push_sendPush:" + JSON.stringify(data));
    //         $cookies.put("dpn_token", prepareTokenObject(headers("token")));
    //         call_push_sendPushCallBackSuccess(data, status, headers, config);
    //     }).error(function (data, status, headers, config) {
    //         console.error("Nastal problém komunikácie so serverom");
    //         call_push_sendPushCallBackError(data, status, headers, config);
    //     });
    // };
    // SEND PUSH PUSH end ---------------------------

    // ADD SHARE QRCODE ---------------------------
    this.call_qrcode_addShare = function (qrCodeId, shareUserLogin, call_qrcode_addShareCallBackSuccess, call_qrcode_addShareCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_qrCodeId = qrCodeId;
        var param_shareUserLogin = shareUserLogin;
        var param_call_qrcode_addShareCallBackSuccess = call_qrcode_addShareCallBackSuccess;
        var param_call_qrcode_addShareCallBackError = call_qrcode_addShareCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/addShare",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                qrCodeId: qrCodeId,
                shareUserLogin: shareUserLogin
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_addShareCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_qrcode_addShare (param_qrCodeId, param_shareUserLogin, param_call_qrcode_addShareCallBackSuccess, param_call_qrcode_addShareCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }               


            }

            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_addShareCallBackError(data, status, headers, config);
        });
    };
    // ADD SHARE QRCODE end ---------------------------

    // GENERATE QRCODE ---------------------------
    this.call_qrcode_generate = function (qrCodeId, call_qrcode_generateCallBackSuccess, call_qrcode_generateCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_qrCodeId = qrCodeId;
        var param_call_qrcode_generateCallBackSuccess = call_qrcode_generateCallBackSuccess;
        var param_call_qrcode_generateCallBackError = call_qrcode_generateCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/generate",
            //TODO setup response, hint: server vracia byte[]
            // dataType: "string",
            method: "POST",
            responseType: "blob",
            contentType: "text/plain",
            data: {},
            params: {
                qrCodeId: qrCodeId,
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_generateCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_qrcode_generate (param_qrCodeId, param_call_qrcode_generateCallBackSuccess, param_call_qrcode_generateCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_generateCallBackError(data, status, headers, config);
        });
    };
    // GENERATE QRCODE end ---------------------------

    // LOAD QRCODE ---------------------------
    this.call_qrcode_load = function (qrCodeId, call_qrcode_loadCallBackSuccess, call_qrcode_loadCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_qrCodeId = qrCodeId;
        var param_call_qrcode_loadCallBackSuccess = call_qrcode_loadCallBackSuccess;
        var param_call_qrcode_loadCallBackError = call_qrcode_loadCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/load",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                qrCodeId: qrCodeId,
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_loadCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date  && _this.getCookieTokenObject().date != 0 ){  
                if (cycleCounter <= 5){
                    _this.call_qrcode_load  (param_qrCodeId, param_call_qrcode_loadCallBackSuccess, param_call_qrcode_loadCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_loadCallBackError(data, status, headers, config);
        });
    };
    // LOAD QRCODE end ---------------------------
    
    // LOAD QRCODE NAME ---------------------------
    this.call_qrcode_loadName = function (qrCodeId, call_qrcode_loadCallBackSuccess, call_qrcode_loadCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_qrCodeId = qrCodeId;
        var param_call_qrcode_loadCallBackSuccess = call_qrcode_loadCallBackSuccess;
        var param_call_qrcode_loadCallBackError = call_qrcode_loadCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/loadName",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                qrCodeId: qrCodeId,
            },
            headers: {}
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            // _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_loadCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);
            call_qrcode_loadCallBackError(data, status, headers, config);
        });
    };
    // LOAD QRCODE NAME end ---------------------------

    // LOAD ALL QRCODE ---------------------------
    this.call_qrcode_loadAll = function (call_qrcode_loadAllCallBackSuccess, call_qrcode_loadAllCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_call_qrcode_loadAllCallBackSuccess = call_qrcode_loadAllCallBackSuccess;
        var param_call_qrcode_loadAllCallBackError = call_qrcode_loadAllCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/loadAll",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            _this.validateAndPutInToCookie(headers("token"));
            // $cookies.put("dpn_userLogin", data.userInfo.userLogin, cookiesExpirationSettings);
            // $cookies.put("dpn_userAlias", data.userInfo.userAlias, cookiesExpirationSettings);
            $rootScope.$broadcast('login.email.value', data.userInfo.userLogin);
            $rootScope.$broadcast('login.alias.value', data.userInfo.userAlias);
            $rootScope.$broadcast('login.icon.value', data.userInfo.userIcon);
            // $cookies.put("dpn_userLogin", data.userInfo.userLogin, cookiesExpirationSettings);
            // $cookies.put("dpn_userAlias", data.userInfo.userAlias, cookiesExpirationSettings);
            localStorage.setItem("dpn_userIcon", data.userInfo.userIcon);
            localStorage.setItem("dpn_userLogin", data.userInfo.userLogin);
            localStorage.setItem("dpn_userAlias", data.userInfo.userAlias);
            
            _this.revalidateLogoutCover();
            
            openWebSocketNotifications();
            call_qrcode_loadAllCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);
            // Call it again if the token was updated by another service

            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){

                if (cycleCounter <= 5){
                    _this.call_qrcode_loadAll (param_call_qrcode_loadAllCallBackSuccess, param_call_qrcode_loadAllCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }               


            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_loadAllCallBackError(data, status, headers, config);
        });
    };
    // LOAD ALL QRCODE end ---------------------------

    // LOAD ALL QRCODE FOR INBOX ---------------------------
    this.call_qrcode_loadAllForInbox = function (call_qrcode_loadAllForInboxCallBackSuccess, call_qrcode_loadAllForInboxCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_call_qrcode_loadAllForInboxCallBackSuccess = call_qrcode_loadAllForInboxCallBackSuccess;
        var param_call_qrcode_loadAllForInboxCallBackError = call_qrcode_loadAllForInboxCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/loadAllForInbox",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_loadAllForInboxCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){   
                if (cycleCounter <= 5){
                    _this.call_qrcode_loadAllForInbox (param_call_qrcode_loadAllForInboxCallBackSuccess, param_call_qrcode_loadAllForInboxCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                
            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_loadAllForInboxCallBackError(data, status, headers, config);
        });
    };
    // LOAD ALL QRCODE FOR INBOX end ---------------------------

    // LOAD SHARES QRCODE ---------------------------
    this.call_qrcode_loadShares = function (qrCodeId, call_qrcode_loadSharesCallBackSuccess, call_qrcode_loadSharesCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_qrCodeId = qrCodeId;
        var param_call_qrcode_loadSharesCallBackSuccess = call_qrcode_loadSharesCallBackSuccess;
        var param_call_call_qrcode_loadSharesCallBackError = call_qrcode_loadSharesCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/loadShares",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                qrCodeId: qrCodeId
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_loadSharesCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_qrcode_loadShares (param_qrCodeId, param_call_qrcode_loadSharesCallBackSuccess, param_call_call_qrcode_loadSharesCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }               

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_loadSharesCallBackError(data, status, headers, config);
        });
    };
    // LOAD SHARES QRCODE end ---------------------------

    // REMOVE SHARE QRCODE ---------------------------
    this.call_qrcode_removeShare = function (qrCodeId, shareUserLogin, call_qrcode_removeShareCallBackSuccess, call_qrcode_removeShareCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_qrCodeId = qrCodeId;
        var param_shareUserLogin = shareUserLogin;
        var param_call_qrcode_removeShareCallBackSuccess = call_qrcode_removeShareCallBackSuccess;
        var param_call_qrcode_removeShareCallBackError = call_qrcode_removeShareCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/removeShare",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                shareUserLogin: shareUserLogin,
                qrCodeId: qrCodeId
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_removeShareCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_qrcode_removeShare (param_qrCodeId, param_shareUserLogin, param_call_qrcode_removeShareCallBackSuccess, param_call_qrcode_removeShareCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                
            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_removeShareCallBackError(data, status, headers, config);
        });
    };
    // REMOVE SHARE QRCODE end ---------------------------

    // SAVE QRCODE ---------------------------
    this.call_qrcode_save = function (request, call_qrcode_saveCallBackSuccess, call_qrcode_saveCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_request = request;
        var param_call_qrcode_saveCallBackSuccess = call_qrcode_saveCallBackSuccess;
        var param_call_qrcode_saveCallBackError = call_qrcode_saveCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/save",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(request),
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_saveCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_qrcode_save (param_request, param_call_qrcode_saveCallBackSuccess, param_call_qrcode_saveCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_qrcode_saveCallBackError(data, status, headers, config);
        });
    };
    // SAVE QRCODE end ---------------------------

    // SCAN QRCODE ---------------------------
    this.call_qrcode_scan = function (qrCodeToken, userToken, call_qrcode_scanCallBackSuccess, call_qrcode_scanCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_qrCodeToken = qrCodeToken;
        var param_userToken = userToken;
        var param_call_qrcode_scanCallBackSuccess = call_qrcode_scanCallBackSuccess;
        var param_call_qrcode_scanCallBackError = call_qrcode_scanCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/qrcode/scan",
            dataType: "json",
            method: "POST",
            contentType: "application/json;charset=UTF-8",
            data: {},
            params: {
                qrCodeToken: qrCodeToken
            },
            headers: {
                token: userToken
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            // _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_qrcode_scanCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_qrcode_scan (param_qrCodeToken, param_userToken, param_call_qrcode_scanCallBackSuccess, param_call_qrcode_scanCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }

            // _this.revalidateLogoutCover();
            
            call_qrcode_scanCallBackError(data, status, headers, config);
        });
    };
    // SCAN QRCODE end ---------------------------

    // ---------------------------
    // QR CODE CONTROLLER end-----
    // ---------------------------
    // ---------------------------
    // ---------------------------
    // ---------------------------
    // ---------------------------
    // MATRIX CONTROLLER----------
    // ---------------------------
    // ---------------------------
    // ---------------------------
    
    // LOAD ADMIN MATRIX ROOM MATRIX ---------------------------
    this.call_matrix_loadAdminRooms = function (call_matrix_loadAdminRooms_callBackSuccess, call_matrix_loadAdminRooms_callBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_call_matrix_loadAdminRooms_callBackSuccess = call_matrix_loadAdminRooms_callBackSuccess;
        var param_call_matrix_loadAdminRooms_callBackError = call_matrix_loadAdminRooms_callBackError;

        $http({
            url: configuration_baseUrl + "/ws/chat/loadAdminRooms",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            
            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_matrix_loadAdminRooms_callBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_matrix_loadAdminRooms (param_call_matrix_loadAdminRooms_callBackSuccess, param_call_matrix_loadAdminRooms_callBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                


            }

            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_matrix_loadAdminRooms_callBackError(data, status, headers, config);
        });
    };

    // LOAD ADMIN MATRIX ROOM MATRIX end ---------------------------

    
    // ---------------------------
    // MATRIX CONTROLLER end------
    // ---------------------------
    // ---------------------------
    // ---------------------------
    // ---------------------------
    // ---------------------------
    // BLACKLIST CONTROLLER-------
    // ---------------------------
    // ---------------------------
    // ---------------------------
    // ADD TO BLACKLIST ---------------------------
    // {"uuid":"1string","note":"1string","alias":"1string","id":2}
    this.call_blacklist_addToBlacklist = function (blackListRequest, call_blacklist_addToBlacklistCallBackSuccess, call_blacklist_addToBlacklistCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_blackListRequest = blackListRequest;
        var param_call_blacklist_addToBlacklistCallBackSuccess = call_blacklist_addToBlacklistCallBackSuccess;
        var param_call_blacklist_addToBlacklistCallBackError = call_blacklist_addToBlacklistCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/blacklist/addToBlacklist",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(blackListRequest),
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_blacklist_addToBlacklistCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_blacklist_addToBlacklist(param_blackListRequest, param_call_blacklist_addToBlacklistCallBackSuccess, param_call_blacklist_addToBlacklistCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_blacklist_addToBlacklistCallBackError(data, status, headers, config);
        });
    };
    // ADD TO BLACKLIST end ---------------------------

    // LOAD BLACKLIST ---------------------------
    this.call_blacklist_loadBlackList = function (call_blacklist_loadBlackListCallBackSuccess, call_blacklist_loadBlackListCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_call_blacklist_loadBlackListCallBackSuccess = call_blacklist_loadBlackListCallBackSuccess;
        var param_call_blacklist_loadBlackListCallBackError = call_blacklist_loadBlackListCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/blacklist/loadBlacklist",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {},
            headers: {
                token: cookiesSessionTokenObject.token
            }
        })
            .success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);

            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_blacklist_loadBlackListCallBackSuccess(data, status, headers, config);
        })
            .error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_blacklist_loadBlackList (param_call_blacklist_loadBlackListCallBackSuccess, param_call_blacklist_loadBlackListCallBackError)
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                

            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_blacklist_loadBlackListCallBackError(data, status, headers, config);
        });
    };
    // LOAD BLACKLIST end ---------------------------

    // REMOVE FROM BLACKLIST ---------------------------
    this.call_blacklist_removeFromBlacklist = function (blacklisedUDID, call_blacklist_removeFromBlacklistCallBackSuccess, call_blacklist_removeFromBlacklistCallBackError) {
        var cookiesSessionTokenObject = this.getCookieTokenObject();
        this.showLoadingDiv(true);

        var param_blacklisedUDID = blacklisedUDID;
        var param_call_blacklist_removeFromBlacklistCallBackSuccess = call_blacklist_removeFromBlacklistCallBackSuccess;
        var param_call_blacklist_removeFromBlacklistCallBackError = call_blacklist_removeFromBlacklistCallBackError;

        $http({
            url: configuration_baseUrl + "/ws/blacklist/removeFromBlacklist",
            dataType: "json",
            method: "POST",
            contentType: "application/json",
            data: {},
            params: {
                blacklisedUDID: blacklisedUDID
            },
            headers: {
                token: cookiesSessionTokenObject.token
            }
        }).success(function (data, status, headers, config) {
            _this.showLoadingDiv(false);
            
            _this.validateAndPutInToCookie(headers("token"));
            _this.revalidateLogoutCover();

            openWebSocketNotifications();
            call_blacklist_removeFromBlacklistCallBackSuccess(data, status, headers, config);
        }).error(function (data, status, headers, config) {
            console.error("There was a problem communicating with the server");
            _this.showLoadingDiv(false);

            // Call it again if the token was updated by another service
            if(data.errorCode == 4006 && _this.getCookieTokenObject().date >= cookiesSessionTokenObject.date && _this.getCookieTokenObject().date != 0 ){
                if (cycleCounter <= 5){
                    _this.call_blacklist_removeFromBlacklist (param_blacklisedUDID, param_call_blacklist_removeFromBlacklistCallBackSuccess, param_call_blacklist_removeFromBlacklistCallBackError);
                    if (_this.getCookieTokenObject().date == cookiesSessionTokenObject.date) {
                        cycleCounter++;
                    }
                    return;
                } else {
                    cycleCounter = 0;
                    _this.clearCookies();
                }                
            }
            // Not valid session token
            if(data.errorCode == 4006){
                _this.clearCookies();
            }
            
            _this.revalidateLogoutCover();

            call_blacklist_removeFromBlacklistCallBackError(data, status, headers, config);
        });
    };
    // REMOVE FROM BLACKLIST end ---------------------------

    // PROCESS ERROR EXCEPTION RESPONSE ---------------------
    this.processErrorResponse = function(data) {
        if (data.errorCode == 4001) {
            dpnToast.showToast("ERROR", data.message, data.description);
        }        
        if (data.errorCode == 4002) {
            dpnToast.showToast("ERROR", data.message, data.description);
        }    
        if (data.errorCode == 4003) {
            dpnToast.showToast("ERROR", data.message, data.description);
        }    
        if (data.errorCode == 4004) {
            dpnToast.showToast("ERROR", data.message, data.description);
        }    
        if (data.errorCode == 4005) {
            dpnToast.showToast("ERROR", data.message, data.description);
            closeWebSocketNotifications();
            dpnDialog.showLogin();  
        }    
        if (data.errorCode == 4006) {
            // Tento toast je zbytocny, lebo ideme tak si tak do login obrazovky
            //dpnToast.showToast("ERROR", data.message, data.description);
            closeWebSocketNotifications();
            dpnDialog.showLogin();    
        }
        if (data.errorCode == 4007) {
            dpnToast.showToast("ERROR", data.message, data.description);
        }    
        if (data.errorCode == 4008) {
            dpnToast.showToast("ERROR", data.message, data.description);
        }   
        if (data.errorCode == 6664002) {
            dpnToast.showToast("ERROR", data.message, data.description);
        }
    }
    // PROCESS ERROR EXCEPTION RESPONSE end ----------------- 

    // WEBSOCKET for NOTIFICATIONS --------------------------
    var websocketNotifications; //websocket object

    var openWebSocketNotifications = function() {
        if (!websocketNotifications) {
            websocketNotifications = new WebSocket(configuration_wsUrl + "/notification");
        }
        // {“type”:“LOGIN”, “token”:“user token”, “qr”:“asi id QR kodu???”, "roomId":"id z DB matrix roomy"}

        /**
         * on websocket opened
         */
        websocketNotifications.onopen = function() {
            console.info("notification token " + _this.getCookieTokenObject().token);
            websocketNotifications.send(JSON.stringify({"type":"LOGIN", "token": _this.getCookieTokenObject().token}));
            console.info("Notifications established.");
        };

        /**
         * on received message
         */
        websocketNotifications.onmessage = function (evt) { 
            // timeout '0' to digest data, otherwise ng-repeat won't update msgList; stackoverflow "ng-repeat-not-updated-on-array-change-in-angular"
            console.info("notification msg: " + evt.data);
            $timeout(function() {
                console.info("notification msg: " + evt.data);
                var received_msg = JSON.parse(evt.data);
                if (received_msg != undefined || received_msg != null || received_msg != "") {
                    if (!window.location.href.includes("#/chat?qr=" + received_msg.qr + "&roomId=" + received_msg.roomId)) {
                        if (received_msg.from && received_msg.msg) {
                            dpnToast.showToast("MESSAGE", received_msg.from, received_msg.msg, received_msg);
                        }
                    }
                } 
            });
        };

        /**
         * on websocket closed
         */
        websocketNotifications.onclose = function() { 
            websocketNotifications = undefined;
            console.info("Notifications closed.");
        };
           
    }

    var closeWebSocketNotifications = function() {
        if (websocketNotifications) {
            websocketNotifications.close();
        }
    }
    
    // Call after init, onLoad call
    this.revalidateLogoutCover();
    
    // WEBSOCKET for NOTIFICATIONS end --------------------------

    }]);

