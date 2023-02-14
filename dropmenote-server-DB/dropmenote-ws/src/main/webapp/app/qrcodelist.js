app.controller("QrcodelistController", function ($rootScope, $scope, $http, $cookies, dpnService, dpnToast, dpnDialog) {
    
    $scope.qrcodelist = null;
    
    function postToCN1(msg) { //TODELETE
        if (window.cn1PostMessage) {
            // Case 1: Running inside native app in a WebView
            window.cn1PostMessage(msg);
        } else {
            // Case 2: Running inside a Javascript app in an iframe
            window.parent.postMessage(msg, '*');
        }
    }

    // get previous saved state
    var storedQrlist = localStorage.getItem('qrcodelist');
    if (storedQrlist) {
        $scope.qrcodelist = JSON.parse(storedQrlist).qrcodelist;
    }

    /* save qrcodelist, so when page reloads it will use that qrcodelist before new one loads */
    function storeState() {
        let copy = Array.from($scope.qrcodelist, (item) => {
            let obj = Object.assign({}, item)
            for (let key of Object.keys(obj)) {
              if (key.startsWith('_') || key === '$$hashKey') {
                delete obj[key]
              }
            }
            return obj
        });
        localStorage.setItem('qrcodelist', JSON.stringify({qrcodelist: copy}));
    }
    
    var urlDeviceId = window.location.href.split("di=")[1];
    if (urlDeviceId) {
        deviceId = urlDeviceId;
    }
    //https://app.dropmenote.com/#/qrcodelist?v=123&di=23452345
    // const urlParams = new URLSearchParams(window.location.search);
    // $scope.deviceIdLocal = urlParams.get('di');
    // postToCN1("qrcodelist.js " + $scope.deviceIdLocal); //TODELETE
    // if ($scope.deviceIdLocal) {
    //     deviceId = $scope.deviceIdLocal;
    // }

    $rootScope.$on('$locationChangeStart', function() {
        $rootScope.previousPage = location.pathname;
    });

    //--------------
    // load data
    //---------------

    // callbacks
    var call_qrcode_loadAll_callBackSuccess = function (data) {
        $scope.qrcodelist = data.qrList;
        storeState();
    }
    var call_qrcode_loadAll_callBackError = function (data) {
        dpnService.processErrorResponse(data);
    }

    // call webservice
    dpnService.call_qrcode_loadAll(call_qrcode_loadAll_callBackSuccess, call_qrcode_loadAll_callBackError);

    //-------------
    // Click events
    //-------------

    // click create new qrcode
    $scope.click_new_qrcode = function() {
        window.open('#/saveqrcode', "_self");
        //TODO mozno volba ci naskenovať existujuci qr kod a z neho vytvorit novy dropmenote_qrcode
    }

    // show toast if shared used
    $scope.showToastShared = function(qrcode) {
        if (qrcode.userType == "SHARED") {
            dpnToast.showToast("INFO", "Shared Item", "Shared user can't modify Item")
        }
    }

    // calculate href if its Shared QR
    $scope.calculateHref = function(qrcode) {
        if (qrcode.userType == undefined) {
            dpnDialog.showLogin();
        } else {
            return qrcode.userType == "SHARED" 
            ? '#/inbox?qrcode_id=' + qrcode.id + '&qrcode_name=' + qrcode.name 
            : '#/saveqrcode?qrcode_id=' + qrcode.id;
        }
    }

    $scope.$on('login.timestamp.value', function (event, value) {
        if (value) {
            var urlDeviceId = window.location.href.split("di=")[1];
            if (urlDeviceId) {
                deviceId = urlDeviceId;
            }
            // const urlParams = new URLSearchParams(window.location.search);
            // $scope.deviceIdLocal = urlParams.get('di');
            // if ($scope.deviceIdLocal) {
            //     deviceId = $scope.deviceIdLocal;
            // }
            dpnService.call_qrcode_loadAll(call_qrcode_loadAll_callBackSuccess, call_qrcode_loadAll_callBackError);
        }
    });
});