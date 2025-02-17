package tests.storage.migrator.mocks;

import java.util.List;

import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.migrator.MySegmentsMigratorHelper;

public class MySegmentsMigratorHelperMock implements MySegmentsMigratorHelper {
    List<MySegmentEntity> mEntities;

    public void setMySegments(List<MySegmentEntity> entities) {
        mEntities = entities;
    }

    @Override
    public List<MySegmentEntity> loadLegacySegmentsAsEntities() {
        return mEntities;
    }
}
