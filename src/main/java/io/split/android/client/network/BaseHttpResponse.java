package io.split.android.client.network;

public interface BaseHttpResponse {
    boolean isSuccess();

    boolean isCredentialsError();

    boolean isBadRequestError();

    boolean isClientRelatedError();

    int getHttpStatus();
}
