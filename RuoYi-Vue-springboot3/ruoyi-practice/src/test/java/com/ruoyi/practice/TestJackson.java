package com.ruoyi.practice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.practice.pojo.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class TestJackson {
    @Autowired
    private ObjectMapper mapper;

    @Test
    public void test() throws JsonProcessingException {
        Student student = getStudent();
        String stuStr = mapper.writeValueAsString(student);
        System.out.println(stuStr);
    }

    public String getStudentListStr() throws JsonProcessingException {
        List<Student> students = List.of(
                new Student("1", "kevin", 28),
                new Student("2", "jack", 27),
                new Student("3", "brain", 26)
        );

        return mapper.writeValueAsString(students);
    }

    public String getStudentString() throws JsonProcessingException {
        Student student = new Student("1", "kevin", 28);
        return mapper.writeValueAsString(student);
    }

    public Student getStudent(){
        return new Student("1", "kevin", 28);
    }

    public List<Student> getStudentList(){
        return List.of(
                new Student("1", "kevin", 28),
                new Student("2", "jack", 27),
                new Student("3", "brain", 26)
        );
    }
}