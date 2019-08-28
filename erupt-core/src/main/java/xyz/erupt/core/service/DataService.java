package xyz.erupt.core.service;

import com.google.gson.JsonObject;
import xyz.erupt.core.bean.EruptModel;
import xyz.erupt.core.bean.Page;
import xyz.erupt.core.bean.TreeModel;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by liyuepeng on 10/10/18.
 */
public interface DataService {

    Object findDataById(EruptModel eruptModel, Serializable id);

    Page queryList(EruptModel eruptModel, Page page, JsonObject searchCondition, String customerCondition);

    Collection<TreeModel> queryTree(EruptModel eruptModel);

    void addData(EruptModel eruptModel, Object object);

    void editData(EruptModel eruptModel, Object object);

    void deleteData(EruptModel eruptModel, Serializable id);

    Collection<TreeModel> findTabTree(EruptModel eruptModel, String fieldName);

    Collection<TreeModel> getReferenceTree(EruptModel eruptModel, String fieldName, Serializable dependValue);

}