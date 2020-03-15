package org.springframework.boot.actuate.metrics.ws;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import org.springframework.boot.actuate.metrics.ws.WsMetricsEndpointInterceptor.TimingContext;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.NestedServletException;

public class WsMetricsFilter extends OncePerRequestFilter {

	private final MeterRegistry registry;

	private final WsTagsProvider tagsProvider;

	private final String metricName;

	private final boolean autoTimeRequests;

	public WsMetricsFilter(MeterRegistry registry, WsTagsProvider tagsProvider, String metricName,
			boolean autoTimeRequests) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimeRequests = autoTimeRequests;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		TimingContext timingContext = TimingContext.get(request);
		if (timingContext == null) {
			startAndAttachTimingContext(request);
		}
		try {
			timingContext = TimingContext.get(request);
			filterChain.doFilter(request, response);
			record(timingContext);
		}
		catch (NestedServletException ex) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			timingContext.setException(ex.getCause());
			record(timingContext);
			throw ex;
		}
	}

	private void startAndAttachTimingContext(HttpServletRequest request) {
		Timer.Sample timerSample = Timer.start(this.registry);
		TimingContext timingContext = new TimingContext(timerSample);
		timingContext.attachTo(request);
	}

	private Set<Timed> getTimedAnnotations(Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return Collections.emptySet();
		}
		return getTimedAnnotations((HandlerMethod) handler);
	}

	private Set<Timed> getTimedAnnotations(HandlerMethod handler) {
		Set<Timed> timed = findTimedAnnotations(handler.getMethod());
		if (timed.isEmpty()) {
			return findTimedAnnotations(handler.getBeanType());
		}
		return timed;
	}

	private Set<Timed> findTimedAnnotations(AnnotatedElement element) {
		MergedAnnotations annotations = MergedAnnotations.from(element);
		if (!annotations.isPresent(Timed.class)) {
			return Collections.emptySet();
		}
		return annotations.stream(Timed.class).collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	private void record(TimingContext timingContext) {
		Set<Timed> annotations = getTimedAnnotations(timingContext);
		Timer.Sample timerSample = timingContext.getTimerSample();
		Supplier<Iterable<Tag>> tags = () -> this.tagsProvider.getTags(timingContext.getRequest(),
				timingContext.getResponse(), timingContext.getEndpoint(), timingContext.getResponseMessage(),
				timingContext.getException());
		if (annotations.isEmpty()) {
			if (this.autoTimeRequests) {
				stop(timerSample, tags, Timer.builder(this.metricName));
			}
		}
		else {
			for (Timed annotation : annotations) {
				stop(timerSample, tags, Timer.builder(annotation, this.metricName));
			}
		}
	}

	private void stop(Timer.Sample timerSample, Supplier<Iterable<Tag>> tags, Timer.Builder builder) {
		timerSample.stop(builder.tags(tags.get()).register(this.registry));
	}

}
