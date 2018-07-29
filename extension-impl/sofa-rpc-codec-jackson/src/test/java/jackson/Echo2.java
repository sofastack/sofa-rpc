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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
