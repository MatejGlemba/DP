app.controller("ChatController", function ($rootScope, $anchorScroll, $scope, $http, $timeout, $cookies, $window, dpnService, dpnDialog, dpnToast, $route) {

    // to enable input after services loads
    $scope.enableInput = function() {
        document.getElementById('chatInputTextarea').removeAttribute('disabled');
    }

    $scope.msgList = [];
    $scope.myMessage = "";
    $scope.guestMsg = undefined;
    $scope.blacklistObj = undefined;
    $scope.token = dpnService.getCookieTokenObject().token;
    $scope.nameList = [ "alligator", "bear", "beaver", "butterfly", "camel", "cat", "crab", "deer", "dolphin", 
                       "elephant", "frog", "giraffe", "gorilla", "horse", "jaguar", "kangaroo", "leopard", "llama", "mouse",
                       "panda", "rabbit", "snail", "spider", "turtle", "whale", "wolf"
                      ];
    
    // Remove cover div
    var coverDiv = document.getElementById('notLoggedDivCover');
    coverDiv.style.display = "none";    

    $scope.qrCodeName = "";

    // Load qrCodeInfo we need to know name
    var qrcode_id = getUrlParam("qr");
    if (qrcode_id && qrcode_id != "undefined") {
        var call_qrcode_loadNameCallBackSuccess= function(data) {
            // console.info("load name: " + JSON.stringify(data));
            $scope.qrCodeName = data;
        }
        var call_qrcode_loadNameCallBackError = function(data) {
            dpnService.processErrorResponse(data);
        }
        dpnService.call_qrcode_loadName(qrcode_id, call_qrcode_loadNameCallBackSuccess, call_qrcode_loadNameCallBackError);
    }

    //------------
    // WEBSOCKET
    //------------
    $scope.openWebSocket = function() {
        $scope.ws = new WebSocket(configuration_wsUrl + "/chat");
        // {“type”:“LOGIN”, “token”:“user token”, “qr”:“asi id QR kodu???”, "roomId":"id z DB matrix roomy"}

        /**
         * on websocket opened
         */
        $scope.ws.onopen = function() {
            console.info("Chat established.");
            //TODO token, qrcode id z inbox screeny
            var qrParam = getUrlParam("qr");
            var roomId = getUrlParam("roomId");
            if (!roomId || roomId == "undefined") {
                roomId = "";
            }
            var tokenUnique = dpnService.getCookieTokenObject().token;
            if (tokenUnique == undefined || tokenUnique == null || tokenUnique == "") {
                tokenUnique = fingerprint_uuid;
                $scope.ws.send(JSON.stringify({"type":"LOGIN", "fingerprint": tokenUnique, "qr":qrParam }));
            } else {
                $scope.ws.send(JSON.stringify({"type":"LOGIN", "token": tokenUnique, "qr":qrParam, "roomId": roomId}));
            }
            console.info("chat token: "+ tokenUnique + " \n qr: " + qrParam + " \n roomId: " + roomId);
            $scope.enableInput();
        };

        /**
         * on received message
         */
        $scope.ws.onmessage = function (evt) { 
            // timeout '0' to digest data, otherwise ng-repeat won't update msgList; stackoverflow "ng-repeat-not-updated-on-array-change-in-angular"
            $timeout(function() {
                var received_msg = JSON.parse(evt.data);
                if (received_msg) {
                    $scope.msgList.push(received_msg);
                    $scope.qrName = received_msg.qrName;
                    
                    // this block enables blacklist button
                    if (received_msg.userType == "GUEST") {
                        if (received_msg.position == "RIGHT") {
                            $scope.blacklistObj = {
                                'uuid': received_msg.from, 
                                'alias': received_msg.alias, 
                                'note': ""
                            };
                        }
                        $scope.guestMsg = received_msg;
                    }
                    
                    // to scroll to last message without closing virtual keyboard on device
                    // 
                    // scrollableDiv.scrollIntoView(false);
                    // scrollableDiv.scrollTop = scrollableDiv.scrollHeight;

                    if (received_msg.type == "ERROR") {
                        dpnToast.showToast("ERROR", "Communication error", "Problem with connection to server");
                    } else if (received_msg.type == "MESSAGE_AI") {
                        dpnToast.showToast("ERROR", "Problem with received message", received_msg.data)
                    }
                } 
            });
        };

        /**
         * on websocket closed
         */
        $scope.ws.onclose = function() { 
            console.info("Chat connection is closed.");
        };
    }

    // ----------------
    // load data
    // ---------------
    $scope.openWebSocket();

    // ------------
    // Click events
    // ------------

    // send msg as json
    $scope.sendMsg = function(myMessage) {

        var tokenUnique = dpnService.getCookieTokenObject().token;
        tokenUnique = tokenUnique ? tokenUnique : fingerprint_uuid;

        $scope.ws.send(JSON.stringify({ "type": "TEXT_MESSAGE", "message": myMessage, "token": tokenUnique }));
        myMessage = "";
        $scope.myMessage = "";
    }

    $scope.$on('$routeChangeStart', function($event, next, current) { 
        $scope.ws.close();
    });

    $scope.click_back = function() {
        if (!isDialogDisplayed()) {
            if (!$scope.token) {
                var qrParam = getUrlParam("qr");
                window.open("#/infoqrcode?q=" + qrParam, "_self");
            } else {
                if ($rootScope.previousPage) {
                    $window.history.back();
                } else {
                    window.open("#/inbox?qrcode_id=&qrcode_name=", "_self");
                }
            }
        }
    }
    
    $scope.click_openQRCodeInfo = function() {
        // TODO spravnu url tu na spravnu url
        // TODO spravnu url tu na spravnu url
        // TODO spravnu url tu na spravnu url
        
    }

    $scope.init = function(){
        //DO NOTHING - to je len preto aby sa dal volat spolocny dialog pre editBlacklist
    }

    $scope.click_addToBlacklist = function() {
        // filter all msgs to get GUEST type user - one that started the Chat
        $scope.msgList.some(msg => {
            if (msg.userType == "GUEST") {
                if (msg.position == "RIGHT") {
                    $scope.blacklistObj = {
                        'uuid': msg.from, 
                        'alias': msg.alias, 
                        'note': ""
                    };
                }
                
                $scope.guestMsg = msg;
                return;
            }
        });
        if ($scope.blacklistObj) {
            dpnDialog.showEditBlacklist($scope, $scope.blacklistObj, false);
        } else {
            if ($scope.guestMsg) {
                dpnToast.showToast("ERROR", "Can't block", "You can't block admin");
            } else {
                // toto nema nikdy nastať
                dpnToast.showToast("ERROR", "Missing guest user", "No valid user to block");
            }
        }
    }

    /**
     * get image href based on anonymous user alias
     */
    $scope.get_image_href = function(msg) {
        if (msg.image) { // server vracia null ak je usertype GUEST a nema uuid (je 'plebs'). Inak vracia Icon enum
            if (msg.image.startsWith('P')) {
                return $scope.getImgNameForChatIcon(msg.image);
            } else {
                return msg.image;
            }
        } else {
            if (msg.alias != undefined) {
                var nameSubstring =  msg.alias.substring(0, msg.alias.length - 3); // remove last 3 random number digits
                if ($scope.nameList.includes(nameSubstring)) {
                    return "app/assets/img/animal_icons/" + nameSubstring + ".png";
                } else {
                    // TODO wtf co to je???
                    return "";
                }
            } else {
                // TODO wtf co to je???
                return "";
            }
        }
    }   

    // $scope.getImgNameForChatIcon = function(chatIcon) {
    //     var imgPathUser = "app/assets/img/profile_icons/";
    //     var imgName = "P1.svg";
    //     if (!chatIcon) {
    //         return imgPathUser + imgName;
    //     }
    //     return imgPathUser + chatIcon + ".svg";
    // } 

    $scope.getImgNameForChatIcon = function(chatIcon) {
        if (/P\d+/.test(chatIcon)) {
            var imgPathUser = "app/assets/img/profile_icons/";
            return imgPathUser + chatIcon + ".svg";
        } else {
            var imgPathUser = "app/assets/img/save_qrcode/";
            var imgName = "qrchat_ball.svg";
            switch(chatIcon) {
                case 'SPORT': imgName = "qrchat_ball.svg"; break;
                case 'FASHION': imgName = "qrchat_figurine.svg"; break;
                case 'MEDICINE': imgName = "qrchat_heart.svg"; break;
                case 'TECHNICS': imgName = "qrchat_monitor.svg"; break;
                case 'ART': imgName = "qrchat_paint.svg"; break;
                case 'WORKOUT': imgName = "qrchat_bodybuilder.svg"; break;
                case 'FOOD': imgName = "qrchat_food.svg"; break;
                case 'ZOO': imgName = "qrchat_fish.svg"; break;
                default: imgName = "qrchat_ball.svg"; break;
            }
            return imgPathUser + imgName;
        }    
    }
});