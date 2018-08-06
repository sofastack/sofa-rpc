package jackson;

import com.alipay.sofa.rpc.codec.jackson.JacksonSerializerHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * JacksonSerializerHelper Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>Aug 6, 2018</pre>
 */
public class JacksonSerializerHelperTest {
    JacksonSerializerHelper jacksonSerializerHelper = new JacksonSerializerHelper();

    @Test
    public void getReqClass() {
        Class[] req = jacksonSerializerHelper.getReqClass(
                "jackson.EchoService", "checkOut");
        Assert.assertTrue(req.length == 2);
        Assert.assertTrue(req[0] == Echo.class);
    }

    @Test
    public void getResClass() {
        Class res = jacksonSerializerHelper.getResClass(
                "jackson.EchoService", "checkOut");
        Assert.assertTrue(res == Echo2.class);
    }



} 
