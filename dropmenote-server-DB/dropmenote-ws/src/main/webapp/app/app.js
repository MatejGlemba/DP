/* eslint no-alert: 0 */

"use strict";

// var webappVersion = "?v=2";
var webappVersion = "?v=13";

// Localhost
var configuration_baseUrl = "http://localhost:8080/dropmenote-ws";
var configuration_wsUrl = "ws://localhost:8080/dropmenote-ws/websocket";

// PetoD
// var configuration_baseUrl = "http://192.168.88.205:8080/starbug-dropmenote-ws";
// var configuration_wsUrl = "ws://192.168.88.205:8080/starbug-dropmenote-ws/websocket";

//var configuration_baseUrl = "https://app.dropmenote.com/starbug-dropmenote-ws-1.0-SNAPSHOT";
//var configuration_wsUrl = "wss://app.dropmenote.com/starbug-dropmenote-ws-1.0-SNAPSHOT/websocket";

//var configuration_baseUrl = "https://app.dropmenote.com/dropmenote-ws";
//var configuration_wsUrl = "wss://app.dropmenote.com/dropmenote-ws/websocket";

var userSessionToken = "token";
var deviceId = "";
var initSizeHeight = window.innerHeight;

//
// Here is how to define your module
// has dependent on mobile-angular-ui
//
var app = angular.module("DropMeNoteAppUI", [
    "ngRoute",
    "mobile-angular-ui",
    "ngMessages", // TODO toto nepotrebujeme
    "mobile-angular-ui.gestures",
    "ngMaterial", // Toast
    "ngCookies",
    "luegg.directives",
    "textAngular"
]);

app.run(function ($transform) {
    window.$transform = $transform;
});

//
// You can configure ngRoute as always, but to take advantage of SharedState location
// feature (i.e. close sidebar on backbutton) you should setup 'reloadOnSearch: false'
// in order to avoid unwanted routing.
//
app.config(function ($routeProvider) {
    $routeProvider.when("/saveqrcode", {
        templateUrl: "s_saveqrcode.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/qrcodelist", {
        templateUrl: "s_qrcodelist.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/setting", {
        templateUrl: "s_setting.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/d_login", {
        templateUrl: "d_login.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/d_registration", {
        templateUrl: "d_registration.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/about", {
        templateUrl: "s_about.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/guide", {
        templateUrl: "s_guide.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/blacklist", {
        templateUrl: "s_blacklist.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/forgotpassword", {
        templateUrl: "s_forgotpassword.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/d_forgotpassword", {
        templateUrl: "d_forgotpassword.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/chat", {
        templateUrl: "s_chat.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/qrcodeshare", {
        templateUrl: "s_qrcodeshare.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/inbox", {
        templateUrl: "s_inbox.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/infoqrcode", {
        templateUrl: "s_infoqrcode.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/", {
        templateUrl: "s_qrcodelist.html" + webappVersion,
        reloadOnSearch: false,
    });
    $routeProvider.when("/d_forgotpasswordemailsend", {
        templateUrl: "d_forgotpasswordemailsend.html" + webappVersion,
        reloadOnSearch: false,
    });
});

/**
 * custom validation for email, since native angularjs validation does not warn after character '@' is typed
 */
app.directive("validateEmail", function () {
    // var EMAIL_REGEXP = /^[_a-z0-9]+(\.[_a-z0-9]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$/;
    // var EMAIL_REGEXP = /(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))/;
    var EMAIL_REGEXP = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,5}))$/;

    return {
        require: "ngModel",
        restrict: "",
        link: function (scope, elm, attrs, ctrl) {
            // only apply the validator if ngModel is present and Angular has added the email validator
            if (ctrl && ctrl.$validators.email) {
                // this will overwrite the default Angular email validator
                ctrl.$validators.email = function (modelValue) {
                    return ctrl.$isEmpty(modelValue) || EMAIL_REGEXP.test(modelValue);
                };
            }
        },
    };
});

app.directive("fileread", [
    function () {
        return {
            scope: {
                fileread: "=",
            },
            link: function (scope, element, attributes) {
                element.bind("change", function (changeEvent) {
                    var reader = new FileReader();
                    reader.onload = function (loadEvent) {
                        scope.$apply(function () {
                            scope.fileread = loadEvent.target.result;
                        });
                    };
                    reader.readAsDataURL(changeEvent.target.files[0]);
                });
            },
        };
    },
]);

// to scroll in chat on last element
app.directive("setFocus", function () {
    return {
        scope: { setFocus: "=" },
        link: function (scope, element) {
            if (scope.setFocus) element[0].focus();
        },
    };
});

function prepareTokenObject(tokenFromService, timestamp) {
    return JSON.stringify({ date: timestamp, token: tokenFromService });
}

/*
 * check if text is valid email string
 */
function checkIfEmailInString(text) {
    var re = /(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))/;
    return re.test(text);
}

/*
 * used to get parameter from url
 */
function getUrlParam(paramName) {
    var vars = {};
    var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi, function (m, key, value) {
        vars[key] = value;
    });
    // return vars[paramName];
    return decodeURIComponent((vars[paramName] + "").replace(/\+/g, "%20"));
}

/*
 * check if string has only whitespaces
 */
function isBlank(str) {
    return !str || /^\s*$/.test(str);
}

/**
 * check if object is contained in list
 * @param {*} obj
 * @param {*} list
 */
function containsObject(obj, list) {
    var i;
    for (i = 0; i < list.length; i++) {
        if (JSON.stringify(list[i]) === JSON.stringify(obj)) {
            return true;
        }
    }
    return false;
}

/**
 * return true if md-dialog is displayed
 */
function isDialogDisplayed() {
    return angular.element(document.body).hasClass("md-dialog-is-showing");
}

/**
 * check if 2 objects have equal values in same fields;
 * @param {*} a
 * @param {*} b
 */
function isEquivalent(a, b) {
    // Create arrays of property names
    if (!a || !b || a == {} || b == {}) {
        return false;
    }
    var aProps = Object.getOwnPropertyNames(a);
    var bProps = Object.getOwnPropertyNames(b);

    // If number of properties is different,
    // objects are not equivalent
    if (aProps.length != bProps.length) {
        return false;
    }

    for (var i = 0; i < aProps.length; i++) {
        var propName = aProps[i];

        // If values of same property are not equal,
        // objects are not equivalent
        if (a[propName] !== b[propName]) {
            return false;
        }
    }

    // If we made it this far, objects
    // are considered equivalent
    return true;
}

/**
 * this function sends message to CN1
 * @param {*} msg
 */
function sendMessageToCn1(msg) {
    window.parent.postMessage(msg, "*");
    if (window.cn1PostMessage) {
        // Case 1: Running inside native app in a WebView
        window.cn1PostMessage(msg);
    } else {
        // Case 2: Running inside a Javascript app in an iframe
        window.parent.postMessage(msg, "*");
    }
}
