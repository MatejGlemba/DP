app.controller("SettingController", function ($rootScope, $scope, $http, $cookies, $location, dpnService, dpnToast, dpnDialog, $timeout) {

    // init data
    // get previous saved state
    var storedUser = localStorage.getItem('user');
    if (storedUser && storedUser != {}) {
        $scope.user = storedUser;
    } else {
        $scope.user = {
            login: '',
            alias: '',
            photo: 'TODO delete',
            pushNotification: true,
            emailNotification: true,
            password: '',
            chatIcon: 'P1'
        };
    }

    /* save user, so when page reloads it will use that user before new one loads */
    function storeState() {
        let copy = Array.from($scope.user, (item) => {
            let obj = Object.assign({}, item)
            for (let key of Object.keys(obj)) {
              if (key.startsWith('_') || key === '$$hashKey') {
                delete obj[key]
              }
            }
            return obj
        });
        localStorage.setItem('user', JSON.stringify({user: copy}));
    }

    // ---------------
    // load data
    // ---------------
    // callbacks
    var call_user_load_callBackSuccess = function (data) {
        $scope.user = JSON.parse(JSON.stringify(data));
        $scope.serverData = JSON.parse(JSON.stringify(data));
        localStorage.setItem('user', JSON.stringify($scope.user));
        // storeState();

        // scroll icon div
        var leftPos = document.getElementById("setting_" + $scope.user.chatIcon);
        leftPos = leftPos ? leftPos.offsetLeft : 0;
        var scrollElement = document.getElementById('setting_scroll_container');
        if (scrollElement) {
            scrollElement.scrollLeft = leftPos - 35;
        }
    }
    var call_user_load_callBackError = function (data) {
        dpnService.processErrorResponse(data);
    }
    dpnService.call_user_load(call_user_load_callBackSuccess, call_user_load_callBackError);

    // autosave on url change (except sidebar opening)
    // $scope.$on('$locationChangeSuccess', function(event){
    //     if ($location.url().includes("setting") || isEquivalent($scope.serverData, $scope.user)) {
    //         return;
    //     }
    //     dpnDialog.showSaveData($scope.user, "setting", "");
    // })


    // -----------------
    // on click function
    // -----------------
    $scope.click_saveuser_submit = function (userRequest) {

        // userRequest.login = $scope.login.login;

        // update user callbacks
        var click_saveuser_submit_callbackSuccess = function () {
            //dpnToast.showToast("INFO", 'Settings saved', 'Your data was saved');
        };
        var click_saveuser_submit_callbackError = function (data) {
            dpnService.processErrorResponse(data);
        };

        // validations
        if (userRequest.alias == null || userRequest.alias == "") {
            dpnToast.showToast("ERROR", 'Invalid input', 'Alias is required field');
            return;
        }
        // if (userRequest.password == null || userRequest.password == "") { // password sa nemeni pri volani user update servisy
        //   alert('empty password');
        //   return;
        // }
        //if (userRequest.photo == null || userRequest.photo == "") {
        //    dpnToast.showToast("ERROR", 'Invalid input', 'Photo is required field');
        //    return;
        //}
        if (userRequest.login == null || userRequest.login == "") {
            dpnToast.showToast("INFO", 'Not loged in', 'Not logged in');
            return;
        }

        // call update user
        dpnService.call_user_update(userRequest, click_saveuser_submit_callbackSuccess, click_saveuser_submit_callbackError);
    }

    // show change password dialog
    $scope.click_showChangePassword = function () {
        dpnDialog.showChangePassword($scope.user.login);
    }

    // show logout dialog
    $scope.click_logout = function() {
        dpnDialog.showLogout();
    }

    $scope.scrollRight = function() {
        var scrollCont = document.getElementById('setting_scroll_container');
        var iconWidth = 50; //document.getElementById('setting_P1').style.minWidth;
        scrollCont.scrollLeft = scrollCont.scrollLeft + iconWidth;
    }

    $scope.scrollLeft = function() {
        var scrollCont = document.getElementById('setting_scroll_container');
        var iconWidth = 50;//document.getElementById('setting_P1').style.width;
        scrollCont.scrollLeft = scrollCont.scrollLeft - iconWidth;
    }

    $scope.$on('login.timestamp.value', function (event, value) {
        if (value) {
            dpnService.call_user_load(call_user_load_callBackSuccess, call_user_load_callBackError);
        }
    });

    // autosave for text input. Saves 2 seconds after change. Clear this action if meanwhile there is new change to alias
    var timer = null;
    $scope.autosave = function() {
        clearTimeout(timer);
        timer = setTimeout(function() {
            $scope.click_saveuser_submit($scope.user);}
        , 1500);
    }
});