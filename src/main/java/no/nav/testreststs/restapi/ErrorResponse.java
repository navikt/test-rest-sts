package no.nav.testreststs.restapi;

public class ErrorResponse {
	private String error;
	
	public ErrorResponse() {
		
	}
	public ErrorResponse(String error) {
		this.error = error;
	}
	public String getError() {
		return error;
	}

}
