app.controller("InboxController", function ($scope, $location, dpnService, dpnDialog, $window) {
    
    $scope.matrixList;

    $scope.orderByParam = "qrCodeBean.id";
    $scope.orderByReverse = false;
    $scope.isOrderClicked = false;

    $scope.finalParamQrId = getUrlParam('qrcode_id');
    $scope.finalParamQrName = getUrlParam('qrcode_name');

    $scope.isFilterClicked = false;
    $scope.filterParam = {
        key: "qrCodeBean.id",
        value: "",
        qrcode_name: ""
    };
    $scope.filteredMatrixObjects = [];
    $scope.dateFormat = 'dd.MM.yyyy';
    $scope.currentDate = new Date();

    $scope.isServiceFinished = false;
    $scope.isFilteredQrAdmin = false;

    $scope.isQrAdmin = function() {
        $scope.matrixList.forEach(function(item){
            // console.info("ccc " + JSON.stringify(item));
            if (item.qrCodeBean.id == $scope.filterParam.value) {
                if (item.qrCodeBean.userType == "ADMIN") {
                    console.info("is true");
                    $scope.isFilteredQrAdmin = true;
                    return;
                }
            }
        });
        if ($scope.isFilteredQrAdmin == true) {
            return;
        }
        $scope.isFilteredQrAdmin = false;
    }

    // format date to either hours:minutes or day.month.year
    // note: was slow when $scope.currentDate = new Date(); in function
    $scope.formatDate = function(date) {
        var date = new Date(date);
        if (!date) {
            date = new Date();
        }
        if (date.getFullYear() == $scope.currentDate.getFullYear() 
        && date.getMonth() == $scope.currentDate.getMonth() 
        && date.getDay() == $scope.currentDate.getDay()) 
        {
            return $scope.dateFormat = 'hh:mm';
        } else {
            return $scope.dateFormat = 'dd.MM.yyyy';
        }
    }

    var qrIdUrlParam = getUrlParam('qrcode_id');
    var qrNameUrlParam = getUrlParam('qrcode_name');
    if (qrIdUrlParam) {
        $scope.filterParam.key = "qrCodeBean.id";
        $scope.filterParam.value = qrIdUrlParam;
        // $scope.isFilterClicked = true;
    } else {
        $scope.filterParam.key = "qrCodeBean.id";
        $scope.filterParam.value = "";
    }
    if (qrNameUrlParam) {
        $scope.filterParam.qrcode_name = qrNameUrlParam;
    } else {
        $scope.filterParam.qrcode_name = "";
    }

    // get previous saved state
    var storedMatrixList = localStorage.getItem('matrixList');
    if (storedMatrixList) {
        $scope.matrixList = JSON.parse(storedMatrixList).matrixList;
        $scope.isQrAdmin();
    }
    // $scope.matrixList = JSON.parse(localStorage.getItem('matrixList') || '{"matrixList":[]}').matrixList;

    // ---------------
    // On url change -
    // ---------------
    var self = this;
    var qrcode_id = null;

    function bindQrName(valueName) {
        // Controller to URL
        $scope.$watch(function() { 
            return $scope.filterParam.qrcode_name;
        }, function (newVal) {
            newVal = $scope.filterParam.qrcode_name;
            $location.search(valueName, newVal);
        });

        // URL to controller
        $scope.$on('$locationChangeSuccess', function(event) {
            $scope.filterParam.key = "qrCodeBean.id";
            $scope.filterParam.qrcode_name = $location.search()[valueName]; 
            // $scope.isFilterClicked = true;
        });
    }

    function bindQrId(valueName) {
        // Controller to URL
        $scope.$watch(function() { 
            return $scope.filterParam.value;
        }, function (newVal) {
            newVal = $scope.filterParam.value;
            $location.search(valueName, newVal);
        });

        // URL to controller
        $scope.$on('$locationChangeSuccess', function(event) {
            $scope.filterParam.key = "qrCodeBean.id";
            $scope.filterParam.value = $location.search()[valueName]; 
            // $scope.isFilterClicked = true;
        });
    }

    // bindQrName("qrcode_name");
    // bindQrId("qrcode_id");


    //--------------
    // load data
    //---------------

    /* save matrixList, so when page reloads it will use that matrixList before new one loads */
    function storeState() {
        let copy = Array.from($scope.matrixList, (item) => {
            let obj = Object.assign({}, item)
            for (let key of Object.keys(obj)) {
              if (key.startsWith('_') || key === '$$hashKey') {
                delete obj[key]
              }
            }
            return obj
        });
        localStorage.setItem('matrixList', JSON.stringify({matrixList: copy}));
    }

    // callbacks
    var call_matrix_loadAdminRooms_callBackSuccess = function (data) {
        $scope.isServiceFinished = true;
        $scope.matrixList = data;
        $scope.isQrAdmin();
        storeState();
    }
    var call_matrix_loadAdminRooms_callBackError = function (data) {
        $scope.isServiceFinished = true;
        dpnService.processErrorResponse(data);
    }

    // call webservice
    dpnService.call_matrix_loadAdminRooms(call_matrix_loadAdminRooms_callBackSuccess, call_matrix_loadAdminRooms_callBackError);

    //-------------
    // Click events
    //-------------

    // vysunie filtrovanie menu (buttony a pole na vpisovanie)
    $scope.click_filter = function() {
        $scope.isFilterClicked = !$scope.isFilterClicked;
        if (!$scope.isFilterClicked) {
            $scope.filterParam.key = "qrCodeBean.id";
            $scope.filterParam.value = ""; 
            $scope.filterParam.qrcode_name = "";
            $scope.filteredMatrixObjects = [];
            $scope.isQrAdmin();
        }
    }

    // vrati len filtrovany objekt
    $scope.filterFunction = function(matrixObject) {
        // vrati objekt do zoznamu, ak obsahuje text
        var isActive = function(obj) {
            // pridava do zoznamu filtrovane polozky. Ak je zoznam prazdny, zobrazi "ziadne polozky"
            if (obj != undefined && obj != null && obj != "") {
                if (obj.toString() == $scope.filterParam.value || !$scope.filterParam.value) {
                    if (!$scope.filteredMatrixObjects.includes(obj)) {
                        $scope.filteredMatrixObjects.push(obj);
                    }
                } else {
                    $scope.filteredMatrixObjects = $scope.filteredMatrixObjects.filter(x => x !== obj);
                }
                return obj.toString() == $scope.filterParam.value || !$scope.filterParam.value;
            }
        }
        
        switch ($scope.filterParam.key) {
            case "matrixRoomId": return isActive(matrixObject.roomId); 
            case "newMsgCount": return isActive(matrixObject.newMsgCount); 
            case "qrCodeBean.id": return isActive(matrixObject.qrCodeBean.id); 
            case "qrCodeBean.type": return isActive(matrixObject.qrCodeBean.type); 
            case "qrCodeBean.ownerAlias": return isActive(matrixObject.qrCodeBean.ownerAlias); 
            case "qrCodeBean.name": return isActive(matrixObject.qrCodeBean.name); 
            case "qrCodeBean.icon": return isActive(matrixObject.qrCodeBean.icon); 
            default: return matrixObject;
        }
    }

    //////////// to open dialog
    $scope.click_openInboxFilterDialog = function() {
        dpnDialog.showInboxFilter();
    }

    $scope.click_removeFilter = function() {
        $scope.filterParam.key = "qrCodeBean.id";
        $scope.filterParam.value = "";
        $scope.filterParam.qrcode_name = "";
        $scope.isQrAdmin();
    }

    $scope.click_back = function() {
        if (!isDialogDisplayed()) {
            if ($scope.filterParam.value) {
                window.open("#/saveqrcode?qrcode_id=" + $scope.filterParam.value, "_self");
            } else {
                $window.history.back();
            }
        }
    }

   
});