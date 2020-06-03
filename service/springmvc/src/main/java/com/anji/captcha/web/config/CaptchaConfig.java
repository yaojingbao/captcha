package com.anji.captcha.web.config;

import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.ImageUtils;
import com.anji.captcha.util.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Base64Utils;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class CaptchaConfig {

    @Bean(name = "AjCaptchaCacheService")
    public CaptchaCacheService captchaCacheService(){
        //缓存类型redis/local/....
        return CaptchaServiceFactory.getCache("local");
    }

    @Bean
    @DependsOn("AjCaptchaCacheService")
    public CaptchaService captchaService(){
        Properties config = new Properties();
        try {
            try (InputStream input = CaptchaConfig.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                config.load(input);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        //各种参数设置....
        //缓存类型redis/local/....
        config.put("captcha.cacheType", "local");
        config.put("captcha.water.mark", "我是水印");
        config.put("captcha.font.type", "宋体");
        config.put("captcha.type", "default");
        config.put("captcha.captchaOriginalPath.jigsaw", "");
        config.put("captcha.captchaOriginalPath.pic-click", "");
        CaptchaService s = CaptchaServiceFactory.getInstance(config);
        if ((StringUtils.isNotBlank(config.getProperty("captcha.captchaOriginalPath.jigsaw")) && config.getProperty("captcha.captchaOriginalPath.jigsaw").startsWith("classpath:"))
                || (StringUtils.isNotBlank(config.getProperty("captcha.captchaOriginalPath.pic-click")) && config.getProperty("captcha.captchaOriginalPath.pic-click").startsWith("classpath:"))) {
            //自定义resources目录下初始化底图
            config.put("captcha.init.original", "true");
            initializeBaseMap(config.getProperty("captcha.captchaOriginalPath.jigsaw"), config.getProperty("captcha.captchaOriginalPath.pic-click"));
        }
        s.init(config);
        return s;
    }

    private static void initializeBaseMap(String jigsaw, String picClick) {
        ImageUtils.cacheBootImage(getResourcesImagesFile(jigsaw + "/original/*.png"),
                getResourcesImagesFile(jigsaw + "/slidingBlock/*.png"),
                getResourcesImagesFile(picClick + "/*.png"));
    }

    public static Map<String, String> getResourcesImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String string = Base64Utils.encodeToString(bytes);
                String filename = resource.getFilename();
                imgMap.put(filename, string);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgMap;
    }

}