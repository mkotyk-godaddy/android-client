package io.split.android.client.cache;

import java.util.List;

import io.split.android.client.dtos.MySegment;

/**
 * Created by guillermo on 11/23/17.
 */

@Deprecated
public interface IMySegmentsCache {

    /**
     * Sets the list of MySegments in cache for a key
     * @param key The key for the list to be saved
     * @param mySegments List of MySegments to cache
     */
    void setMySegments(String key, List<MySegment> mySegments);

    /**
     * Gets MySegments from the cache
     * @param key The key corresponding to the cached segments
     * @return The cached list of MySegments
     */
    List<MySegment> getMySegments(String key);

    /**
     * Deletes the list of MySegments from the cache
     */
    void deleteMySegments(String key);

    /**
     * Saves in memory cash to disk
     */
     void saveToDisk();

}
