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

import com.example.GetOriginV1Request;
import com.example.GetPersonV1Request;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.actuate.metrics.ws.client.PersonClient;
import org.springframework.boot.actuate.metrics.ws.config.WsTestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ws.soap.client.SoapFaultClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

/**
 * Tests for {@link WebMvcMetricsFilter} in the presence of a custom exception handler.
 *
 * @author Jon Schneider
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WsTestConfiguration.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "debug=true", "server.port=8081" })
class WsMetricsIntegrationTests {

	@Autowired
	private SimpleMeterRegistry registry;

	@Autowired
	private PersonClient personClient;

	@BeforeEach
	public void setUp() {
		this.registry.getMeters().forEach(meter -> registry.remove(meter));
	}

	@Test
	void soapFaultIsRecordedInMetricTag() {
		Throwable throwable =
				catchThrowable(
						() -> this.personClient.getOrigin(GetOriginV1Request.builder().build()));

		assertThat(throwable)
				.isInstanceOf(SoapFaultClientException.class)
				.hasMessageContaining("Fault occurred while processing.");

		await().untilAsserted(() -> {
			assertThat(this.registry.get("ws.server.requests").tags(
					"faultCode", "Server",
					"faultReason", "Fault occurred while processing.",
					"operation", "getOriginV1",
					"status", "500").timer()
					.count()).isEqualTo(1L);
		});
	}

	@Test
	void thrownExceptionIsRecordedInMetricTag() {
		Throwable throwable =
				catchThrowable(
						() -> this.personClient.getPerson(GetPersonV1Request.builder().build()));

		assertThat(throwable)
				.isInstanceOf(SoapFaultClientException.class)
				.hasMessageContaining("java.lang.NullPointerException");

		await().untilAsserted(() -> {
			assertThat(this.registry.get("ws.server.requests").tags(
					"faultCode", "Server",
					"faultReason", "java.lang.NullPointerException",
					"operation", "getPersonV1",
					"status", "500").timer()
					.count()).isEqualTo(1L);
		});
	}
}
