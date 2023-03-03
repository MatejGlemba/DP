app.service("dpnDialog", ['$mdDialog', function ($mdDialog ) {

    var parentEl = angular.element(document.body);

    // ----------------
    // Show dialogs
    // ----------------
    this.showLogin = function(login, password) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_login.html',
            locals: {
                paramLogin: login,
                paramPassword: password
            },
            controller: 'DLoginController'
        });
    };

    this.showLogout = function() {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_logout.html',
            locals: {},
            controller: 'DLogoutController'
        });
    };
    
    this.showForgotPasswordEmailSend = function(paramEmail) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            templateUrl: 'd_forgotpasswordemailsend.html',
            locals: {
                paramEmail: paramEmail
            },
            controller: 'DForgotPasswordEmailSendController'
        });
    }

    this.showSaveData = function(paramObject, paramScreen, paramDestination) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            templateUrl: 'd_savedata.html',
            locals: {
                paramObject: paramObject, 
                paramScreen: paramScreen,
                paramDestination: paramDestination
            },
            controller: 'DSaveDataController'
        });
    }

    this.showForgotPassword = function(paramEmail, paramEmailRecoveryToken) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_forgotpassword.html',
            locals: {
                paramEmail: paramEmail,
                paramEmailRecoveryToken: paramEmailRecoveryToken
            },
            controller: 'DForgotPasswordController'
        });
    };

    this.showChangePassword = function(paramLogin) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            templateUrl: 'd_changepassword.html',
            locals: {
                paramLogin: paramLogin
            },
            controller: 'DChangePasswordController'
        });
    };

    this.showInboxFilter = function() {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            templateUrl: 'd_inboxfilter.html',
            locals: {},
            controller: 'DInboxFilterController'
        });
    };

    this.showRegistration = function() {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_registration.html',
            locals: {
                //items: $scope.items
            },
            controller: 'DRegistrationController'
        });
    };

    this.showEditBlacklist = function(parent, blacklistObject, paramShowDelete) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_editblacklist.html',
            locals: {
                param_parent: parent,
                param_blacklistObject: blacklistObject,
                param_showDelete: paramShowDelete
            },
            controller: 'DBlacklistController'
        });
    };

    // qr code share user
    this.showAddQrCodeShare = function(parent, param_qrcode_id) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_addqrcodeshare.html',
            locals: {
                param_parent: parent,
                param_qrcode_id: param_qrcode_id
            },
            controller: 'DAddQrCodeShareController'
        });
    };
    
    this.showEditQrCodeShare = function(param_parent, param_qrshareObject, param_qrcode_id) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_editqrcodeshare.html',
            locals: {
                param_parent: param_parent,
                param_qrshareObject: param_qrshareObject,
                param_qrcode_id: param_qrcode_id
            },
            controller: 'DEditQrCodeShareController'
        });
    };

    // dialog na prompt 'instaluj si appku'
    this.showInstallApp = function(parent) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            templateUrl: 'd_installapp.html',
            locals: {
                param_parent: parent,
            },
            controller: 'DInstallAppController'
        });
    };
    
    // dialog chat blacklist
    this.showChatBlacklist = function(parent, param_blacklist) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_chatblacklist.html',
            locals: {
                param_parent: parent,
                param_blacklist: param_blacklist
            },
            controller: 'DChatBlacklistController'
        });
    };
    

    // dialog chat blacklist
    this.showQrNotFound = function(parent) {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'd_qrcodenotfound.html',
            locals: {
                param_parent: parent,
            },
            controller: 'DQrCodeNotFoundController'
        });
    };
    /*
    * Toto je klasicky Confirmation dialog. Message, ano a cancel. Pouziva sa napriakld na delete zaznamov.
    */
    this.showConfirmationDialog = function() {
        $mdDialog.show({
            fullscreen: true,
            parent: parentEl,
            //targetEvent: $event,
            templateUrl: 'confirmation.html',
            locals: {
                //items: $scope.items
            },
            controller: 'ConfirmationController'
        });
    };

}]);
