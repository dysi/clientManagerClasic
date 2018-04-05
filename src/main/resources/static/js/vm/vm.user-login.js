function VMUserLogin(msg, port) {

	// Params handlers
	if (msg) {
		alertify.success(msg);
	}
	
	console.log("Port " + port);

	// Form validation
	$("#loginForm").validate({
		rules: {
			username: {
				required: true,
				minlength: 4
			},
			password: {
				required: true,
				minlength: 4
			}
		}
	});

	// KnokoutJS
	function UserLoginVM() {
		var vm = this;
		// credentials
		vm.username = ko.observable("");
		vm.password = ko.observable("");

		// var
		vm.isLoading = ko.observable(false);
		vm.errorAlertMsg = ko.observable("");
		// vm.renderHome = renderHome;

		// Methods
		vm.onSubmit = login;

		function login() {
			vm.errorAlertMsg("");
			vm.isLoading(true);
			var credentials = {
				username: vm.username(),
				password: vm.password()
			}

			axios.post("/clientmanager/auth",
					credentials, {
						headers: {
							'Content-Type': 'application/json'
						}
					})
				.then(function (res) {
					console.log(res);
					vm.isLoading(false);
					localStorage.setItem('token', res.data.token);
					window.location.href = "/clientmanager/index/home";
				})
				.catch(function (error) {
					vm.isLoading(false);
					vm.errorAlertMsg("El nombre de usuario o contraseña es incorrecto");
					console.log(error);
				});
		}
		
	}

	ko.applyBindings(new UserLoginVM());
}