/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.znerd.confluence.client.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.znerd.confluence.client.utils.IoUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

import static java.nio.charset.Charset.defaultCharset;

/**
 * @author Alain Sahli
 * @author Christian Stettler
 * @author Laurent Verbruggen
 */
@SuppressWarnings("WeakerAccess")
public class RequestFailedException extends RuntimeException {

    RequestFailedException(HttpRequest request, HttpResponse response) {
        super("" +
                response.getStatusLine().getStatusCode() +
                " " +
                response.getStatusLine().getReasonPhrase() +
                " " +
                request.getRequestLine().getMethod() +
                " " +
                request.getRequestLine().getUri() +
                "\n" +
                "request: '" + failedRequestContent(request) + "'" +
                "\n" +
                "response: '" + failedResponseContent(response) + "'"
        );
    }

    private static String failedRequestContent(HttpRequest request) {
        return request instanceof HttpEntityEnclosingRequest ? entityAsString(((HttpEntityEnclosingRequest) request).getEntity()) : "";
    }

    private static String failedResponseContent(HttpResponse response) {
        return entityAsString(response.getEntity());
    }

    private static String entityAsString(HttpEntity entity) {
        try {
            InputStream content = entity.getContent();
            Charset encoding = entity.getContentEncoding() == null ? defaultCharset() : Charset.forName(entity.getContentEncoding().getValue());
            String contentString = IoUtils.inputStreamAsString(content, encoding);

            return contentString;
        } catch (Exception ignored) {
            return "";
        }
    }
}
