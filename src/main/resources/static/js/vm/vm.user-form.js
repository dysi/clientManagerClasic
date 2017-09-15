(function VMUserForm() {

  // Form validation
  $("#userForm").validate({
    rules: {
      username: {
        required: true,
        minlength: 4
      },
      firstname: {
        required: true,
        minlength: 4
      },
      lastname: {
        required: true,
        minlength: 4
      },
      email: {
        required: true,
        email: true
      },
      password: {
        required: true,
        minlength: 4
      },
      confirmPassword: {
        required: true,
        equalTo: "#password"
      },
      authority: {
        required: true
      }
    },
    submitHandler: function (form) {
      debugger;
      var authValid = false;
      $(".submit").attr("disabled", true);
      $(".authority").each(function(index, value){
        if(!authValid){
          authValid = $(this).is(":checked");
        }
      })
      if (!$("#userForm").valid() || !authValid) {
        $(".submit").attr("disabled", false);        
        return false;
      }
      form.submit();
    }
  });

  function UserFormVM() {
    var vm = this;

    // var
    vm.isLoading = ko.observable(false);

    // Methods handlers

  }

  ko.applyBindings(new UserFormVM());
})();