package com.zzw.zzwgx.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zzw.zzwgx.dto.deserializer.SingleOrListDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员更新用户请求DTO
 */
@Data
@Schema(description = "管理员更新用户账号请求参数")
public class UpdateUserRequest {
    
    @Schema(description = "真实姓名，最大长度50个字符", example = "张三")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;
    
    @Schema(description = "身份证号码，18位身份证号格式", example = "110101199001011234")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", 
            message = "身份证号格式不正确")
    private String idCard;
    
    @Schema(description = "手机号码，11位手机号格式", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Schema(description = "用户密码，长度6-20个字符（可选，如果提供则更新密码）", example = "123456")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;
    
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer status;
    
    @Schema(description = "用户角色：WORKER（施工人员）/ADMIN（管理员）", example = "WORKER")
    private String roleCode;
    
    @Schema(description = "工点ID（可选，更新用户时可修改绑定的工点）。可以传单个值如 1，也可以传数组如 [1, 2]。如果提供此字段，将替换用户原有的工点绑定", example = "1")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<Long> siteIds;
    
    @Schema(description = "隧道ID（可选，更新用户时可修改绑定的隧道）。可以传单个值如 3，也可以传数组如 [3, 4]。如果提供此字段，将替换用户原有的隧道绑定", example = "3")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<Long> tunnelIds;
}

