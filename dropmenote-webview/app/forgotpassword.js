app.controller("ForgotPasswordDialogController", function ($rootScope, $timeout, $scope, $http, dpnDialog, dpnToast, dpnService) {
    
    $scope.isValidated = null;

    // if parameter with password recovery token exists, validate that token
    var paramEmailRecoveryToken = getUrlParam("recoveryEmailToken");
    if (paramEmailRecoveryToken) {
        var call_user_validateResetPasswordToken_CallbackSuccess = function(data) {
            $timeout(function() {
                $scope.isValidated = true;
                dpnDialog.showForgotPassword('', paramEmailRecoveryToken);
            });
        }
        var call_user_validateResetPasswordToken_CallbackError = function(data) {
            $timeout(function() {
                $scope.isValidated = false;
                dpnToast.showToast("ERROR", "Validation failed", "Your link is not valid!");
            });
        }
        dpnService.call_user_validateResetPasswordToken(paramEmailRecoveryToken, call_user_validateResetPasswordToken_CallbackSuccess, call_user_validateResetPasswordToken_CallbackError);
    }
});