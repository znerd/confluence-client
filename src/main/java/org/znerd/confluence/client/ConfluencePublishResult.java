package org.znerd.confluence.client;

import java.util.List;

public interface ConfluencePublishResult {
    static ConfluencePublishResultBuilder builder() {
        return new ConfluencePublishResultBuilder();
    }

    String getConfluenceRootUrl();

    String getSpaceKey();

    String getAncestorId();

    List<PublishedPageInfo> getPages();
}
