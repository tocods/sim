package org.sim.controller;



public class ResultDTO {
    public static int SUCCESS_CODE=200;
    public static int ERROR_CODE=500;
    public static int FORBIDDEN_CODE=403;
    int code;
    Object data;
    String message;

    public static ResultDTO success(Object data) {
        ResultDTO d = new ResultDTO();
        d.data = data;
        d.code = SUCCESS_CODE;
        d.message = "success";
        return d;
    }

    public static ResultDTO error(String errMsg) {
        ResultDTO d = new ResultDTO();
        d.code = ERROR_CODE;
        d.message = errMsg;
        return d;
    }


    public void setData(Object d) {
        this.data = d;
    }

    public boolean ifSuccess() {
        return this.code == SUCCESS_CODE;
    }

    public boolean ifError() {
        return this.code == ERROR_CODE;
    }

}
