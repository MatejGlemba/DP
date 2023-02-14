app.controller("QrcodeshareController", function ($rootScope, $scope, $http, $mdDialog, dpnService, dpnDialog) {

    //--------------
    // load data
    //---------------
    $scope.qrcode_id = 0;
    $scope.qrcodeshares = [];

    // process url param qrcode_id
    $scope.qrcode_id = getUrlParam("qrcode_id");

    // define webservice calls
    $scope.init = function () {
        // callbacks
        var call_qrcode_loadSharesCallBackSuccess = function (data) {
            $scope.qrcodeshares = data;
        }
        var call_qrcode_loadSharesCallBackError = function (data) {
            dpnService.processErrorResponse(data);
        }
        // call webservice
        dpnService.call_qrcode_loadShares($scope.qrcode_id, call_qrcode_loadSharesCallBackSuccess, call_qrcode_loadSharesCallBackError);
    }

    // ------------
    // Click events
    // ------------
    $scope.click_showdialog_addQrcodeShare = function () {
        dpnDialog.showAddQrCodeShare($scope, $scope.qrcode_id);
    }

    $scope.click_showdialog_modifyQrcodeShare = function (qrcodeshare) {
        dpnDialog.showEditQrCodeShare($scope, qrcodeshare, $scope.qrcode_id);
    }
    
    // call webservice
    if ($scope.qrcode_id != null && $scope.qrcode_id != "undefined" && $scope.qrcode_id > 0) {
        $scope.init();
    } 

    $scope.click_back = function() {
        if (!isDialogDisplayed()) {
            window.open('#/saveqrcode?qrcode_id=' + $scope.qrcode_id, "_self");
        }
    }
});