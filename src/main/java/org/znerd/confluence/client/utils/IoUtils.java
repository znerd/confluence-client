/*
 * Copyright 2016-2017 the original author or authors.
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

package org.znerd.confluence.client.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

public final class IoUtils {

    private IoUtils() {
        throw new UnsupportedOperationException("Utils class cannot be instantiated");
    }

    public static String fileContent(final String filePath, final Charset encoding) {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
            return inputStreamAsString(fileInputStream, encoding);
        } catch (final IOException e) {
            throw new RuntimeException("Could not read file [" + filePath + "] using encoding [" + encoding + "].", e);
        }
    }

    public static String inputStreamAsString(final InputStream is, final Charset encoding) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is, encoding))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        } catch (final IOException e) {
            throw new RuntimeException("Could not convert InputStream to String ", e);
        }
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            closeable.close();
        } catch (final IOException ignored) {
        }
    }
}
