package com.alipay.sofa.rpc.tripleGeneric;

import java.util.Date;

public class TestEntity {
    private String name;
    private Integer age;
    private Date birth;
    private Inner inner;

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Inner getInner() {
        return inner;
    }

    public void setInner(Inner inner) {
        this.inner = inner;
    }

    public static class Inner{
        private String add;

        public String getAdd() {
            return add;
        }

        public void setAdd(String add) {
            this.add = add;
        }

        @Override
        public String toString() {
            return "Inner{" +
                    "add='" + add + '\'' +
                    '}';
        }
    }
    @Override
    public String toString() {
        return "!!!!TestEntity{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", birth=" + birth +
                ", inner=" + inner +
                '}';
    }
}
