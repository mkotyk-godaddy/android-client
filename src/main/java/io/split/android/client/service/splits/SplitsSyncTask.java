package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.sleep;

public class SplitsSyncTask implements SplitTask {

    static final String SINCE_PARAM = "since";
    private final String mSplitsFilterQueryString;

    private final SplitsStorage mSplitsStorage;
    private final boolean mCheckCacheExpiration;
    private final long mCacheExpirationInSeconds;
    private final SplitsSyncHelper mSplitsSyncHelper;

    public SplitsSyncTask(SplitsSyncHelper splitsSyncHelper,
                          SplitsStorage splitsStorage,
                          boolean checkCacheExpiration,
                          long cacheExpirationInSeconds,
                          String splitsFilterQueryString) {

        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
        mCacheExpirationInSeconds = cacheExpirationInSeconds;
        mCheckCacheExpiration = checkCacheExpiration;
        mSplitsFilterQueryString = splitsFilterQueryString;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        long storedChangeNumber = mSplitsStorage.getTill();
        long updateTimestamp = mSplitsStorage.getUpdateTimestamp();
        String storedSplitsFilterQueryString = mSplitsStorage.getSplitsFilterQueryString();

        boolean shouldClearExpiredCache = mCheckCacheExpiration &&
                mSplitsSyncHelper.cacheHasExpired(storedChangeNumber, updateTimestamp, mCacheExpirationInSeconds);

        boolean splitsFilterHasChanged = splitsFilterHasChanged(storedSplitsFilterQueryString);

        if(splitsFilterHasChanged) {
            mSplitsStorage.updateSplitsFilterQueryString(mSplitsFilterQueryString);
            storedChangeNumber = -1;
        }

        Map<String, Object> params = new HashMap<>();
        params.put(SINCE_PARAM, storedChangeNumber);
        return mSplitsSyncHelper.sync(params, splitsFilterHasChanged || shouldClearExpiredCache);
    }

    private boolean splitsFilterHasChanged(String storedSplitsFilterQueryString) {
        return !sanitizeString(mSplitsFilterQueryString).equals(sanitizeString(storedSplitsFilterQueryString));
    }

    private String sanitizeString(String string) {
        return string != null ? string : "";
    }
}
