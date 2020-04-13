package org.springframework.boot.actuate.metrics.ws;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.Timer;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;

class TimingContext {

	private static final String ATTRIBUTE = TimingContext.class.getName();

	private final Timer.Sample timerSample;

	private Object endpoint;

	private HttpServletRequest request;

	private HttpServletResponse response;

	private WebServiceMessage responseMessage;

	private Throwable exception;

	TimingContext(Timer.Sample timerSample) {
		this.timerSample = timerSample;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public void setEndpoint(Object endpoint) {
		this.endpoint = endpoint;
	}

	public Object getEndpoint() {
		return endpoint;
	}

	public Timer.Sample getTimerSample() {
		return this.timerSample;
	}

	public void setResponseMessage(WebServiceMessage responseMessage) {
		this.responseMessage = responseMessage;
	}

	public WebServiceMessage getResponseMessage() {
		return responseMessage;
	}

	public void setException(Throwable cause) {
		this.exception = cause;
	}

	public Throwable getException() {
		return exception;
	}

	public void attachTo(MessageContext request) {
		request.setProperty(ATTRIBUTE, this);
	}

	public void attachTo(HttpServletRequest request) {
		request.setAttribute(ATTRIBUTE, this);
	}

	public static TimingContext get(MessageContext messageContext) {
		return (TimingContext) messageContext.getProperty(ATTRIBUTE);
	}

	public static TimingContext get(HttpServletRequest request) {
		return (TimingContext) request.getAttribute(ATTRIBUTE);
	}

}
