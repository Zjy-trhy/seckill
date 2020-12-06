package com.zjy.seckill.response;

public class CommonReturnType {

    //表明返回结果的情况，success,fail
    private String status;
    //若success，则data返回前端需要的json数据
    //若fail，则data使用通用的错误码格式
    private Object data;

    //定义一个通用的创建方法，默认状态是success
    public static CommonReturnType create(Object data) {
        return CommonReturnType.create(data, "success");
    }

    public static CommonReturnType create(Object data, String status) {
        CommonReturnType commonReturnType = new CommonReturnType();
        commonReturnType.setData(data);
        commonReturnType.setStatus(status);
        return commonReturnType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
