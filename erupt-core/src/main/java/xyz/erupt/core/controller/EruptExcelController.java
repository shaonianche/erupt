package xyz.erupt.core.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.core.annotation.EruptRouter;
import xyz.erupt.core.bean.EruptApiModel;
import xyz.erupt.core.bean.EruptModel;
import xyz.erupt.core.bean.Page;
import xyz.erupt.core.constant.RestPath;
import xyz.erupt.core.service.CoreService;
import xyz.erupt.core.service.DataFileService;
import xyz.erupt.core.util.HttpUtil;
import xyz.erupt.eruptcommon.util.SpringUtil;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

/**
 * 对Excel数据的处理
 * Created by liyuepeng on 10/15/18.
 */
@RestController
@RequestMapping(RestPath.ERUPT_EXCEL)
public class EruptExcelController {

    @Autowired
    private DataFileService dataFileService;


    @Autowired
    private EruptDataController eruptDataController;

    @Autowired
    private Gson gson;

    @Value("erupt.uploadPath:/opt/file")
    private String uploadPath;

    //模板下载
    @RequestMapping(value = "/template/{erupt}")
    @EruptRouter(verifyMethod = EruptRouter.VerifyMethod.PARAM, authIndex = 2)
    public String getExcelTemplate(@PathVariable("erupt") String eruptName, HttpServletResponse response) {
        EruptModel eruptModel = CoreService.getErupt(eruptName);
        if (eruptModel.getErupt().power().importable()) {
            dataFileService.createExcelTemplate(eruptModel, response);
        } else {
            throw new RuntimeException("没有导入权限");
        }
        return null;
    }

    //导出
    @PostMapping("/export/{erupt}")
    @EruptRouter(verifyMethod = EruptRouter.VerifyMethod.PARAM, authIndex = 2)
    public void exportData(@PathVariable("erupt") String eruptName, @Param("condition") String condition,
                           HttpServletResponse response) throws IOException {
        EruptModel eruptModel = CoreService.getErupt(eruptName);
        if (eruptModel.getErupt().power().export()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(Page.PAGE_INDEX_STR, 1);
            jsonObject.addProperty(Page.PAGE_SIZE_STR, Page.PAGE_MAX_DATA);
            jsonObject.addProperty(Page.PAGE_SORT_STR, "");
            JsonObject jsonCondition = new JsonParser().parse(URLDecoder.decode(condition, "utf-8")).getAsJsonObject();
            for (String key : jsonCondition.keySet()) {
                jsonObject.add(key, jsonCondition.get(key));
            }
            Page page = eruptDataController.getEruptData(eruptName, jsonObject);
            Workbook wb = dataFileService.exportExcel(eruptModel, page);
            for (Class<? extends DataProxy> proxy : eruptModel.getErupt().dateProxy()) {
                SpringUtil.getBean(proxy).excelExport(wb);
            }
            wb.write(HttpUtil.downLoadFile(response, eruptModel.getErupt().name() + DataFileService.XLS_FORMAT));
        } else {
            throw new RuntimeException("没有导出权限");
        }
    }

    //导入
    @PostMapping("/import/{erupt}")
    @ResponseBody
    @EruptRouter(authIndex = 2)
    @Transactional(rollbackOn = Exception.class)
    public EruptApiModel importExcel(@PathVariable("erupt") String eruptName, @RequestParam("file") MultipartFile file) {
        EruptModel eruptModel = CoreService.getErupt(eruptName);
        if (eruptModel.getErupt().power().importable()) {
            if (file.isEmpty()) {
                return EruptApiModel.errorApi("上传失败，请选择文件");
            }
            String fileName = file.getOriginalFilename();
            List<JsonObject> list;
            int i = 1;
            try {
                i++;
                if (fileName.endsWith(DataFileService.XLS_FORMAT)) {
                    list = dataFileService.excelToEruptObject(eruptModel, new HSSFWorkbook(file.getInputStream()));
                } else if (fileName.endsWith(DataFileService.XLSX_FORMAT)) {
                    list = dataFileService.excelToEruptObject(eruptModel, new XSSFWorkbook(file.getInputStream()));
                } else {
                    throw new RuntimeException("上传文件必须为Excel");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Excel解析异常" + "，出错行数：" + i);
            }
            i = 1;
            for (JsonObject jo : list) {
                i++;
                EruptApiModel eruptApiModel = eruptDataController.addEruptData(eruptName, jo);
                ;
                if (eruptApiModel.getStatus() == EruptApiModel.Status.ERROR) {
                    throw new RuntimeException("第" + i + "行：" + eruptApiModel.getMessage());
                }
            }
            return EruptApiModel.successApi();
        } else {
            throw new RuntimeException("没有导入权限");
        }
    }


}