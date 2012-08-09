 
$(document).ready(function() {
    $('#create-tracker-form').submit(function() {
	var form = $('#create-tracker-form');
	var name = $('#tracker-name').val();
	var code = $('#tracker-code').val();
	var shared_secret = $('#shared-secret').val();

	var request = {
	    "tracker": {
		"name": name,
		"code": code,
		"shared_secret": shared_secret
		}
	    };

	function createSuccess(data, status) {
	    console.log("success", data, JSON.stringify(data));
	    $('#feedback').hide();
	    $('#feedback').text("Tracker created! " + JSON.stringify(data)).fadeIn(500);
	};
	function createFailure(data, status) {
	    var result = JSON.parse(data.responseText);
	    console.log("fail", data.responseText);
	    $('#feedback').hide();
	    $('#feedback').text("Tracker creation failed: " + result.error.message).fadeIn(500);
	};

	$.ajax({url: '/api/v1-dev/trackers',
		type: 'POST',
		data: JSON.stringify(request),
		processData: false,
		contentType: 'application/json',
		success: createSuccess,
		error: createFailure
	       });
	console.log("sent");
	form.show().fadeOut(500);
	return false;
    });
});
