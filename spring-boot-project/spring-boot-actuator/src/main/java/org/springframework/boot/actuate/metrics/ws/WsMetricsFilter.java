package org.springframework.boot.actuate.metrics.ws;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.NestedServletException;

/**
 * Intercepts incoming WS requests and records metrics about Spring MVC execution time
 * and results.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
public class WsMetricsFilter extends OncePerRequestFilter {

	private final MeterRegistry registry;

	private final WsTagsProvider tagsProvider;

	private final String metricName;

	private final AutoTimer autoTimer;

	/**
	 * Create a new {@link WsMetricsFilter} instance.
	 * @param registry the meter registry
	 * @param tagsProvider the tags provider
	 * @param metricName the metric name
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 */
	public WsMetricsFilter(MeterRegistry registry, WsTagsProvider tagsProvider, String metricName,
			AutoTimer autoTimer) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimer = autoTimer;
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
		if (annotations.isEmpty()) {
			Builder builder = this.autoTimer.builder(this.metricName);
			Timer timer = getTimer(builder, timingContext);
			timerSample.stop(timer);
			return;
		}
		for (Timed annotation : annotations) {
			Builder builder = Timer.builder(annotation, this.metricName);
			timerSample.stop(getTimer(builder, timingContext));
		}
	}

	private Timer getTimer(Builder builder, TimingContext timingContext) {
		return builder.tags(this.tagsProvider.getTags(timingContext.getRequest(), timingContext.getResponse(), timingContext.getEndpoint(), timingContext.getResponseMessage(), timingContext.getException())).register(this.registry);
	}

}
