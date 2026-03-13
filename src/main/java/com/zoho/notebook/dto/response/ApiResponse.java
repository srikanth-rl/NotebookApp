package com.zoho.notebook.dto.response;

public class ApiResponse {

    private String status;
    private String message;
    private Object data;

    public ApiResponse() {}

    public static ApiResponse success(String message) {
        ApiResponse response = new ApiResponse();
        response.status = "success";
        response.message = message;
        return response;
    }

    public static ApiResponse error(String message) {
        ApiResponse response = new ApiResponse();
        response.status = "error";
        response.message = message;
        return response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
    
}