package org.znerd.confluence.client;

import org.znerd.confluence.client.metadata.ConfluencePageMetadata;

import java.util.ArrayList;
import java.util.List;

public final class ConfluencePublishResultBuilder {
    private final List<PublishedPageInfo> pages;

    private String confluenceRootUrl;
    private String spaceKey;
    private String ancestorId;

    ConfluencePublishResultBuilder() {
        this.pages = new ArrayList<>();
    }

    public ConfluencePublishResultBuilder defaults() {
        // no particular defaults to apply
        return this;
    }

    public ConfluencePublishResultBuilder setRootConfluenceUrl(final String confluenceRootUrl) {
        this.confluenceRootUrl = confluenceRootUrl;
        return this;
    }

    public ConfluencePublishResultBuilder setSpaceKey(final String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    public ConfluencePublishResultBuilder setAncestorId(final String ancestorId) {
        this.ancestorId = ancestorId;
        return this;
    }

    public ConfluencePublishResultBuilder addPage(final String spaceKey, final String ancestorId, final ConfluencePageMetadata page, final String contentId) {
        pages.add(new PublishedPageInfo(spaceKey, ancestorId, page, contentId));
        return this;
    }

    public ConfluencePublishResult build() {
        return new BasicConfluencePublishResult(confluenceRootUrl, spaceKey, ancestorId, pages);
    }
}
