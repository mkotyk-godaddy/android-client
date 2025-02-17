package io.split.android.client.service.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.FilterGrouper;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.storage.SplitStorageContainer;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskFactoryImpl implements SplitTaskFactory {

    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final String mUserKey;
    private final SplitsSyncHelper mSplitsSyncHelper;
    private final String mSplitsFilterQueryString;

    public SplitTaskFactoryImpl(@NonNull SplitClientConfig splitClientConfig,
                                @NonNull SplitApiFacade splitApiFacade,
                                @NonNull SplitStorageContainer splitStorageContainer,
                                @NonNull String userKey,
                                @Nullable String splistFilterQueryString) {

        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mUserKey = checkNotNull(userKey);
        mSplitsFilterQueryString = splistFilterQueryString;
        mSplitsSyncHelper = new SplitsSyncHelper(mSplitApiFacade.getSplitFetcher(),
                mSplitsStorageContainer.getSplitsStorage(),
                new SplitChangeProcessor());
    }

    @Override
    public EventsRecorderTask createEventsRecorderTask() {
        return new EventsRecorderTask(
                mSplitApiFacade.getEventsRecorder(),
                mSplitsStorageContainer.getEventsStorage(),
                new EventsRecorderTaskConfig(mSplitClientConfig.eventsPerPush()));
    }

    @Override
    public ImpressionsRecorderTask createImpressionsRecorderTask() {
        return new ImpressionsRecorderTask(
                mSplitApiFacade.getImpressionsRecorder(),
                mSplitsStorageContainer.getImpressionsStorage(),
                new ImpressionsRecorderTaskConfig(
                        mSplitClientConfig.impressionsPerPush(),
                        ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES));
    }

    @Override
    public SplitsSyncTask createSplitsSyncTask(boolean checkCacheExpiration) {

        return new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorageContainer.getSplitsStorage(), checkCacheExpiration,
                mSplitClientConfig.cacheExpirationInSeconds(), mSplitsFilterQueryString);
    }

    @Override
    public MySegmentsSyncTask createMySegmentsSyncTask() {
        return new MySegmentsSyncTask(
                mSplitApiFacade.getMySegmentsFetcher(),
                mSplitsStorageContainer.getMySegmentsStorage());
    }

    @Override
    public LoadMySegmentsTask createLoadMySegmentsTask() {
        return new LoadMySegmentsTask(mSplitsStorageContainer.getMySegmentsStorage());
    }

    @Override
    public LoadSplitsTask createLoadSplitsTask() {
        return new LoadSplitsTask(mSplitsStorageContainer.getSplitsStorage());
    }

    @Override
    public SplitKillTask createSplitKillTask(Split split) {
        return new SplitKillTask(mSplitsStorageContainer.getSplitsStorage(), split);
    }

    @Override
    public MySegmentsUpdateTask createMySegmentsUpdateTask(List<String> segments) {
        return new MySegmentsUpdateTask(mSplitsStorageContainer.getMySegmentsStorage(), segments);
    }

    @Override
    public SplitsUpdateTask createSplitsUpdateTask(long since) {
        return new SplitsUpdateTask(mSplitsSyncHelper, mSplitsStorageContainer.getSplitsStorage(), since);
    }

    @Override
    public FilterSplitsInCacheTask createFilterSplitsInCacheTask() {
        List<SplitFilter> filters = new FilterGrouper().group(mSplitClientConfig.syncConfig().getFilters());
        return new FilterSplitsInCacheTask(mSplitsStorageContainer.getPersistentSplitsStorage(),
                filters, mSplitsFilterQueryString);
    }
}
