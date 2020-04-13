/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.ws;

import java.util.List;

import javax.servlet.DispatcherType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Ws.Server.ServerRequest;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.ws.DefaultWsTagsProvider;
import org.springframework.boot.actuate.metrics.ws.WsMetricsEndpointInterceptor;
import org.springframework.boot.actuate.metrics.ws.WsMetricsFilter;
import org.springframework.boot.actuate.metrics.ws.WsTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring Web
 * MVC servlet-based request mappings.
 *
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(MetricsProperties.class)
public class WsMetricsAutoConfiguration {

	private final MetricsProperties properties;

	public WsMetricsAutoConfiguration(MetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(WsTagsProvider.class)
	public DefaultWsTagsProvider wsTagsProvider() {
		return new DefaultWsTagsProvider();
	}

	@Bean
	public FilterRegistrationBean<WsMetricsFilter> wsMetricsFilter(MeterRegistry registry,
			WsTagsProvider tagsProvider) {
		ServerRequest request = this.properties.getWs().getServer().getRequest();
		WsMetricsFilter filter = new WsMetricsFilter(registry, tagsProvider, request.getMetricName(),
				request.getAutotime());
		FilterRegistrationBean<WsMetricsFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

	@Bean
	@Order(0)
	public MeterFilter metricsWsServerUriTagFilter() {
		String metricName = this.properties.getWs().getServer().getRequest().getMetricName();
		MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
				() -> String.format("Reached the maximum number of URI tags for '%s'.", metricName));
		return MeterFilter.maximumAllowableTags(metricName, "uri", this.properties.getWs().getServer().getMaxUriTags(),
				filter);
	}

	@Bean
	public MetricsWsConfigurer metricsWsConfigurer() {
		return new MetricsWsConfigurer();
	}

	/**
	 * {@link MetricsWsConfigurer} to add metrics interceptors.
	 */
	static class MetricsWsConfigurer extends WsConfigurerAdapter {


		@Override
		public void addInterceptors(List<EndpointInterceptor> interceptors) {
			interceptors.add(new WsMetricsEndpointInterceptor());
		}
	}

}
