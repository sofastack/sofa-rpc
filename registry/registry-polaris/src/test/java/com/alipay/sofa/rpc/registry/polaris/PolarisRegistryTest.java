package com.alipay.sofa.rpc.registry.polaris;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.stream.IntStream;

public class PolarisRegistryTest {



    private NamingServer polaris;
    private RegistryConfig registryConfig;

    private PolarisRegistry registry;

    @Before
    public void setup() {
        polaris = new NamingServer(8091);
        polaris.getNamingService().addService(new ServiceKey(NAMESPACE, SERVICE));
        try {
            polaris.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        registryConfig = new RegistryConfig()
                .setProtocol("polaris")
                .setAddress("127.0.0.1:8091")
                .setRegister(true);

        registry = (PolarisRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
    }

    @After
    public void tearDown() {
        registry.destroy();
        polaris.terminate();
    }
    private static final String NAMESPACE = "polaris-test";
    private static final String SERVICE = "com.alipay.sofa.rpc.registry.polaris.TestService";
    @Test
    public void testRegister() {
        ProviderConfig<?> providerConfig = providerConfig("test-registry", 12200, 12201, 12202);
        registry.register(providerConfig);

        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPI();

        GetAllInstancesRequest request=new GetAllInstancesRequest();
        request.setNamespace(NAMESPACE);
        request.setService(SERVICE);
        InstancesResponse allInstance = consumerAPI.getAllInstance(request);
        Assert.assertEquals(3, allInstance.getInstances().length);

        providerConfig = providerConfig("test-registry", 12200, 12201);
        registry.unRegister(providerConfig);
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        allInstance = consumerAPI.getAllInstance(request);
        Assert.assertEquals(0, allInstance.getInstances().length);
    }

    private ProviderConfig<?> providerConfig(String uniqueId, int... ports) {
        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId(SERVICE)
                .setUniqueId(uniqueId)
                .setApplication(new ApplicationConfig().setAppName(NAMESPACE))
                .setProxy("javassist")
                .setRegister(true)
                .setRegistry(registryConfig)
                .setSerialization("hessian2")
                .setWeight(222)
                .setTimeout(3000);

        IntStream.of(ports)
                .mapToObj(port ->
                        new ServerConfig()
                                .setProtocol("bolt")
                                .setHost("127.0.0.1")
                                .setPort(port)
                ).forEach(provider::setServer);
        return provider;
    }
}
