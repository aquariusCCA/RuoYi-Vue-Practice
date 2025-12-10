package com.ruoyi.practice;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.SimplePropertyPreFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest
class RuoyiPracticeApplicationTests {

    @Test
    public void test01() throws ParseException {
        User user=new User();
        user.setId(1);
        user.setUserName("admin");
        user.setPassword("123456");
        user.setSex("男");
        user.setAge("28");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        user.setBirthday(format.parse("1997-02-10"));
        String userJSON= JSON.toJSONString(user);
        String userJSON2=JSON.toJSONString(user,new SimplePropertyPreFilter(User.class, "userName","sex"));
        SimplePropertyPreFilter simplePropertyPreFilter = new SimplePropertyPreFilter(User.class, "userName","sex");
        simplePropertyPreFilter.getExcludes().add("sex");//添加排除属性
        simplePropertyPreFilter.getIncludes().add("birthday");//添加属性
        String userJSON3=JSON.toJSONString(user,simplePropertyPreFilter);
        System.out.println("userJSON"+userJSON);
        System.out.println("userJSON2使用了SimplePropertyPreFilter过滤器--->"+userJSON2);
        System.out.println("userJSON3使用了SimplePropertyPreFilter过滤器添加或排除属性--->"+userJSON3);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class User {
    private int id;
    private String userName;
    private String password;
    private String sex;
    private String age;
    private Date birthday;
}
