package org.springframework.boot.actuate.metrics.ws.domain;

import com.example.ServiceException;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.SERVER, faultStringOrReason = "Fault occurred while processing.")
public class ServiceFaultException extends Exception {
	private final ServiceException faultInfo;

	public ServiceFaultException(String message, ServiceException faultInfo) {
		super(message);
		this.faultInfo = faultInfo;
	}

	public ServiceFaultException(String message, ServiceException faultInfo, Throwable cause) {
		super(message, cause);
		this.faultInfo = faultInfo;
	}

	public ServiceException getFaultInfo() {
		return faultInfo;
	}

}
