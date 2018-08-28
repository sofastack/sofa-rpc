package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.config.ProviderConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildProviderPath;

public class ZookeeperStateChange implements ConnectionStateListener {

    private Map<ProviderConfig, List<String>> zkPathsOld;
    private byte[] regContent;
    private String rootPath;

    public ZookeeperStateChange(Map zkPathsOld, String rootPath, byte[] regContent) {
        this.regContent = regContent;
        this.zkPathsOld = zkPathsOld;
        this.rootPath = rootPath;
    }

    private void registry(Map<ProviderConfig, List<String>> zkPaths, CuratorFramework curatorFramework)throws Exception{
        if(zkPaths.size()!=0){

            for(Map.Entry<ProviderConfig, List<String>> paths: zkPaths.entrySet()){
                for(String path :paths.getValue()){
                    String providerPath = buildProviderPath(rootPath, paths.getKey());
                    path = URLEncoder.encode(path, "UTF-8");
                    String providerUrl = providerPath + CONTEXT_SEP + path;
                    curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                            .forPath(providerUrl, regContent);
                }
            }
        }
    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        if (connectionState == ConnectionState.RECONNECTED) {
            while (true) {
                try {
                    if (curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
                        registry(zkPathsOld,curatorFramework);
                        break;
                    }
                } catch (InterruptedException e) {
                    //TODO: log something
                    break;
                } catch (Exception e) {
                    //TODO: log something
                }
            }
        }

    }

}
