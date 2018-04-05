function VMHome() {

	// KnokoutJS
	function HomeVM() {
		var vm = this;

		vm.isLoading = ko.observable(false);
		vm.clienteID = ko.observable(-1);
		vm.clientesList = ko.observableArray([]);
		
		vm.client = ko.observable();
		
		vm.editando = ko.observable(false);
		
		vm.companyId = ko.observable(-1);
		
		vm.licenciasList = ko.observableArray([]);
		
		vm.licenciaUpd = ko.observable();
		
		vm.pagosList = ko.observableArray([]);
		
		var AUTH_TOKEN = localStorage.getItem('token');

		// Methods
		vm.initHome = function() {
			
			vm.isLoading(true);
			
			vm.clientesList().length = 0;
			
			$.ajax
			({
			  type: "GET",
			  url: "../api/clients/getClients",
			  dataType: 'json',
			  async: false,
			  headers: {
			    "Authorization": "Bearer " + AUTH_TOKEN
			  },
			  success: function (data){
				if(!!data){
	        		vm.clientesList(data);
	        	}
			  },
			  error: function (error){
				  console.log("vm.initHome ERROR: " + error);
			  }
			});
			
		};
		
		vm.clientChanged = function() {
			vm.editando(false);
			vm.client(null);
			vm.licenciaUpd(null);
			$.each(vm.clientesList(), function( index, client ) {
			  if(client.id == vm.clienteID()){
				  client.idOld = client.id;
				  vm.client(client);
				  vm.getLicencias();
				  
				  vm.client.valueHasMutated();
				  return;
			  }
			});
		};
		
		vm.clientNew = function() {
			
			vm.licenciasList().length = 0;
			vm.pagosList().length = 0;
			vm.licenciasList.valueHasMutated();
			vm.pagosList.valueHasMutated();
			
			var clientNew = {id: "", firstname: "", lastname: "", cuil: "", phone: "", email: "", idOld: -1 };
			vm.client(clientNew);
			
			vm.clientEdit();
			
		};
		
		vm.clientEdit = function() {
			vm.editando(true);
			vm.editando.valueHasMutated();
		};
		
		vm.clientRevert = function() {
			if(!!vm.clienteID() && vm.clienteID() > 0){
				vm.clientChanged();
			} else {
				vm.client(null);
				vm.client.valueHasMutated();
			}
			
		};
		
		vm.clientGuardar = function() {
			
			axios.post("../api/clients/edit",
				vm.client(), {
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
//						if(vm.client().idOld < 1){
//							var listAux = vm.clientesList();
//							listAux.push(vm.client());
//						} else {
//							$.each(vm.clientesList(), function( index, client ) {
//								 if(client.id == parseInt(vm.client().id)){
//									 client = vm.client();
//								 }
//							});
//						}
//						
//						vm.clientesList.valueHasMutated();
//						
//						vm.clienteID(vm.client().id);
//						vm.clientChanged();
//						
//						console.log(dataSplit[1]);
						
						window.location.replace("home");
						
					} else {
						console.log("ERROR: " + dataSplit[1]);
					}
					vm.editando(false);
	        	}
			})
			.catch(function (error) {

				console.log(error);
			});
			
		};
		
		// LICENCIAS
		vm.getLicencias = function() {
			
			vm.isLoading(true);
			vm.licenciasList().length = 0;
			
			$.ajax
			({
			  type: "GET",
			  url: "../api/payments/licenses",
			  dataType: 'json',
			  async: false,
			  data : { clientID : vm.clienteID() },
			  headers: {
			    "Authorization": "Bearer " + AUTH_TOKEN
			  },
			  success: function (data){
				if(!!data){
//					data.licenseId;
//					data.description;
//					data.companyName;
//					data.companyId;
	        		vm.licenciasList(data);
	        	}
			  },
			  error: function (error){
				  console.log("vm.getLicencias ERROR: " + error);
			  }
			});
			
		};
		
		vm.licenciaNew = function() {
			vm.licenciaUpd({ id: -1, companyId: "", companyName: "", description: "", initialActivity: "", prePayment: true });
			
		};
		
		vm.editarLicencia = function(data) {
			$.each(vm.licenciasList(), function( index, lic ) {
			  if(lic.id == data.id){
				  vm.licenciaUpd(lic);			  
				  vm.licenciaUpd.valueHasMutated();
				  
				  $("#companyID").focus();
				  
				  return;
			  }
			});
		};
		
		vm.licenciaRevert = function() {
			vm.licenciaUpd(null);			  
			vm.licenciaUpd.valueHasMutated();
		};
		
		vm.licenciaGuardar = function() {
			
			var dataPost = jQuery.extend({}, vm.licenciaUpd());
			
			//ELIMINO ATRIBUTO YA QUE NO ME ITERESA
			delete dataPost.licenseStatuses;
			dataPost.clienteId = vm.clienteID();
			
			//Formateo la fecha como timestamp
			var dateAux = moment($("#initialActivity").val());
			dataPost.initialActivity = dateAux.valueOf();
			
			axios.post("../api/payments/edit", dataPost, {
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
							
							vm.clientChanged();
							
							//console.log(dataSplit[1]);
							
						} else {
							console.log("ERROR: " + dataSplit[1]);
						}
		        	}
				})
				.catch(function (error) {
					console.log(error.message);
				});
		};
		
		vm.licenciaBloquear = function(data) {
			console.log("vm.licenciaBloquear: Muy Pronto " + data.companyId);
		};
		
		
		// PAGOS
		vm.getPagos = function(data) {
			
			vm.companyId(data.companyId);
			vm.isLoading(true);
			vm.pagosList().length = 0;
			
			$.ajax
			({
			  type: "GET",
			  url: "../api/payments/paymentsByCompany",
			  dataType: 'json',
			  async: false,
			  data : { clientID : vm.clienteID(), companyId : vm.companyId() },
			  headers: {
			    "Authorization": "Bearer " + AUTH_TOKEN
			  },
			  success: function (payments){
				if(!!payments){
	        		vm.pagosList(payments);
	        	}
			  },
			  error: function (error){
				  console.log("vm.getPagos ERROR: " + error);
			  }
			});
			
		};
		
		vm.pagoNew = function() {
			console.log("vm.pagoNew: Muy Pronto");
		};
		
		vm.editarPago = function() {
			console.log("vm.editarPago: Muy Pronto");
		};
		
		
		vm.initHome();
		
	}

	ko.applyBindings(new HomeVM(), document.getElementById('container.home'));
}