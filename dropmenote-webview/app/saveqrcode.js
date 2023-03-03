app.controller("SaveqrcodeController", function ($rootScope, $scope, $timeout, $http, $cookies, $location, dpnService, dpnToast, $mdDialog, dpnDialog) {

    // init qrcode object
    $scope.qrcode_chatSupport = true;
    $scope.qrcode = {
        id: 0,
        type: 'CHAT',
        ownerAlias: '',
        name: '',
        description: '',
        photo: '',
        icon: 'SPORT',
        active: true,
        pushNotification: false,
        emailNotification: false,
        uuid: '',
        unreadMsgs: 0,
        roomsCount: 0,
        created: 0
    }

    $scope.isProcessingSubmit = false;

    $scope.photoUploadElement = document.getElementById('qrPhotoUploadButtonForApp');
    
    var isLoaded= false;

    $scope.serviceFinished = false;

    // ------------------------------------------------
    // Observer to react on base64 upload from CN1 app
    // ------------------------------------------------
    var inputElement = document.getElementById('qrPhotoUploadButtonForApp');
    var observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'attributes' && mutation.attributeName === 'base64') {
                $timeout(function() {
                    $scope.qrcode.photo = inputElement.getAttribute('base64');
                });
            }
        });
    });
    observer.observe(inputElement, {
        attributes: true
    });

    //---------------
    // load qrcode
    //--------------
    // predvyplni pushnotification, emailnotification, active stavy podla nastavenia z UserBeany
    var userLoadWrapper = function () { 
        var call_user_load_callBackSuccess = function (data) {
            $scope.qrcode.pushNotification = data.pushNotification;
            $scope.qrcode.emailNotification = data.emailNotification;
            $scope.qrcode.ownerAlias = data.alias;
            $scope.serviceFinished = true;
            $scope.enableInput();
        }
        var call_user_load_callBackError = function (data) {
            $scope.serviceFinished = true;
            dpnService.processErrorResponse(data);
        }
        dpnService.call_user_load(call_user_load_callBackSuccess, call_user_load_callBackError);
    }

    // load qrcodebean from server OR load user and setup push/emailNotifications flags;
    // process url param qrcode_id
    var qrcode_id = getUrlParam("qrcode_id");
    if (qrcode_id != null && qrcode_id != "undefined" && qrcode_id > 0) {
        // load qrcode callbacks
        var call_qrcode_loadCallBackSuccess = function (data) {

            $scope.qrcode = JSON.parse(JSON.stringify(data));
            $scope.serverData = JSON.parse(JSON.stringify(data));
            $scope.qrcode_chatSupport = ($scope.qrcode.type == 'CHAT');
            // scroll icon list to set icon
            var leftPos = document.getElementById("saveqrcode_scroll_" + $scope.qrcode.icon);
            leftPos = leftPos ? leftPos.offsetLeft : 0;
            var scrollElement = document.getElementById('saveqrcode_scroll_container');
            if (scrollElement) {
                scrollElement.scrollLeft = leftPos;
            }
            
            isLoaded = true;
            $scope.serviceFinished = true;
            $scope.enableInput();
        }
        var call_qrcode_loadCallBackError = function (data) {
            $scope.serviceFinished = true;
            dpnService.processErrorResponse(data);
        }
        // call webservice
        dpnService.call_qrcode_load(qrcode_id, call_qrcode_loadCallBackSuccess, call_qrcode_loadCallBackError);
    } else {
        userLoadWrapper();
    }

    // process newqrcode_token parameter - used from scan screen to acquire new qr code token
    var newqrcode_token = getUrlParam("newqrcode_token");
    if (newqrcode_token) {
        $scope.qrcode.uuid = newqrcode_token;
    }

    // autosave on url change (except sidebar opening)
    // $scope.$on('$locationChangeSuccess', function(event){
    //     if ($location.url().includes("saveqrcode") || isEquivalent($scope.serverData, $scope.qrcode)) {
    //         return;
    //     }
    //     // get base64 from javascript injection from app;
    //     var base64Photo = document.getElementById('qrPhotoUploadButtonForApp').getAttribute('base64');
    //     if (base64Photo && base64Photo.length > 20) {
    //         $scope.qrcode.photo = base64Photo;
    //     }

    //     var request_qrcode = $scope.qrcode;
    //     request_qrcode.type = $scope.qrcode_chatSupport ? 'CHAT' : 'INFO';

    //     dpnDialog.showSaveData(request_qrcode, "saveqrcode", "");
    // })

    // ------------
    // Click events
    // ------------
    $scope.click_saveqrcode = function(qrcode) {
        if (qrcode.id > 0) {
            if (!$scope.qrcode.name || isBlank($scope.qrcode.name)) {
                $scope.qrcode.name = $scope.serverData.name;
            }
            if (!$scope.qrcode.ownerAlias || isBlank($scope.qrcode.ownerAlias)) {
                $scope.qrcode.ownerAlias = $scope.serverData.ownerAlias;
            }
            $scope.click_saveqrcode_submit(qrcode);
        }
    }

    $scope.click_deletePhoto = function() {
        $scope.photoUploadElement.setAttribute('value', "");
        $scope.photoUploadElement.setAttribute('base64', "");
        document.getElementById("qrPhotoUploadImageForApp").setAttribute('src', "");
        $scope.qrcode.photo = ''; // THIS must be LAST, because there is watcher firing on qrcode.photo
    }    

    // to enable input after services loads
    $scope.enableInput = function() {
        document.getElementById('saveqrcode_nameInput').removeAttribute('disabled');
        document.getElementById('saveqrcode_aliasInput').removeAttribute('disabled');
        document.getElementById('saveqrcode_descriptionInput').removeAttribute('disabled');
    }

    $scope.click_saveqrcode_submit = function (qrcode) {
        if (qrcode.id == 0) {
            dpnToast.showToast("INFO", "Saving...", "Saving DMN code");
        }
        if ($scope.isProcessingSubmit && qrcode.id == 0) {
            return;
        }

        // to disable submit button
        $scope.isProcessingSubmit = true;

        // get base64 from javascript injection from app;
        var base64Photo = $scope.photoUploadElement.getAttribute('base64');
        if (base64Photo && base64Photo.length > 20) {
            $scope.qrcode.photo = base64Photo;
        }

        // callbacks
        var click_saveqrcode_callbackSuccess = function (data) {
            if (qrcode.id == 0) {
                dpnToast.showToast("INFO", 'DMN Code saved', 'Your data was saved');
                window.open('#/qrcodelist?qrcode_id=' + data.id, "_self");
            }
            $scope.isProcessingSubmit = false;
        };
        var click_saveqrcode_callbackError = function (data) {
            dpnService.processErrorResponse(data);
            $scope.isProcessingSubmit = false;
        };

        // validation 
        if (isBlank(qrcode.name)) {
            $scope.isProcessingSubmit = false;
            dpnToast.showToast("ERROR", 'Invalid input', 'Empty name!');
            return;
        }
        if (isBlank(qrcode.type)) {
            $scope.isProcessingSubmit = false;
            dpnToast.showToast("ERROR", 'Invalid input', 'Unselected type!');
            return;
        }
        if (isBlank(qrcode.ownerAlias)) {
            $scope.isProcessingSubmit = false;
            dpnToast.showToast("ERROR", 'Invalid input', 'Empty alias');
            return;
        }
        // if (isBlank(qrcode.description)) {
        //     $scope.isProcessingSubmit = false;
        //     dpnToast.showToast("ERROR", 'Invalid input', 'Empty description!');
        //     return;
        // }
        if (!isBlank(qrcode.photo) && qrcode.photo.includes("data:image/gif;base64")){
            $scope.isProcessingSubmit = false;
            dpnToast.showToast("ERROR", 'Invalid input', "App doesn't support .GIF!");
            return;
        }
        if (isBlank(qrcode.icon)) {
            $scope.isProcessingSubmit = false;
            dpnToast.showToast("ERROR", 'Invalid input', 'Unselected icon!');
            return;
        }

        // create request object
        var request_qrcode = qrcode;
        request_qrcode.type = $scope.qrcode_chatSupport ? 'CHAT' : 'INFO';

        // call webservice
        dpnService.call_qrcode_save(request_qrcode, click_saveqrcode_callbackSuccess, click_saveqrcode_callbackError);
    }

    // call send qr by email service
    $scope.isSendingEmail  = false;
    $scope.call_sendQrByEmail = function() {
        if (!$scope.isSendingEmail) {
            $scope.isSendingEmail = true;
            dpnToast.showToast("INFO", 'Downloading DMN Code', 'Sending DMN Code to your email.');

            var call_qrcode_generateCallBackSuccess = function() {
                dpnToast.showToast("INFO", 'Done', 'Email was send');
                $scope.isSendingEmail = false;
            }
            var call_qrcode_generateCallBackError = function(data) {
                dpnService.processErrorResponse(data);
                $scope.isSendingEmail = false;
            }
            dpnService.call_qrcode_generate($scope.qrcode.id, call_qrcode_generateCallBackSuccess, call_qrcode_generateCallBackError);
        }
    }

    $scope.click_back = function() {
        if ($scope.qrcode.id > 0) {
            if (!$scope.qrcode.name || isBlank($scope.qrcode.name)) {
                $scope.qrcode.name = $scope.serverData.name;
            }
            if (!$scope.qrcode.ownerAlias || isBlank($scope.qrcode.ownerAlias)) {
                $scope.qrcode.ownerAlias = $scope.serverData.ownerAlias;
            }
            $scope.click_saveqrcode($scope.qrcode);
        }
        if (!isDialogDisplayed()) {
            window.open("#/qrcodelist", "_self");
        }
    }

    $scope.$watch('qrcode.photo', function (newValue, oldValue, scope) {
        if($scope.qrcode.id > 0 && isLoaded){
            if (!$scope.qrcode.name || isBlank($scope.qrcode.name)) {
                $scope.qrcode.name = $scope.serverData.name;
            }
            if (!$scope.qrcode.ownerAlias || isBlank($scope.qrcode.ownerAlias)) {
                $scope.qrcode.ownerAlias = $scope.serverData.ownerAlias;
            }
            $scope.click_saveqrcode($scope.qrcode);
        }
    });

    $scope.scrollRight = function() {
        var scrollCont = document.getElementById('saveqrcode_scroll_container');
        var iconWidth = 50; //document.getElementById('saveqrcode_scroll_SPORT').style.minWidth;
        scrollCont.scrollLeft = scrollCont.scrollLeft + iconWidth;
    }

    $scope.scrollLeft = function() {
        var scrollCont = document.getElementById('saveqrcode_scroll_container');
        var iconWidth = 50;//document.getElementById('saveqrcode_scroll_SPORT').style.width;
        scrollCont.scrollLeft = scrollCont.scrollLeft - iconWidth;
    }

    // cancel swipe over chat icon list
    // $scope.sidebar = false;

    // $scope.updateSidebar = function($event, show){

    //     var element = $event.toElement || $event.srcElement; 
    //     shouldFire(element) && ($scope.sidebar = show);

    // }

    // $scope.cancelSwipe = function($event){
    //     $event.stopPropagation();
    // }

    // autosave for text input. Saves 2 seconds after change. Clear this action if meanwhile there is new change to alias, description, name
    var timer = null;
    $scope.autosave = function() {
        clearTimeout(timer);
        timer = setTimeout(function() {
            if ($scope.qrcode.name && !isBlank($scope.qrcode.name) 
                &&
                $scope.qrcode.ownerAlias && !isBlank($scope.qrcode.ownerAlias)
            ) {
                $scope.click_saveqrcode($scope.qrcode);
            }
        }, 1500);
    }
});