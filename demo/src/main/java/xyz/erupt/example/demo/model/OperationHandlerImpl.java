package xyz.erupt.example.demo.model;

import com.google.gson.JsonObject;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.annotation.model.BoolAndReason;

/**
 * Created by liyuepeng on 10/10/18.
 */
public class OperationHandlerImpl implements OperationHandler {

    @Override
    public BoolAndReason exec(Object data, JsonObject param) {
        return new BoolAndReason(true, "2333");
    }


//    public static void main(String[] args) {
//        new ProjectUtil().projectStartLoaded("demo", (bool) -> {
//            if (bool) {
//                System.out.println("first ");
//            } else {
//                System.out.println("不是第一次调用");
//            }
//        });
//    }
}