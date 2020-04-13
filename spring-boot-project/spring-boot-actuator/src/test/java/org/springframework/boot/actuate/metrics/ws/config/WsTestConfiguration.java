package org.springframework.boot.actuate.metrics.ws.config;

import java.util.List;

import javax.servlet.DispatcherType;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.actuate.metrics.ws.DefaultWsTagsProvider;
import org.springframework.boot.actuate.metrics.ws.WsMetricsEndpointInterceptor;
import org.springframework.boot.actuate.metrics.ws.WsMetricsFilter;
import org.springframework.boot.actuate.metrics.ws.client.PersonClient;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.ws.wsdl.wsdl11.Wsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@Configuration(proxyBeanMethods = false)
@EnableWs
public class WsTestConfiguration {

	@Value("${server.port}")
	private int port;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ServletWebServerFactory webServerFactory() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.setPort(this.port);
		return factory;
	}

	@Bean
	public MockClock clock() {
		return new MockClock();
	}

	@Bean
	public MeterRegistry meterRegistry(Clock clock) {
		return new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
	}

	@Bean
	public FilterRegistrationBean<WsMetricsFilter> wsMetricsFilter(MeterRegistry registry) {
		WsMetricsFilter filter = new WsMetricsFilter(registry, new DefaultWsTagsProvider(), "ws.server.requests",
				AutoTimer.ENABLED);
		FilterRegistrationBean<WsMetricsFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

	@Bean
	public MetricsWsConfigurer metricsWsConfigurer() {
		return new MetricsWsConfigurer();
	}

	@Bean
	public Jaxb2Marshaller marshaller() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("com.example");
		return marshaller;
	}

	@Bean
	public PersonClient personClient(Jaxb2Marshaller marshaller) {
		PersonClient client = new PersonClient(port);
		client.setMarshaller(marshaller);
		client.setUnmarshaller(marshaller);
		return client;
	}

	@Bean
	public ServletRegistrationBean messageDispatcherServlet(ApplicationContext applicationContext) {
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		servlet.setTransformWsdlLocations(true);
		return new ServletRegistrationBean(servlet, "/soap/*");
	}

	@Bean(name = "people")
	public Wsdl11Definition defaultWsdl11Definition(XsdSchema personSchema) {
		DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
		wsdl11Definition.setPortTypeName("PersonPort");
		wsdl11Definition.setLocationUri("/soap");
		wsdl11Definition.setTargetNamespace("http://example.com/");
		wsdl11Definition.setSchema(personSchema);
		return wsdl11Definition;
	}

	@Bean
	public XsdSchema personSchema() {
		return new SimpleXsdSchema(new ClassPathResource("ws/person.xsd"));
	}

	@Bean
	public PersonEndpoint personEndpoint() {
		return new PersonEndpoint();
	}

	static class MetricsWsConfigurer extends WsConfigurerAdapter {

		@Override
		public void addInterceptors(List<EndpointInterceptor> interceptors) {
			interceptors.add(new WsMetricsEndpointInterceptor());
		}
	}
}