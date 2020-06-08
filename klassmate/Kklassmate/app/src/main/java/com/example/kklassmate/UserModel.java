package com.example.kklassmate;

// 사용자 정보 객체
public class UserModel {
    String Email;
    String Student_code;
    String Department;

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getStudent_code() {
        return Student_code;
    }

    public void setStudent_code(String student_code) {
        Student_code = student_code;
    }

    public String getDepartment() {
        return Department;
    }

    public void setDepartment(String department) {
        Department = department;
    }
}
