app.controller("BlacklistController", function ($rootScope, $scope, $http, $cookies, dpnService, dpnToast, dpnDialog) {

    // ----------------
    // load data
    // ----------------

    // callbacks
    var loadBlackList_callbackSuccess = function (data) {
        $scope.blacklist = data;
    };
    var loadBlackList_callbackError = function (data) {
        dpnService.processErrorResponse(data);
    };

    // call webservice
    $scope.init = function (){
        dpnService.call_blacklist_loadBlackList(loadBlackList_callbackSuccess, loadBlackList_callbackError);
    }

    // ------------
    // Click events
    // ------------
    $scope.click_showdialog_addNewBlacklist = function () {
        dpnDialog.showEditBlacklist($scope);
    }

    $scope.click_showdialog_modifyBlasklist = function (blacklistObject) {
        dpnDialog.showEditBlacklist($scope, blacklistObject, true);
    }

    $scope.click_back = function() {
        if (!isDialogDisplayed()) {
            window.open("#/setting" , "_self");
        }
    }
    
    // ------------
    // Init function
    // ------------
    $scope.init();


});