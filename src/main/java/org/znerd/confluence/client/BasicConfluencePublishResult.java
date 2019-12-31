package org.znerd.confluence.client;

import org.znerd.confluence.client.metadata.ConfluencePageMetadata;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static org.znerd.confluence.client.utils.AssertUtils.assertNotBlank;
import static org.znerd.confluence.client.utils.AssertUtils.assertNotNull;

public final class BasicConfluencePublishResult implements ConfluencePublishResult {
    private final String rootConfluenceUrl;
    private final String spaceKey;
    private final String ancestorId;
    private final List<ConfluencePageMetadata> pages;

    public BasicConfluencePublishResult(final String rootConfluenceUrl,
                                        final String spaceKey,
                                        final String ancestorId,
                                        final List<ConfluencePageMetadata> pages) {
        this.rootConfluenceUrl = assertNotBlank(rootConfluenceUrl, "rootConfluenceUrl");
        this.spaceKey = assertNotBlank(spaceKey, "spaceKey");
        this.ancestorId = assertNotBlank(ancestorId, "ancestorId");
        this.pages = unmodifiableList(new ArrayList<>(assertNotNull(pages, "pages")));
    }

    @Override
    public String getRootConfluenceUrl() {
        return rootConfluenceUrl;
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
    public List<ConfluencePageMetadata> getPages() {
        return pages;
    }
}
