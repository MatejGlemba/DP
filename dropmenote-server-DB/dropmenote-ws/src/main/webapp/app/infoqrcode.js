app.controller("InfoqrcodeController", function ($rootScope, $scope, $timeout, $http, $cookies, dpnService, dpnToast, $mdDialog, dpnDialog) {

    var coverDiv = document.getElementById('notLoggedDivCover');
    coverDiv.style.display = "block"; 

    // init qrcode object
    $scope.qrcode = {
        id: 0,
        type: 'CHAT',
        ownerAlias: '',
        name: '',
        description: '',
        photo: '',
        icon: 'SPORT',
        active: false,
        pushNotification: false,
        emailNotification: false,
        uuid: '',
        userType: 'GUEST',
        blocked: false
    }
    $scope.isScreenVisible = false;
    $scope.token = dpnService.getCookieTokenObject().token;
    $scope.isChatButtonVisible = false;
    $scope.room = "";

    //--------------
    // scan qrcode
    //--------------
    $scope.processQrCode = function() {
        // ak je user prihlaseny 
        if ($scope.token) { 

            if ($scope.qrcode && $scope.qrcode.uuid && $scope.qrcode.id == 0 && $scope.qrcode.userType == 'ADMIN') {
                // check if it is a new qrcode (qrcode.uuid != prazdne)
                window.open("#/saveqrcode?newqrcode_token=" + $scope.qrcode.uuid, "_self");
            } else {
                // check if qrcode exists (qrcode.id != 0)
                if ($scope.qrcode.userType == 'ADMIN') { 
                    window.open("#/saveqrcode?qrcode_id=" + $scope.qrcode.id, "_self");
                } else {
                    // pre shared a guest zobrazi screenu
                    $scope.isScreenVisible = true;
                }
                
            }
            // pre plebs zobrazi dialog a tuto screenu
        } else {
            $scope.isScreenVisible = true;
            $timeout(function () {
                dpnDialog.showInstallApp($scope);
            }, 2000);
        }
    }

    var qrcode_id = getUrlParam("q");
    if (qrcode_id != null && qrcode_id != "undefined") {
        var call_qrcode_scanCallBackSuccess = function(data) {
            $scope.qrcode = data;
            $scope.isChatButtonVisible = $scope.qrcode.type == 'CHAT';

            $scope.processQrCode();
            // dpnService.revalidateLogoutCover(); MJU: WTF preco to tu je?
            
            // Hide Loading div
            coverDiv.style.display = "none"; 
            
        }
        var call_qrcode_scanCallBackError = function(data) {
            dpnDialog.showQrNotFound($scope);

            // dpnService.processErrorResponse(data);
        }
        dpnService.call_qrcode_scan(qrcode_id, ($scope.token ? $scope.token : fingerprint_uuid), call_qrcode_scanCallBackSuccess, call_qrcode_scanCallBackError)
    }

    // ------------------
    // -- Click events --
    // ------------------

    $scope.click_open_chat = function() {
        if ($scope.qrcode.blocked) {
            dpnToast.showToast("ERROR", "User blocked", "You are in blacklist");
        } else {
            window.open("#/chat?qr=" + $scope.qrcode.uuid + "&roomId=" + $scope.room, "_self");
        }
    }
});