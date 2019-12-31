package org.znerd.confluence.client;

import org.znerd.confluence.client.metadata.ConfluencePageMetadata;

import java.util.List;

public interface ConfluencePublishResult {
    String getRootConfluenceUrl();

    String getSpaceKey();

    String getAncestorId();

    List<ConfluencePageMetadata> getPages();
}
