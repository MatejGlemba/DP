app.controller("MainController", ['$rootScope', '$cookies', '$scope', '$http', 'dpnService', 'dpnToast', '$window', function ($rootScope, $cookies, $scope, $http, dpnService, dpnToast, $window) {

    var param_q = getUrlParam('q');
    if (param_q && param_q != 'null' && param_q != 'undefined') {
        window.open("#/infoqrcode?q=" + param_q, "_self");
    }
    // TODO toto treba refaktornut, este to nie je hotove. rozrobeny task pre hidovanie pozadia
    // get cookie, if doesnt exist, return empty string
    $scope.getCookieTokenObject = function() {
        // var cookie = $cookies.get("dpn_token");
        var cookie = localStorage.getItem("dpn_token");
        if(cookie){
            try {
                return JSON.parse(cookie);
            }catch(e){
                return JSON.parse('{"date": 0, "token":""}');
            }
        }else{
            return JSON.parse('{"date": 0, "token":""}');
        }
    }

    // Needed for the loading screen
    $rootScope.$on("$routeChangeStart", function () {
        $rootScope.loading = true;
    });

    $rootScope.$on("$routeChangeSuccess", function () {
        $rootScope.loading = false;
    });

    $scope.isUserLogged = function(){
        var tokenFromSession = $scope.getCookieTokenObject().token;
        if (tokenFromSession == undefined || tokenFromSession == null || tokenFromSession == '' ){
            return false;
        }else{
            return true;
        }
    }

    // Window resize event
    angular.element($window).bind('resize', function () {

        var footerDiv = document.getElementById('footerDiv');
        var footerDeadeSettingDiv = document.getElementById('headerSettingDiv');
        var headerQRcodeDiv = document.getElementById('headerQRcodeDiv');

        if($window.innerHeight < initSizeHeight){
            // hide footer and header
            if(footerDiv != null){
                footerDiv.style.display = "none"; 
            }

            if(footerDeadeSettingDiv != null){
                footerDeadeSettingDiv.style.display = "none"; 
            }

            if(headerQRcodeDiv != null){
                headerQRcodeDiv.style.display = "none"; 
            }
        }else{
            // show footer and header
            if(footerDiv != null){
                footerDiv.style.display = "block"; 
            }

            if(footerDeadeSettingDiv != null){
                footerDeadeSettingDiv.style.display = "block"; 
            }

            if(headerQRcodeDiv != null){
                headerQRcodeDiv.style.display = "block"; 
            }
        }

    });
}]);