(function VMUserForm() {


  function UserFormVM() {
    var vm = this;
    // credentials
    vm.password = ko.observable("");
    vm.authoritySelected = ko.observable();

    // var
    vm.isLoading = ko.observable(false);
    vm.authorityList = ko.observableArray();

    // Methods handlers
    vm.addAuthority = addAuthority;
    vm.removeAuthority = removeAuthority;

    function addAuthority(authorityId) {

      console.log(authorityId);

    }

    function removeAuthority(index, data) {
      vm.authorityList.splice(index, 1);
    }

  }

  ko.applyBindings(new UserFormVM());
})();