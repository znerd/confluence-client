package org.znerd.confluence.client;

import org.znerd.confluence.client.http.ConfluencePage;

public interface ConfluencePublisherListener {

    void pageAdded(ConfluencePage addedPage);

    void pageUpdated(ConfluencePage existingPage, ConfluencePage updatedPage);

    void pageDeleted(ConfluencePage deletedPage);

    void publishCompleted();
}
