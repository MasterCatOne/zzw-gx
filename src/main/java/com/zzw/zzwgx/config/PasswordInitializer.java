//package com.zzw.zzwgx.config;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.zzw.zzwgx.entity.User;
//import com.zzw.zzwgx.mapper.UserMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
///**
// * 密码初始化器
// * 在应用启动时自动重置所有用户的密码为123456，确保测试/演示环境账号可用
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PasswordInitializer implements CommandLineRunner {
//
//    private final UserMapper userMapper;
//    private final PasswordEncoder passwordEncoder;
//
//    @Value("${app.init.reset-password:true}")
//    private boolean resetPassword;
//
//    @Override
//    public void run(String... args) {
//        if (!resetPassword) {
//            log.info("跳过用户密码初始化（app.init.reset-password=false）");
//            return;
//        }
//
//        log.info("开始初始化用户密码...");
//        String defaultPassword = "123456";
//        String encodedPassword = passwordEncoder.encode(defaultPassword);
//
//        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
//        if (users.isEmpty()) {
//            log.warn("未找到任何用户，跳过密码初始化");
//            return;
//        }
//
//        int count = 0;
//        for (User user : users) {
//            user.setPassword(encodedPassword);
//            userMapper.updateById(user);
//            count++;
//            log.debug("用户密码已重置，用户名: {}, 用户ID: {}", user.getUsername(), user.getId());
//        }
//
//        log.info("用户密码初始化完成，共重置 {} 个用户的密码为: {}", count, defaultPassword);
//    }
//}
//
