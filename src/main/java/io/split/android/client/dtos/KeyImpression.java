package io.split.android.client.dtos;


import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.InBytesSizable;
import io.split.android.client.impressions.Impression;
import io.split.android.client.utils.Json;

public class KeyImpression implements InBytesSizable {
    public transient long storageId;
    public String feature;
    public String keyName;
    public String bucketingKey;
    public String treatment;
    public String label;
    public long time;
    public Long changeNumber; // can be null if there is no changeNumber

    public KeyImpression() {
    }

    public KeyImpression(Impression impression) {
        this.feature = impression.split();
        this.keyName = impression.key();
        this.bucketingKey = impression.bucketingKey();
        this.label = impression.appliedRule();
        this.treatment = impression.treatment();
        this.time = impression.time();
        this.changeNumber = impression.changeNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyImpression that = (KeyImpression) o;

        if (time != that.time) return false;
        if (feature != null ? !feature.equals(that.feature) : that.feature != null) return false;
        if (!keyName.equals(that.keyName)) return false;
        if (!treatment.equals(that.treatment)) return false;

        if (bucketingKey == null) {
            return that.bucketingKey == null;
        }

        return bucketingKey.equals(that.bucketingKey);
    }

    @Override
    public long getSizeInBytes() {
        return ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES;
    }

    @Override
    public int hashCode() {
        int result = feature != null ? feature.hashCode() : 0;
        result = 31 * result + keyName.hashCode();
        result = 31 * result + (bucketingKey == null ? 0 : bucketingKey.hashCode());
        result = 31 * result + treatment.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }
}
