package org.znerd.confluence.client;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static org.znerd.confluence.client.utils.AssertUtils.assertNotBlank;
import static org.znerd.confluence.client.utils.AssertUtils.assertNotNull;

public final class BasicConfluencePublishResult implements ConfluencePublishResult {
    private final String confluenceRootUrl;
    private final String spaceKey;
    private final String ancestorId;
    private final List<PublishedPageInfo> pages;

    public BasicConfluencePublishResult(final String confluenceRootUrl,
                                        final String spaceKey,
                                        final String ancestorId,
                                        final List<PublishedPageInfo> pages) {
        this.confluenceRootUrl = assertNotBlank(confluenceRootUrl, "confluenceRootUrl");
        this.spaceKey = assertNotBlank(spaceKey, "spaceKey");
        this.ancestorId = assertNotBlank(ancestorId, "ancestorId");
        this.pages = unmodifiableList(new ArrayList<>(assertNotNull(pages, "pages")));
    }

    @Override
    public String getConfluenceRootUrl() {
        return confluenceRootUrl;
    }

    @Override
    public String getSpaceKey() {
        return spaceKey;
    }

    @Override
    public String getAncestorId() {
        return ancestorId;
    }

    @Override
    public List<PublishedPageInfo> getPages() {
        return pages;
    }
}
