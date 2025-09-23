package uw.common.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * 测试Schema。
 */
public class SchemaTestVo {

    @Schema(title = "编号", description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private long id;

    @Schema(title = "名称", description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(title = "描述", description = "描述")
    private String desc;

    @Schema(title = "价格", description = "价格", requiredMode = Schema.RequiredMode.REQUIRED)
    private double price;

    @Schema(title = "创建时间", description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date createDate;

    @Schema(title = "更新时间", description = "更新时间")
    private Date updateDate;

    @Schema(title = "状态", description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private int status;

    public SchemaTestVo() {
    }

    public SchemaTestVo(long id, String name, String desc, double price, Date createDate, Date updateDate, int status) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
