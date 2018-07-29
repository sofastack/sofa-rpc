package jackson;

/**
 * @author 景竹 2018/7/29
 */
public class Echo {
    private String name;
    private int age;
    private boolean isDel;
    private Echo2 echo2;

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

    public Echo2 getEcho2() {
        return echo2;
    }

    public void setEcho2(Echo2 echo2) {
        this.echo2 = echo2;
    }
}
