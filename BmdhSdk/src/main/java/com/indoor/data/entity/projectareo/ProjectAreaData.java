package com.indoor.data.entity.projectareo;

/**
 * Created by Aaron on 2021-12-09
 * 请求 区域地图配置数据时使用
 */
public class ProjectAreaData {
    private String 	projectAreaId;
    private int versionNum;

    public ProjectAreaData(String projectAreaId, int versionNum) {
        this.projectAreaId = projectAreaId;
        this.versionNum = versionNum;
    }

    public String getProjectAreaId() {
        return projectAreaId;
    }

    public void setProjectAreaId(String projectAreaId) {
        this.projectAreaId = projectAreaId;
    }

    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }
}
