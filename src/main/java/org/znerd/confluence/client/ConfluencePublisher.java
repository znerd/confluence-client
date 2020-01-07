/*
 * Copyright 2016-2019 the original author or authors.
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

package org.znerd.confluence.client;

import org.znerd.confluence.client.http.ConfluenceAttachment;
import org.znerd.confluence.client.http.ConfluenceClient;
import org.znerd.confluence.client.http.ConfluencePage;
import org.znerd.confluence.client.http.NotFoundException;
import org.znerd.confluence.client.metadata.ConfluencePageMetadata;
import org.znerd.confluence.client.metadata.ConfluencePublisherMetadata;
import org.znerd.confluence.client.utils.IoUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.znerd.confluence.client.utils.AssertUtils.assertMandatoryParameter;
import static org.znerd.confluence.client.utils.AssertUtils.assertNotNull;
import static org.znerd.confluence.client.utils.IoUtils.closeQuietly;

public class ConfluencePublisher {
    static final String CONTENT_HASH_PROPERTY_KEY = "content-hash";
    static final int INITIAL_PAGE_VERSION = 1;

    private final ConfluencePublisherMetadata metadata;
    private final PublishingStrategy publishingStrategy;
    private final ConfluenceClient confluenceClient;
    private final ConfluencePublisherListener confluencePublisherListener;
    private final String versionMessage;

    public ConfluencePublisher(final ConfluencePublisherMetadata metadata,
                               final PublishingStrategy publishingStrategy,
                               final ConfluenceClient confluenceClient) {
        this(metadata, publishingStrategy, confluenceClient, null, null);
    }

    public ConfluencePublisher(final ConfluencePublisherMetadata metadata,
                               final PublishingStrategy publishingStrategy,
                               final ConfluenceClient confluenceClient,
                               final ConfluencePublisherListener confluencePublisherListener,
                               final String versionMessage) {
        this.metadata = assertNotNull(metadata, "metadata");
        this.publishingStrategy = assertNotNull(publishingStrategy, "publishingStrategy");
        this.confluenceClient = assertNotNull(confluenceClient, "confluenceClient");
        this.confluencePublisherListener = confluencePublisherListener != null ? confluencePublisherListener : NoOpConfluencePublisherListener.SINGLETON;
        this.versionMessage = versionMessage;
    }

    public ConfluencePublishResult publish() {
        final String spaceKey = this.metadata.getSpaceKey();
        final String ancestorId = this.metadata.getAncestorId();

        assertMandatoryParameter(isNotBlank(spaceKey), "spaceKey");
        assertMandatoryParameter(isNotBlank(ancestorId), "ancestorId");

        final ConfluencePublishResultBuilder resultBuilder = ConfluencePublishResult.builder()
                .defaults()
                .setRootConfluenceUrl(confluenceClient.getConfluenceRootUrl())
                .setSpaceKey(spaceKey)
                .setAncestorId(ancestorId);

        final List<ConfluencePageMetadata> pages = this.metadata.getPages();
        if (this.publishingStrategy.isAppendToAncestor()) {
            startPublishingUnderAncestorId(resultBuilder, pages, spaceKey, ancestorId);
        } else if (this.publishingStrategy.isReplaceAncestor()) {
            startPublishingReplacingAncestorId(resultBuilder, singleRootPage(this.publishingStrategy, this.metadata), spaceKey, ancestorId);
        } else {
            throw new IllegalArgumentException("Invalid publishing strategy '" + this.publishingStrategy + "'");
        }

        this.confluencePublisherListener.publishCompleted();

        return resultBuilder.build();
    }

    private static ConfluencePageMetadata singleRootPage(final PublishingStrategy publishingStrategy, final ConfluencePublisherMetadata metadata) {
        final List<ConfluencePageMetadata> rootPages = metadata.getPages();

        if (rootPages.size() > 1) {
            final String rootPageTitles = rootPages.stream()
                    .map(page -> "'" + page.getTitle() + "'")
                    .collect(joining(", "));
            throw new IllegalArgumentException("Multiple root pages detected: " + rootPageTitles + ", but '" + publishingStrategy + "' publishing strategy only supports one single root page");
        }

        return rootPages.size() == 1 ? rootPages.get(0) : null;
    }

    private void startPublishingReplacingAncestorId(final ConfluencePublishResultBuilder resultBuilder,
                                                    final ConfluencePageMetadata rootPage,
                                                    final String spaceKey,
                                                    final String ancestorId) {
        if (rootPage != null) {
            updatePage(ancestorId, null, rootPage);
            resultBuilder.addPage(spaceKey, ancestorId, rootPage, ancestorId);

            deleteConfluenceAttachmentsNotPresentUnderPage(ancestorId, rootPage.getAttachments());
            addAttachments(ancestorId, rootPage.getAttachments());

            startPublishingUnderAncestorId(resultBuilder, rootPage.getChildren(), spaceKey, ancestorId);
        }
    }

    private void startPublishingUnderAncestorId(final ConfluencePublishResultBuilder resultBuilder, List<ConfluencePageMetadata> pages, String spaceKey, String ancestorId) {
        if (this.publishingStrategy.isDeleteExistingChildren()) {
            deleteConfluencePagesNotPresentUnderAncestor(pages, ancestorId);
        }
        pages.forEach(page -> {
            String contentId = addOrUpdatePageUnderAncestor(spaceKey, ancestorId, page);
            resultBuilder.addPage(spaceKey, ancestorId, page, contentId);

            final Map<String, String> attachments = page.getAttachments();
            deleteConfluenceAttachmentsNotPresentUnderPage(contentId, attachments);
            addAttachments(contentId, attachments);

            startPublishingUnderAncestorId(resultBuilder, page.getChildren(), spaceKey, contentId);
        });
    }

    private void deleteConfluencePagesNotPresentUnderAncestor(List<ConfluencePageMetadata> pagesToKeep, String ancestorId) {
        final List<ConfluencePage> childPagesOnConfluence = this.confluenceClient.getChildPages(ancestorId);

        final List<ConfluencePage> childPagesOnConfluenceToDelete = childPagesOnConfluence.stream()
                .filter(childPageOnConfluence -> pagesToKeep.stream().noneMatch(page -> page.getTitle().equals(childPageOnConfluence.getTitle())))
                .collect(toList());

        childPagesOnConfluenceToDelete.forEach(pageToDelete -> {
            final List<ConfluencePage> pageScheduledForDeletionChildPagesOnConfluence = this.confluenceClient.getChildPages(pageToDelete.getContentId());
            pageScheduledForDeletionChildPagesOnConfluence.forEach(parentPageToDelete ->
                    this.deleteConfluencePagesNotPresentUnderAncestor(emptyList(), pageToDelete.getContentId()));
            this.confluenceClient.deletePage(pageToDelete.getContentId());
            this.confluencePublisherListener.pageDeleted(pageToDelete);
        });
    }

    private void deleteConfluenceAttachmentsNotPresentUnderPage(String contentId, Map<String, String> attachments) {
        final List<ConfluenceAttachment> confluenceAttachments = this.confluenceClient.getAttachments(contentId);

        confluenceAttachments.stream()
                .filter(confluenceAttachment -> attachments.keySet().stream().noneMatch(attachmentFileName -> attachmentFileName.equals(confluenceAttachment.getTitle())))
                .forEach(confluenceAttachment -> {
                    this.confluenceClient.deletePropertyByKey(contentId, getAttachmentHashKey(confluenceAttachment.getTitle()));
                    this.confluenceClient.deleteAttachment(confluenceAttachment.getId());
                });
    }

    private String addOrUpdatePageUnderAncestor(String spaceKey, String ancestorId, ConfluencePageMetadata page) {
        try {
            final String contentId = this.confluenceClient.getPageByTitle(spaceKey, page.getTitle());
            updatePage(contentId, ancestorId, page);
            return contentId;
        } catch (final NotFoundException e) {
            // fall through
        }

        final String content = IoUtils.fileContent(page.getContentFilePath(), UTF_8);
        final String contentId = this.confluenceClient.addPageUnderAncestor(spaceKey, ancestorId, page.getTitle(), content, this.versionMessage);
        this.confluenceClient.setPropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY, hash(content));
        this.confluencePublisherListener.pageAdded(new ConfluencePage(contentId, page.getTitle(), content, INITIAL_PAGE_VERSION));
        return contentId;
    }

    private void updatePage(String contentId, String ancestorId, ConfluencePageMetadata page) {
        final String content = IoUtils.fileContent(page.getContentFilePath(), UTF_8);
        final ConfluencePage existingPage = this.confluenceClient.getPageWithContentAndVersionById(contentId);
        final String existingContentHash = this.confluenceClient.getPropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY);
        final String newContentHash = hash(content);

        if (notSameHash(existingContentHash, newContentHash) || !existingPage.getTitle().equals(page.getTitle())) {
            this.confluenceClient.deletePropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY);
            int newPageVersion = existingPage.getVersion() + 1;
            this.confluenceClient.updatePage(contentId, ancestorId, page.getTitle(), content, newPageVersion, this.versionMessage);
            this.confluenceClient.setPropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY, newContentHash);
            this.confluencePublisherListener.pageUpdated(existingPage, new ConfluencePage(contentId, page.getTitle(), content, newPageVersion));
        }
    }

    private void addAttachments(String contentId, Map<String, String> attachments) {
        attachments.forEach((attachmentFileName, attachmentPath) -> addOrUpdateAttachment(contentId, attachmentPath, attachmentFileName));
    }

    private void addOrUpdateAttachment(String contentId, String attachmentPath, String attachmentFileName) {
        final Path absoluteAttachmentPath = absoluteAttachmentPath(attachmentPath);
        final String newAttachmentHash = hash(fileInputStream(absoluteAttachmentPath));

        try {
            final ConfluenceAttachment existingAttachment = this.confluenceClient.getAttachmentByFileName(contentId, attachmentFileName);
            final String attachmentId = existingAttachment.getId();
            final String existingAttachmentHash = this.confluenceClient.getPropertyByKey(contentId, getAttachmentHashKey(attachmentFileName));

            if (notSameHash(existingAttachmentHash, newAttachmentHash)) {
                if (existingAttachmentHash != null) {
                    this.confluenceClient.deletePropertyByKey(contentId, getAttachmentHashKey(attachmentFileName));
                }
                this.confluenceClient.updateAttachmentContent(contentId, attachmentId, fileInputStream(absoluteAttachmentPath));
                this.confluenceClient.setPropertyByKey(contentId, getAttachmentHashKey(attachmentFileName), newAttachmentHash);
            }
        } catch (final NotFoundException e) {
            this.confluenceClient.deletePropertyByKey(contentId, getAttachmentHashKey(attachmentFileName));
            this.confluenceClient.addAttachment(contentId, attachmentFileName, fileInputStream(absoluteAttachmentPath));
            this.confluenceClient.setPropertyByKey(contentId, getAttachmentHashKey(attachmentFileName), newAttachmentHash);
        }
    }

    private String getAttachmentHashKey(String attachmentFileName) {
        return attachmentFileName + "-hash";
    }

    private Path absoluteAttachmentPath(String attachmentPath) {
        return Paths.get(attachmentPath);
    }

    private static boolean notSameHash(String actualHash, String newHash) {
        return actualHash == null || !actualHash.equals(newHash);
    }

    private static String hash(String content) {
        return sha256Hex(content);
    }

    private static String hash(InputStream content) {
        try {
            return sha256Hex(content);
        } catch (final IOException e) {
            throw new RuntimeException("Could not compute hash from input stream", e);
        } finally {
            closeQuietly(content);
        }
    }

    private static FileInputStream fileInputStream(final Path filePath) {
        try {
            return new FileInputStream(filePath.toFile());
        } catch (final FileNotFoundException e) {
            throw new RuntimeException("Could not find attachment [" + filePath + "]; absolute path is [" + filePath.toAbsolutePath() + "].", e);
        }
    }

    private static class NoOpConfluencePublisherListener implements ConfluencePublisherListener {
        private static NoOpConfluencePublisherListener SINGLETON = new NoOpConfluencePublisherListener();
    }
}
