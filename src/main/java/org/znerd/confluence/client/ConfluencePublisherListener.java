package org.znerd.confluence.client;

import org.znerd.confluence.client.http.ConfluencePage;

public interface ConfluencePublisherListener {
    default void pageAdded(ConfluencePage addedPage) {
        // empty
    }

    default void pageUpdated(ConfluencePage existingPage, ConfluencePage updatedPage) {
        // empty
    }

    default void pageDeleted(ConfluencePage deletedPage) {
        // empty
    }

    default void publishCompleted() {
        // empty
    }
}
