/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jiaomo.framework.commons;

import java.io.Serializable;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public class ApiResult<T> implements Serializable {
    public static final int SUCCESS_CODE = 0;
    public static final String SUCCESS_MESSAGE = "success";
    public static final ApiResult<Void> SUCCESS = ApiResult.success(null);

    private Boolean success;
    private Integer code;
    private String message;

    private T data;

    public ApiResult() {
    }

    public ApiResult(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public ApiResult(boolean success, int code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    protected ApiResult(T data) {
        this.data = data;
        this.success = Boolean.TRUE;
        this.code = SUCCESS_CODE;
        this.message = SUCCESS_MESSAGE;
    }
    public static ApiResult<Void> success() {
        return SUCCESS;
    }
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(data);
    }

    public static <T> ApiResult<T> failure(int code, String message) {
        return new ApiResult<>(code, message);
    }
    public static <T> ApiResult<T> failure(boolean success, int code, String message) {
        return new ApiResult<>(success, code, message);
    }

    public Boolean isSuccess() {
        return success;
    }
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}
