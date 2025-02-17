package io.split.android.client.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Filter;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SynchronizerTest {

    Synchronizer mSynchronizer;
    @Mock
    SplitTaskExecutor mTaskExecutor;
    @Mock
    SplitApiFacade mSplitApiFacade;
    @Mock
    SplitStorageContainer mSplitStorageContainer;
    @Mock
    PersistentSplitsStorage mPersistentSplitsStorageContainer;
    @Mock
    PersistentEventsStorage mEventsStorage;
    @Mock
    PersistentImpressionsStorage mImpressionsStorage;
    @Mock
    SplitTaskExecutionListener mTaskExecutionListener;

    @Mock
    RetryBackoffCounterTimerFactory mRetryBackoffFactory;

    @Mock
    RetryBackoffCounterTimer mRetryTimerSplitsSync;

    @Mock
    RetryBackoffCounterTimer mRetryTimerSplitsUpdate;

    @Mock
    RetryBackoffCounterTimer mRetryTimerMySegmentsSync;

    @Mock
    WorkManager mWorkManager;
    @Mock
    SplitTaskFactory mTaskFactory;
    @Mock
    SplitEventsManager mEventsManager;
    @Mock
    WorkManagerWrapper mWorkManagerWrapper;


    public void setup(SplitClientConfig splitClientConfig) {
        MockitoAnnotations.initMocks(this);
        HttpFetcher<SplitChange> splitsFetcher = Mockito.mock(HttpFetcher.class);
        HttpFetcher<List<MySegment>> mySegmentsFetcher = Mockito.mock(HttpFetcher.class);
        HttpRecorder<List<Event>> eventsRecorder = Mockito.mock(HttpRecorder.class);
        HttpRecorder<List<KeyImpression>> impressionsRecorder = Mockito.mock(HttpRecorder.class);

        SplitsStorage splitsStorage = Mockito.mock(SplitsStorage.class);
        MySegmentsStorage mySegmentsStorage = Mockito.mock(MySegmentsStorage.class);

        when(mSplitApiFacade.getSplitFetcher()).thenReturn(splitsFetcher);
        when(mSplitApiFacade.getMySegmentsFetcher()).thenReturn(mySegmentsFetcher);
        when(mSplitApiFacade.getEventsRecorder()).thenReturn(eventsRecorder);
        when(mSplitApiFacade.getImpressionsRecorder()).thenReturn(impressionsRecorder);

        when(mSplitStorageContainer.getSplitsStorage()).thenReturn(splitsStorage);
        when(mSplitStorageContainer.getPersistentSplitsStorage()).thenReturn(mPersistentSplitsStorageContainer);
        when(mSplitStorageContainer.getMySegmentsStorage()).thenReturn(mySegmentsStorage);
        when(mSplitStorageContainer.getEventsStorage()).thenReturn(mEventsStorage);
        when(mSplitStorageContainer.getImpressionsStorage()).thenReturn(mImpressionsStorage);

        when(mTaskFactory.createSplitsSyncTask(anyBoolean())).thenReturn(Mockito.mock(SplitsSyncTask.class));
        when(mTaskFactory.createMySegmentsSyncTask()).thenReturn(Mockito.mock(MySegmentsSyncTask.class));
        when(mTaskFactory.createImpressionsRecorderTask()).thenReturn(Mockito.mock(ImpressionsRecorderTask.class));
        when(mTaskFactory.createEventsRecorderTask()).thenReturn(Mockito.mock(EventsRecorderTask.class));
        when(mTaskFactory.createLoadSplitsTask()).thenReturn(Mockito.mock(LoadSplitsTask.class));
        when(mTaskFactory.createFilterSplitsInCacheTask()).thenReturn(Mockito.mock(FilterSplitsInCacheTask.class));

        when(mWorkManager.getWorkInfoByIdLiveData(any())).thenReturn(mock(LiveData.class));

        when(mRetryBackoffFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerMySegmentsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mSynchronizer = new SynchronizerImpl(splitClientConfig, mTaskExecutor,
                mSplitStorageContainer, mTaskFactory, mEventsManager, mWorkManagerWrapper, mRetryBackoffFactory);
    }

    @Test
    public void splitExecutorSchedule() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicFetching();
        mSynchronizer.startPeriodicRecording();
        verify(mTaskExecutor, times(1)).schedule(
                any(SplitsSyncTask.class), anyLong(), anyLong(),
                any(SplitTaskExecutionListener.class));
        verify(mTaskExecutor, times(1)).schedule(
                any(MySegmentsSyncTask.class), anyLong(), anyLong(),
                any(SplitTaskExecutionListener.class));
        verify(mTaskExecutor, times(1)).schedule(
                any(EventsRecorderTask.class), anyLong(), anyLong(),
                any(SplitTaskExecutionListener.class));
        verify(mTaskExecutor, times(1)).schedule(
                any(ImpressionsRecorderTask.class), anyLong(), anyLong(),
                any(SplitTaskExecutionListener.class));

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.SPLITS_SYNC.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.MY_SEGMENTS_SYNC.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.EVENTS_RECORDER.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.IMPRESSIONS_RECORDER.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManagerWrapper, times(1)).removeWork();
        verify(mWorkManagerWrapper, never()).scheduleWork();
    }

    @Test
    public void workManagerSchedule() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(true)
                .impressionsQueueSize(3)
                .build();
        setup(config);

        verify(mWorkManagerWrapper, times(1)).scheduleWork();
        verify(mWorkManagerWrapper, never()).removeWork();
    }

    @Test
    public void pause() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicFetching();
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pause();
        verify(mTaskExecutor, times(1)).pause();
    }

    @Test
    public void resume() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicFetching();
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pause();
        mSynchronizer.resume();
        verify(mTaskExecutor, times(1)).resume();
    }

    @Test
    public void pushEvent() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        Event event = new Event();
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pushEvent(event);
        Thread.sleep(200);
        verify(mTaskExecutor, times(0)).submit(
                any(EventsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
        verify(mEventsStorage, times(1)).push(event);
    }

    @Test
    public void pushEventReachQueueSize() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 22; i++) {
            mSynchronizer.pushEvent(new Event());
        }
        Thread.sleep(200);
        verify(mEventsStorage, times(22)).push(any(Event.class));
        verify(mTaskExecutor, times(2)).submit(
                any(EventsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @Test
    public void pushEventBytesLimit() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 6; i++) {
            Event event = new Event();
            event.setSizeInBytes(2000000);
            mSynchronizer.pushEvent(event);
        }
        Thread.sleep(200);
        verify(mEventsStorage, times(6)).push(any(Event.class));
        verify(mTaskExecutor, times(2)).submit(
                any(EventsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @Test
    public void pushImpression() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        Impression impression = createImpression();
        ArgumentCaptor<KeyImpression> impressionCaptor = ArgumentCaptor.forClass(KeyImpression.class);
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pushImpression(impression);
        Thread.sleep(200);
        verify(mTaskExecutor, times(0)).submit(
                any(ImpressionsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
        verify(mImpressionsStorage, times(1)).push(impressionCaptor.capture());
        Assert.assertEquals("key", impressionCaptor.getValue().keyName);
        Assert.assertEquals("bkey", impressionCaptor.getValue().bucketingKey);
        Assert.assertEquals("split", impressionCaptor.getValue().feature);
        Assert.assertEquals("on", impressionCaptor.getValue().treatment);
        Assert.assertEquals(100L, impressionCaptor.getValue().time);
        Assert.assertEquals("default rule", impressionCaptor.getValue().label);
        Assert.assertEquals(999, impressionCaptor.getValue().changeNumber.longValue());
    }

    @Test
    public void pushImpressionReachQueueSize() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 8; i++) {
            mSynchronizer.pushImpression(createImpression());
        }
        Thread.sleep(200);
        verify(mImpressionsStorage, times(8)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(
                any(ImpressionsRecorderTask.class),
                any(RecorderSyncHelper.class));
    }

    @Test
    public void pushImpressionBytesLimit() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);

        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 10; i++) {
            mSynchronizer.pushImpression(createImpression());
        }
        Thread.sleep(200);
        verify(mImpressionsStorage, times(10)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(
                any(ImpressionsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @Test
    public void loadLocalData() {
        SplitClientConfig config = SplitClientConfig.builder()
                .sychronizeInBackground(false)
                .build();
        setup(config);

        List<SplitTaskExecutionInfo> list = new ArrayList<>();
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_MY_SYGMENTS));
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));
        SplitTaskExecutor executor = new SplitTaskExecutorSub(list);
        when(mRetryBackoffFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerMySegmentsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mSynchronizer = new SynchronizerImpl(config, executor,
                mSplitStorageContainer, mTaskFactory, mEventsManager, mWorkManagerWrapper, mRetryBackoffFactory);
        mSynchronizer.loadSplitsFromCache();
        mSynchronizer.loadMySegmentsFromCache();
        verify(mEventsManager, times(1))
                .notifyInternalEvent(SplitInternalEvent.MYSEGMENTS_LOADED_FROM_STORAGE);
        verify(mEventsManager, times(1))
                .notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
    }

    @Test
    public void loadAndSynchronizeSplits() {
        SplitClientConfig config = SplitClientConfig.builder()
                .sychronizeInBackground(false)
                .build();
        setup(config);

        List<SplitTaskExecutionInfo> list = new ArrayList<>();
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.FILTER_SPLITS_CACHE));
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        SplitTaskExecutor executor = new SplitTaskExecutorSub(list);
        when(mRetryBackoffFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerMySegmentsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mSynchronizer = new SynchronizerImpl(config, executor,
                mSplitStorageContainer, mTaskFactory, mEventsManager, mWorkManagerWrapper, mRetryBackoffFactory);
        mSynchronizer.loadAndSynchronizeSplits();
        verify(mEventsManager, times(1))
                .notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        verify(mRetryTimerSplitsSync, times(1)).start();
    }

    @Test
    public void stop() {
        SplitClientConfig config = SplitClientConfig.builder()
                .sychronizeInBackground(false)
                .build();
        setup(config);

        mSynchronizer.destroy();

        verify(mRetryTimerSplitsSync, times(1)).stop();
        verify(mRetryTimerMySegmentsSync, times(1)).stop();
        verify(mRetryTimerSplitsUpdate, times(1)).stop();
        verify(mTaskExecutor, times(1)).submit(
                any(ImpressionsRecorderTask.class),
                any(RecorderSyncHelper.class));
        verify(mTaskExecutor, times(1)).submit(
                any(EventsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @After
    public void tearDown() {
    }

    private Impression createImpression() {
        return new Impression("key", "bkey", "split", "on",
                100L, "default rule", 999L, null);
    }

    private KeyImpression keyImpression(Impression impression) {
        KeyImpression result = new KeyImpression();
        result.feature = impression.split();
        result.keyName = impression.key();
        result.bucketingKey = impression.bucketingKey();
        result.label = impression.appliedRule();
        result.treatment = impression.treatment();
        result.time = impression.time();
        result.changeNumber = impression.changeNumber();
        return result;
    }

    static class SplitTaskExecutorSub implements SplitTaskExecutor {

        List<SplitTaskExecutionInfo> mInfoList;
        int mRequestIndex = 0;

        public SplitTaskExecutorSub(List<SplitTaskExecutionInfo> infoList) {
            mInfoList = infoList;
        }

        @Override
        public String schedule(@NonNull SplitTask task, long initialDelayInSecs,
                               long periodInSecs,
                               @Nullable SplitTaskExecutionListener executionListener) {
            return UUID.randomUUID().toString();

        }

        @Override
        public void submit(@NonNull SplitTask task,
                           @Nullable SplitTaskExecutionListener executionListener) {

            SplitTaskExecutionInfo info = mInfoList.get(mRequestIndex);
            mRequestIndex++;
            executionListener.taskExecuted(info);
        }

        @Override
        public void executeSerially(List<SplitTaskBatchItem> tasks) {
            for (SplitTaskBatchItem enqueued : tasks) {
                SplitTaskExecutionInfo info = mInfoList.get(mRequestIndex);
                mRequestIndex++;
                enqueued.getTask().execute();
                SplitTaskExecutionListener executionListener = enqueued.getListener();
                if (executionListener != null) {
                    executionListener.taskExecuted(info);
                }
            }
        }

        @Override
        public void pause() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void stopTask(String taskId) {

        }

        @Override
        public void stop() {

        }

        @Override
        public String schedule(@NonNull SplitTask task, long initialDelayInSecs, @Nullable SplitTaskExecutionListener executionListener) {
            return null;
        }
    }
}
