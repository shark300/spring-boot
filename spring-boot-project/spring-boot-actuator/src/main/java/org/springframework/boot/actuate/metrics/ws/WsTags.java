/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.ws;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import io.micrometer.core.instrument.Tag;

import org.springframework.util.StringUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.server.endpoint.MethodEndpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.soap.SoapMessage;

import static java.util.Optional.ofNullable;

/**
 * Factory methods for {@link Tag Tags} associated with a request-response exchange that
 * is handled by Spring MVC.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Michael McFadyen
 * @since 2.0.0
 */
public final class WsTags {

	private static final Tag URI_UNKNOWN = Tag.of("uri", "UNKNOWN");

	private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

	private static final Tag FAULTREASON_NONE = Tag.of("faultReason", "None");

	private static final Tag STATUS_UNKNOWN = Tag.of("status", "UNKNOWN");

	private static final Tag OUTCOME_UNKNOWN = Tag.of("outcome", "UNKNOWN");

	private static final Tag OUTCOME_INFORMATIONAL = Tag.of("outcome", "INFORMATIONAL");

	private static final Tag OUTCOME_SUCCESS = Tag.of("outcome", "SUCCESS");

	private static final Tag OUTCOME_REDIRECTION = Tag.of("outcome", "REDIRECTION");

	private static final Tag OUTCOME_CLIENT_ERROR = Tag.of("outcome", "CLIENT_ERROR");

	private static final Tag OUTCOME_SERVER_ERROR = Tag.of("outcome", "SERVER_ERROR");

	private static final Tag METHOD_UNKNOWN = Tag.of("method", "UNKNOWN");

	private static final Tag OPERATION_UNKNOWN = Tag.of("operation", "UNKNOWN");

	private static final Tag FAULTCODE_NONE = Tag.of("faultCode", "None");

	private WsTags() {
	}

	public static Tag operation(Object endpoint) {
		Tag operation = OPERATION_UNKNOWN;
		if (endpoint instanceof MethodEndpoint) {
			Method method = ((MethodEndpoint) endpoint).getMethod();
			PayloadRoot annotation = method.getAnnotation(PayloadRoot.class);
			if (annotation != null) {
				String localPart = annotation.localPart();
				if (hasSuffix(localPart)) {
					String operationName = removeSuffix(localPart);
					operation = Tag.of("operation", operationName);
				}
			}
			else {
				operation = Tag.of("operation", method.getName());
			}
		}
		return operation;
	}

	private static boolean hasSuffix(String localPart) {
		return localPart.endsWith("Request");
	}

	private static String removeSuffix(String messageName) {
		return messageName.substring(0, messageName.length() - "Request".length());
	}

	public static Tag method(HttpServletRequest request) {
		return (request != null) ? Tag.of("method", request.getMethod()) : METHOD_UNKNOWN;
	}

	public static Tag status(HttpServletResponse response) {
		return (response != null) ? Tag.of("status", Integer.toString(response.getStatus())) : STATUS_UNKNOWN;
	}

	public static Tag uri(HttpServletRequest request) {
		return ofNullable(request).map(HttpServletRequest::getRequestURI).map(e -> Tag.of("uri", e))
				.orElse(URI_UNKNOWN);
	}

	public static Tag faultReason(Throwable exception, WebServiceMessage responseMessage) {
		return ofNullable(responseMessage).filter(e -> e instanceof SoapMessage).map(e -> (SoapMessage) e)
				.map(SoapMessage::getFaultReason).map(e -> Tag.of("faultReason", e)).orElse(FAULTREASON_NONE);
	}

	public static Tag faultCode(WebServiceMessage responseMessage) {
		return ofNullable(responseMessage).map(e -> (SoapMessage) e).map(SoapMessage::getFaultCode)
				.map(QName::getLocalPart).map(faultCode -> Tag.of("faultCode", faultCode)).orElse(FAULTCODE_NONE);
	}

	public static Tag exception(Throwable exception) {
		return ofNullable(exception).map(Throwable::getClass).map(Class::getSimpleName)
				.map(simpleName -> Tag.of("exception",
						StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName()))
				.orElse(EXCEPTION_NONE);
	}

	public static Tag outcome(HttpServletResponse response) {
		if (response != null) {
			int status = response.getStatus();
			if (status < 200) {
				return OUTCOME_INFORMATIONAL;
			}
			if (status < 300) {
				return OUTCOME_SUCCESS;
			}
			if (status < 400) {
				return OUTCOME_REDIRECTION;
			}
			if (status < 500) {
				return OUTCOME_CLIENT_ERROR;
			}
			return OUTCOME_SERVER_ERROR;
		}
		return OUTCOME_UNKNOWN;
	}

}
