app.controller("DRegistrationController", function ($rootScope, $scope, $http, $cookies, dpnService, dpnToast) {

    // init data
    $scope.registration = {
        login: '',
        password: '',
        repeatpassword: '',
        deviceId: ''
    };

    //-------------------------
    // Click events
    //-------------------------
    $scope.click_registration_submit = function (registration, form) {

        // callbacks
        var click_registration_submit_callbackSuccess = function () {
            dpnToast.showToast("INFO", 'Registration', 'Your accout was created');
            window.open("#/qrcodelist", "_self");
        };
        var click_registration_submit_callbackError = function (data) {
            dpnService.processErrorResponse(data);
        };

        repeatpasswordValidate(form);
        if (registration.login != "" && checkIfEmailInString(registration.login)) {
            if ((registration.password == registration.repeatpassword) && registration.password != "") {

                var registration_request = {
                    login: registration.login,
                    password: registration.password,
                    deviceId: registration.deviceId
                }
                // call webservice
                dpnService.call_user_register(registration_request, click_registration_submit_callbackSuccess, click_registration_submit_callbackError);
            } else {
                // alert('password is not the same as repeat password!'); //TODO delete
            }
        } else {
            // alert('invalid email format!'); //TODO delete
        }
    }

    // check if passwords are same, otherwise display error-msg
    var repeatpasswordValidate = function (form) {
        if ($scope.registration.password != $scope.registration.repeatpassword) {
            form.registration_repeatpassword.$setValidity('passwordMismatch', false);
        } else {
            form.registration_repeatpassword.$setValidity('passwordMismatch', true);
        }
    };

    $scope.click_back = function() {
        
    }
});