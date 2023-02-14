app.controller("TutorialController", function ($rootScope, $scope, $http, $cookies, dpnService, $mdDialog, dpnDialog, dpnToast) {

    // ----------------
    // tries to login
    // ---------------
    $scope.deviceId= fingerprint_uuid;

    var call_user_loginByToken_callbackSuccess = function () {
        window.open("#/qrcodelist","_self");
    };
    var call_user_loginByToken_callbackError = function () {
        //DO NOTHING;
    };
    // call webservice
    dpnService.call_user_loginByToken($scope.deviceId, call_user_loginByToken_callbackSuccess, call_user_loginByToken_callbackError);

});