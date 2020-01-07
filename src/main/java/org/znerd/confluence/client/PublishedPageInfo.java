package org.znerd.confluence.client;

import org.znerd.confluence.client.metadata.ConfluencePageMetadata;

import java.util.Objects;

import static org.znerd.confluence.client.utils.AssertUtils.assertNotBlank;
import static org.znerd.confluence.client.utils.AssertUtils.assertNotNull;

public final class PublishedPageInfo {
    private final String spaceKey;
    private final String ancestorId;
    private final ConfluencePageMetadata page;
    private final String pageId;

    public PublishedPageInfo(final String spaceKey, final String ancestorId, final ConfluencePageMetadata page, final String pageId) {
        this.spaceKey = assertNotBlank(spaceKey, "spaceKey");
        this.ancestorId = ancestorId;
        this.page = assertNotNull(page, "page");
        this.pageId = assertNotBlank(pageId, "contentId");
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public String getAncestorId() {
        return ancestorId;
    }

    public ConfluencePageMetadata getPage() {
        return page;
    }

    public String getTitle() {
        return page.getTitle();
    }

    public String getPageId() {
        return pageId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PublishedPageInfo that = (PublishedPageInfo) o;
        return Objects.equals(spaceKey, that.spaceKey) &&
                Objects.equals(ancestorId, that.ancestorId) &&
                Objects.equals(page, that.page) &&
                Objects.equals(pageId, that.pageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spaceKey, ancestorId, page, pageId);
    }

    @Override
    public String toString() {
        return "PublishedPageInfo{" +
                "spaceKey='" + spaceKey + '\'' +
                ", ancestorId='" + ancestorId + '\'' +
                ", page=" + page +
                ", contentId='" + pageId + '\'' +
                '}';
    }
}
