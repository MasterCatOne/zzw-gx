package com.zzw.zzwgx.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zzw.zzwgx.dto.deserializer.SingleOrListDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员创建用户请求DTO
 */
@Data
@Schema(description = "管理员创建用户账号请求参数")
public class CreateUserRequest {
    
    @Schema(description = "用户账号，长度3-50个字符，必须唯一", requiredMode = Schema.RequiredMode.REQUIRED, example = "worker05")
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度必须在3-50个字符之间")
    private String username;
    
    @Schema(description = "用户密码，长度6-20个字符", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;
    
    @Schema(description = "真实姓名，最大长度50个字符", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;
    
    @Schema(description = "身份证号码，18位身份证号格式", example = "110101199001011234")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", 
            message = "身份证号格式不正确")
    private String idCard;
    
    @Schema(description = "手机号码，11位手机号格式", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Schema(description = "用户角色：WORKER（施工人员）/ADMIN（管理员），默认为WORKER", example = "WORKER")
    private String roleCode;
    
    @Schema(description = "用户状态：0-禁用，1-启用，默认为1（启用）", example = "1")
    private Integer status;
    
    @Schema(description = "工点ID（可选，创建用户时可绑定工点）。可以传单个值如 1，也可以传数组如 [1, 2]", example = "1")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<Long> siteIds;
    
    @Schema(description = "隧道ID（可选，创建用户时可绑定隧道）。可以传单个值如 3，也可以传数组如 [3, 4]", example = "3")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<Long> tunnelIds;
}

