package com.alipay.sofa.rpc.codec.fury.model.Registered;

public class HelloServiceImpl2 implements HelloService {

    @Override
    public String sayHello(Person person) {
        return "yes, my name is " + person.getName() + ".  " + person.getAge() + " years old.";
    }
}