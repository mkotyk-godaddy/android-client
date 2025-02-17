package fake;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;

import static java.lang.Thread.sleep;

public class HttpClientMock implements HttpClient {
    HttpResponseMockDispatcher mResponseDispatcher;
    public HttpClientMock(HttpResponseMockDispatcher responseDispatcher) throws IOException {
        mResponseDispatcher = responseDispatcher;
    }

    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public void addHeaders(Map<String, String> headers) {
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod httpMethod) {
        HttpResponse response = mResponseDispatcher.getResponse(uri, httpMethod, null);
        return new HttpRequestMock(response);
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod httpMethod, String body) {
        HttpResponse response = mResponseDispatcher.getResponse(uri, httpMethod, body);
        return new HttpRequestMock(response);
    }

    @Override
    public HttpStreamRequest streamRequest(URI uri) {
        HttpStreamResponse response = mResponseDispatcher.getStreamResponse(uri);
        return new HttpStreamRequestMock(response);
    }

    @Override
    public void close() {
    }
}
