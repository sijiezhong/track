package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.domain.User;
import io.github.sijiezhong.track.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器
 * 
 * <p>在应用启动时执行数据初始化操作，例如创建默认管理员账号。
 * 
 * @author sijie
 */
@Component
public class DataInitializer implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final Integer DEFAULT_ADMIN_APP_ID = 1;
    
    private final UserRepository userRepository;
    
    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始执行数据初始化...");
        
        // 初始化管理员账号
        initializeAdminUser();
        
        log.info("数据初始化完成");
    }
    
    /**
     * 初始化管理员账号
     */
    private void initializeAdminUser() {
        // 检查 admin 用户是否已存在
        var existingAdmin = userRepository.findByUsername(DEFAULT_ADMIN_USERNAME);
        
        if (existingAdmin.isPresent()) {
            log.info("管理员账号已存在，跳过初始化: username={}", DEFAULT_ADMIN_USERNAME);
            return;
        }
        
        // 创建默认管理员账号
        User admin = new User();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setPassword(DEFAULT_ADMIN_PASSWORD); // TODO: 生产环境应使用加密密码
        admin.setRealName("系统管理员");
        admin.setIsAnonymous(false);
        admin.setAppId(DEFAULT_ADMIN_APP_ID);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        
        userRepository.save(admin);
        
        log.info("默认管理员账号创建成功: username={}, appId={}", 
            DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_APP_ID);
        log.warn("警告：管理员账号使用默认密码 '{}', 建议在生产环境中修改", DEFAULT_ADMIN_PASSWORD);
    }
}

