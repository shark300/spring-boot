package org.springframework.boot.actuate.metrics.ws.config;

import java.util.Objects;

import com.example.GetOriginV1Request;
import com.example.GetOriginV1Response;
import com.example.GetPersonV1Request;
import com.example.GetPersonV1Response;
import com.example.ObjectFactory;
import com.example.ServiceException;

import org.springframework.boot.actuate.metrics.ws.domain.ServiceFaultException;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class PersonEndpoint {

	public static final String NAMESPACE_URI = "http://example.com/";

	private final ObjectFactory objectFactory = new ObjectFactory();

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getPersonV1Request")
	@ResponsePayload
	public GetPersonV1Response getPersonV1(@RequestPayload GetPersonV1Request request) {
		Objects.requireNonNull(request.getId());
		GetPersonV1Response response = objectFactory.createGetPersonV1Response();
		response.setName("Peter");
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getOriginV1Request")
	@ResponsePayload
	public GetOriginV1Response getOriginV1(@RequestPayload GetOriginV1Request request)
			throws ServiceFaultException {
		try {
			Objects.requireNonNull(request.getId());
			GetOriginV1Response personResponse = objectFactory.createGetOriginV1Response();
			personResponse.setName("Peter");
			return personResponse;
		}
		catch (Exception e) {
			throw new ServiceFaultException(e.getMessage(), ServiceException.builder().withText(e.getMessage()).build());
		}
	}
}
