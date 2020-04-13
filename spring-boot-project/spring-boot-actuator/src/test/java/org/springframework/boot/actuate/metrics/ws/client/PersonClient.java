package org.springframework.boot.actuate.metrics.ws.client;

import com.example.GetOriginV1Request;
import com.example.GetOriginV1Response;
import com.example.GetPersonV1Request;
import com.example.GetPersonV1Response;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

public class PersonClient extends WebServiceGatewaySupport {

	private static final String PERSON_SERVICE_NAME = "getPersonV1";

	private static final String ORIGIN_SERVICE_NAME = "getOriginV1";

	private final int port;

	public PersonClient(int port) {
		this.port = port;
	}

	public GetPersonV1Response getPerson(GetPersonV1Request personRequest) {
		String address = "http://localhost:" + port + "/soap/" + PERSON_SERVICE_NAME;
		return (GetPersonV1Response)
				getWebServiceTemplate().marshalSendAndReceive(address, personRequest);
	}

	public GetOriginV1Response getOrigin(GetOriginV1Request personRequest) {
		String address = "http://localhost:" + port + "/soap/" + ORIGIN_SERVICE_NAME;
		return (GetOriginV1Response)
				getWebServiceTemplate().marshalSendAndReceive(address, personRequest);
	}
}
