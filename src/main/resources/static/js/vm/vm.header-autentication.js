var isLoginPage = window.location.href.indexOf("login") != -1;
if (isLoginPage) {
	if (localStorage.getItem('token')) {
		localStorage.removeItem('token');
	}
} else {
	if (localStorage.getItem('token')) {
		
	} else {
		window.location.href = "../users/login";
	}
}