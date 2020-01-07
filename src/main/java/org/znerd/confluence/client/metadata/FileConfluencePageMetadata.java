package org.znerd.confluence.client.metadata;

import org.znerd.confluence.client.support.RuntimeUse;
import org.znerd.confluence.client.utils.IoUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

public final class FileConfluencePageMetadata implements ConfluencePageMetadata {
    private String                       title;
    private String                       contentFilePath;
    private List<ConfluencePageMetadata> children    = new ArrayList<>();
    private Map<String, String> attachments = new HashMap<>();

    @Override
    public String getTitle() {
        return this.title;
    }

    @RuntimeUse
    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentFilePath() {
        return this.contentFilePath;
    }

    @Override
    public String getContent() {
        return IoUtils.fileContent(getContentFilePath(), UTF_8);
    }

    @RuntimeUse
    public void setContentFilePath(String contentFilePath) {
        this.contentFilePath = contentFilePath;
    }

    @Override
    public List<ConfluencePageMetadata> getChildren() {
        if (this.children == null) {
            return emptyList();
        } else {
            return this.children;
        }
    }

    @RuntimeUse
    public void setChildren(List<ConfluencePageMetadata> children) {
        this.children = children;
    }

    @Override
    public Map<String, String> getAttachments() {
        return this.attachments;
    }

    @RuntimeUse
    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }
}
