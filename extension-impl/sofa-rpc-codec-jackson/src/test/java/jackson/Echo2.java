package jackson;

/**
 * @author 景竹 2018/7/29
 */
public class Echo2 {

    public Echo2(String name) {
        this.name = name;
    }
    public Echo2() {
        this.name = name;
    }

    private String name;
    private int age;
    private boolean isDel;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isDel() {
        return isDel;
    }

    public void setDel(boolean del) {
        isDel = del;
    }
}
