package org.springframework.boot.actuate.metrics.ws;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;

public class WsMetricsEndpointInterceptor implements EndpointInterceptor {

	@Override
	public boolean handleRequest(MessageContext messageContext, Object endpoint) {
		TransportContext previousTransportContext = TransportContextHolder.getTransportContext();
		HttpServletConnection connection = (HttpServletConnection) previousTransportContext.getConnection();

		TimingContext timingContext = TimingContext.get(connection.getHttpServletRequest());
		timingContext.setRequest(connection.getHttpServletRequest());
		timingContext.setResponse(connection.getHttpServletResponse());
		timingContext.setEndpoint(endpoint);
		timingContext.attachTo(messageContext);

		return true;
	}

	@Override
	public boolean handleResponse(MessageContext messageContext, Object endpoint) {
		return true;
	}

	@Override
	public boolean handleFault(MessageContext messageContext, Object endpoint) {
		return true;
	}

	@Override
	public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
		TimingContext timingContext = TimingContext.get(messageContext);
		HttpServletRequest httpServletRequest = timingContext.getRequest();
		timingContext.setResponseMessage(messageContext.getResponse());
		timingContext.attachTo(httpServletRequest);
	}
}
