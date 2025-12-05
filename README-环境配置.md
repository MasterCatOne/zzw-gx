# 环境配置说明

## 配置文件说明

项目使用Spring Profile来管理不同环境的配置：

- `application.yml` - 通用配置，包含所有环境共用的配置
- `application-dev.yml` - 开发环境配置（**端口8080，给前端访问**）
- `application-debug.yml` - 调试环境配置（**端口8081，用于本地调试**）

**重要：两个环境使用同一个数据库（`zzw_gx`），可以同时运行，互不干扰。**

## 如何切换环境

### 方式1：通过启动参数（推荐）

**开发环境（端口8080，给前端访问）：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 或者
java -jar zzw-gx.jar --spring.profiles.active=dev
```

**调试环境（端口8081，用于本地调试）：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=debug
# 或者
java -jar zzw-gx.jar --spring.profiles.active=debug
```

### 方式2：通过IDE配置

**IntelliJ IDEA：**
1. 运行配置（Run Configuration）
2. 在 "VM options" 或 "Program arguments" 中添加：`--spring.profiles.active=dev`

**Eclipse：**
1. Run Configuration
2. Arguments 标签页
3. Program arguments 中添加：`--spring.profiles.active=dev`

### 方式3：通过环境变量

```bash
export SPRING_PROFILES_ACTIVE=dev
java -jar zzw-gx.jar
```

## 开发环境配置说明

开发环境配置（`application-dev.yml`）包含：

- **数据库连接**：默认连接本地MySQL（localhost:3306）
- **日志级别**：DEBUG级别，便于调试
- **Knife4j文档**：启用API文档，方便前端查看接口

### 修改开发环境数据库地址

如果需要连接远程开发数据库，修改 `application-dev.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-dev-db-server:3306/zzw_gx?...
    username: your_username
    password: your_password
```

## 前端访问接口

### 本地开发

如果后端运行在本地：
- API地址：`http://localhost:8080`
- API文档：`http://localhost:8080/doc.html`

### 开发服务器

如果后端部署到开发服务器：
1. 修改 `application-dev.yml` 中的 `knife4j.openapi.servers` 配置
2. 前端配置API基础地址为开发服务器地址

### CORS跨域配置

项目已配置CORS，允许跨域访问。如果需要限制特定域名，可以修改 `SecurityConfig.java` 中的CORS配置。

## 🚀 同一台电脑运行两个实例（推荐方案）

### 场景：一个给前端访问（8080），一个用于调试（8081）

**两个实例使用同一个数据库（`zzw_gx`），可以同时运行，互不干扰。**

### 方式1：使用Maven命令（推荐）

**终端1 - 启动开发环境（给前端访问）：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- 端口：**8080**
- 前端访问：`http://localhost:8080`
- API文档：`http://localhost:8080/doc.html`

**终端2 - 启动调试环境（自己调试）：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=debug
```
- 端口：**8081**
- 调试访问：`http://localhost:8081`
- API文档：`http://localhost:8081/doc.html`

### 方式2：使用JAR包

**终端1 - 启动开发环境：**
```bash
java -jar target/zzw-gx.jar --spring.profiles.active=dev
```

**终端2 - 启动调试环境：**
```bash
java -jar target/zzw-gx.jar --spring.profiles.active=debug
```

### 方式3：IDE中同时运行两个实例

**IntelliJ IDEA 详细步骤：**

#### 步骤1：创建第一个配置（Dev Server - 8080）

1. 点击右上角的运行配置下拉框，选择 **"Edit Configurations..."**（或按 `Alt+Shift+F10` 然后选择 `Edit Configurations...`）
2. 点击左上角的 **"+"** 号，选择 **"Spring Boot"**
3. 配置第一个实例：
   - **Name**: `Dev Server (8080)` （给前端访问）
   - **Main class**: 选择你的主类（通常是 `ZzwGxApplication` 或类似名称）
   - **Active profiles**: 填写 `dev` （或者）
   - **VM options**: 填写 `-Dspring.profiles.active=dev` （或者）
   - **Program arguments**: 填写 `--spring.profiles.active=dev` （三选一即可，推荐使用 Active profiles）
4. 点击 **"Apply"** 保存

#### 步骤2：创建第二个配置（Debug Server - 8081）

1. 在同一个配置窗口中，再次点击左上角的 **"+"** 号，选择 **"Spring Boot"**
2. 配置第二个实例：
   - **Name**: `Debug Server (8081)` （自己调试用）
   - **Main class**: 选择你的主类（与第一个配置相同）
   - **Active profiles**: 填写 `debug` （或者）
   - **VM options**: 填写 `-Dspring.profiles.active=debug` （或者）
   - **Program arguments**: 填写 `--spring.profiles.active=debug` （三选一即可，推荐使用 Active profiles）
3. 点击 **"OK"** 保存并关闭

#### 步骤3：运行两个配置

**方法A：分别运行**
1. 点击右上角运行配置下拉框，选择 **"Dev Server (8080)"**，点击运行按钮（绿色三角形）
2. 等待第一个实例启动完成后，再次点击运行配置下拉框，选择 **"Debug Server (8081)"**，点击运行按钮
3. 现在两个实例都在运行了！

**方法B：使用运行配置组合（推荐）**
1. 点击运行配置下拉框，选择 **"Edit Configurations..."**
2. 点击左上角的 **"+"** 号，选择 **"Compound"**
3. 配置组合：
   - **Name**: `Run Both Servers`
   - 在右侧勾选 **"Dev Server (8080)"** 和 **"Debug Server (8081)"**
4. 点击 **"OK"** 保存
5. 选择 **"Run Both Servers"** 配置，点击运行，两个实例会同时启动！

**提示：**
- 推荐使用 **Active profiles** 字段，这是 Spring Boot 最标准的配置方式
- 如果找不到 Active profiles 字段，可以使用 **VM options** 或 **Program arguments**
- 两个配置可以同时运行，互不干扰
- 停止时，点击控制台左侧的停止按钮，或者使用 `Ctrl+F2` 停止当前运行的程序

**Eclipse：**
1. 创建两个运行配置：
   - **配置1**：Name = "Dev Server (8080)"
     - Arguments -> Program arguments: `--spring.profiles.active=dev`
   - **配置2**：Name = "Debug Server (8081)"
     - Arguments -> Program arguments: `--spring.profiles.active=debug`
2. 分别运行两个配置即可

### 注意事项

- ✅ 两个实例使用**同一个数据库**（`zzw_gx`），数据会共享
- ✅ 两个实例可以**同时运行**，互不干扰
- ✅ 前端访问8080端口，你调试时访问8081端口
- ⚠️ 如果担心数据冲突，可以创建两个数据库：`zzw_gx` 和 `zzw_gx_debug`，然后修改 `application-debug.yml` 中的数据库名称

## 常见问题

### 1. 如何让前端在开发时也能访问接口？

**方案A：保持后端服务运行**
- 在开发服务器上部署后端服务
- 前端配置API地址指向开发服务器

**方案B：使用Knife4j Mock功能**
- 在Knife4j文档中可以测试接口
- 可以导出OpenAPI规范给前端使用Mock工具

**方案C：本地运行**
- 后端在本地运行
- 前端配置代理或直接访问 `http://localhost:8080`

### 2. 如何查看当前使用的环境？

启动日志中会显示：
```
The following profiles are active: dev
```

### 3. 如何临时修改配置？

可以通过启动参数覆盖配置：
```bash
java -jar zzw-gx.jar --spring.profiles.active=dev --server.port=9090
```

### 4. 如何修改数据库配置？

如果需要修改数据库连接信息，编辑对应的配置文件：

**开发环境（8080）：**
- 编辑 `application-dev.yml` 中的数据库配置

**调试环境（8081）：**
- 编辑 `application-debug.yml` 中的数据库配置

**示例：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zzw_gx?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

### 5. 如何修改端口？

如果需要修改端口，编辑对应的配置文件：

**开发环境（8080）：**
- 编辑 `application-dev.yml` 中的 `server.port`

**调试环境（8081）：**
- 编辑 `application-debug.yml` 中的 `server.port`

