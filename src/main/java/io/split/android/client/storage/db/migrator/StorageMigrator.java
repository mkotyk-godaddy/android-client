package io.split.android.client.storage.db.migrator;

import androidx.core.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class StorageMigrator {

    private final SplitRoomDatabase mSqLiteDatabase;
    private MySegmentsMigratorHelper mMySegmentsMigratorHelper;
    private SplitsMigratorHelper mSplitsMigratorHelper;
    private EventsMigratorHelper mEventsMigratorHelper;
    private ImpressionsMigratorHelper mImpressionsMigratorHelper;
    private final GeneralInfoDao mGeneralInfoDao;

    public StorageMigrator(@NotNull SplitRoomDatabase sqLiteDatabase) {
        mSqLiteDatabase = checkNotNull(sqLiteDatabase);
        mGeneralInfoDao = mSqLiteDatabase.generalInfoDao();
    }

    public boolean isMigrationDone() {
        MigrationChecker migrationChecker = new MigrationChecker();
        return migrationChecker.isMigrationDone();
    }

    public void runMigration(@NotNull MySegmentsMigratorHelper mySegmentsMigratorHelper,
                             @NotNull SplitsMigratorHelper splitsMigratorHelper,
                             @NotNull EventsMigratorHelper eventsMigratorHelper,
                             @NotNull ImpressionsMigratorHelper impressionsMigratorHelper) {

        mMySegmentsMigratorHelper = checkNotNull(mySegmentsMigratorHelper);
        mSplitsMigratorHelper = checkNotNull(splitsMigratorHelper);
        mEventsMigratorHelper = checkNotNull(eventsMigratorHelper);
        mImpressionsMigratorHelper = checkNotNull(impressionsMigratorHelper);

        MigrationRunner migrationRunner = new MigrationRunner();
        migrationRunner.runMigration();
    }

    private void migrateMySegments() {
        List<MySegmentEntity> mySegmentEntities = mMySegmentsMigratorHelper.loadLegacySegmentsAsEntities();
        for (MySegmentEntity entity : mySegmentEntities) {
            mSqLiteDatabase.mySegmentDao().update(entity);
        }
    }

    private void migrateSplits() {
        Pair<Long, List<SplitEntity>> splitsSnapshot = mSplitsMigratorHelper.loadLegacySplitsAsEntities();
        mSqLiteDatabase.splitDao().insert(splitsSnapshot.second);
        GeneralInfoEntity changeNumberInfo = new GeneralInfoEntity(
                GeneralInfoEntity.CHANGE_NUMBER_INFO,
                splitsSnapshot.first);
        mSqLiteDatabase.generalInfoDao().update(changeNumberInfo);
    }

    private void migrateEvents() {
        List<EventEntity> eventEntities = mEventsMigratorHelper.loadLegacyEventsAsEntities();
        for (EventEntity entity : eventEntities) {
            mSqLiteDatabase.eventDao().insert(entity);
        }
    }

    private void migrateImpressions() {
        List<ImpressionEntity> impressionEntities = mImpressionsMigratorHelper.loadLegacyImpressionsAsEntities();
        for (ImpressionEntity entity : impressionEntities) {
            mSqLiteDatabase.impressionDao().insert(entity);
        }
    }

    private class MigrationChecker extends Thread {
        private CountDownLatch mLatch = new CountDownLatch(1);
        private AtomicBoolean mResult = new AtomicBoolean(false);

        public void run() {
            final GeneralInfoEntity migrationStatus =
                    mGeneralInfoDao.getByName(GeneralInfoEntity.DATBASE_MIGRATION_STATUS);
            mResult.set(migrationStatus != null);
            mLatch.countDown();
        }

        public boolean isMigrationDone() {
            this.start();
            try {
                mLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return mResult.get();
        }
    }

    private class MigrationRunner extends Thread {

        private CountDownLatch mLatch = new CountDownLatch(1);

        @Override
        public void run() {
            // If migration fails, data is erased and
            // new storage is used anyway to avoid trying to migrate
            // every time sdk is initialized
            mGeneralInfoDao.update(new GeneralInfoEntity(
                    GeneralInfoEntity.DATBASE_MIGRATION_STATUS,
                    GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));

            // Migration data is loaded within the transaction to limit its scope
            // to its own function and that way try to use as less memory as possible
            mSqLiteDatabase.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    try {
                        migrateMySegments();
                        migrateSplits();
                        migrateEvents();
                        migrateImpressions();
                    } catch (Exception e) {
                        Logger.e("Couldn't migrate legacy data. Reason: " +
                                e.getLocalizedMessage());
                    }
                }
            });
            mLatch.countDown();
        }

        public void runMigration() {
            this.start();
            try {
                mLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
