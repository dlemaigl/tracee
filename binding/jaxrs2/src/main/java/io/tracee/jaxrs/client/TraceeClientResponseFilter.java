package io.tracee.jaxrs.client;

import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import io.tracee.TraceeConstants;
import io.tracee.transport.HttpHeaderTransport;


import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.tracee.configuration.TraceeFilterConfiguration.Channel.IncomingResponse;

@Provider
public class TraceeClientResponseFilter implements ClientResponseFilter {

    private final TraceeBackend backend;
    private final HttpHeaderTransport transportSerialization;

	@SuppressWarnings("unused")
	public TraceeClientResponseFilter() {
		this(Tracee.getBackend(), new HttpHeaderTransport(Tracee.getBackend().getLoggerFactory()));
	}

	TraceeClientResponseFilter(TraceeBackend backend, HttpHeaderTransport transportSerialization) {
		this.backend = backend;
		this.transportSerialization =  transportSerialization;
	}

	@Override
    public final void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        final List<String> serializedHeaders = responseContext.getHeaders().get(TraceeConstants.HTTP_HEADER_NAME);
        if (serializedHeaders != null && backend.getConfiguration().shouldProcessContext(IncomingResponse)) {
			final Map<String, String> parsed = transportSerialization.parse(serializedHeaders);
			backend.putAll(backend.getConfiguration().filterDeniedParams(parsed, IncomingResponse));
		}
    }
}