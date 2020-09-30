(function VMUserForm() {

	// Form validation
	var AUTH_TOKEN = localStorage.getItem('token');
	
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
      // form.submit();
      
      try {
    	  var authoritiesAuxList = $("#userForm").find('input[name="authorities"]');
          var authoritiesAux = [];
          $.each(authoritiesAuxList, function( index, item ) {
    		  if(item.checked){
    			  authoritiesAux.push(parseInt(item.value));
    		  }
    		});
          
          var userId = $("#userForm").find('input[name="id"]').val();
          var user = {
        		id: (userId != '' ? parseInt(userId) : null),
        		username: $("#userForm").find('input[name="username"]').val(),
        		password: $("#userForm").find('input[name="password"]').val(),
        		firstname: $("#userForm").find('input[name="firstname"]').val(),
        		lastname: $("#userForm").find('input[name="lastname"]').val(),
        		email: $("#userForm").find('input[name="email"]').val(),
        		enabled: ($("#userForm").find('input[name="enabled"]').val() == "true"),
        		clientID: parseInt($("#userForm").find('select[name="clientID"]').val()),
        		authorities: authoritiesAux
    		}
          
          axios.post("../api/users/edit",
        	user, {
    			headers: {
    				'Authorization': 'Bearer ' + AUTH_TOKEN,
    				'Content-Type': 'application/json'
    			}
    		})
    	.then(function (res) {
    		console.log(res);
    		if(!!res){
    			var dataSplit = res.data.split("|");
    			if(dataSplit[0] == "1"){
    				window.location.replace("/clientmanager/index/home");
    				return false;
    			} else {
    				console.log("ERROR: " + dataSplit[1]);
    				$(".submit").attr("disabled", false);        
    		        return false;
    			}
        	}
    	})
    	.catch(function (error) {
    		console.log(error);
    		$(".submit").attr("disabled", false);        
            return false;
    	});
      } catch (e) {
    	  $(".submit").attr("disabled", false);        
          return false;
      }
      
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