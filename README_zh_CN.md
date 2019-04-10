# SOFARPC

[![Build Status](https://travis-ci.org/alipay/sofa-rpc.svg?branch=master)](https://travis-ci.org/alipay/sofa-rpc)
[![Coverage Status](https://codecov.io/gh/alipay/sofa-rpc/branch/master/graph/badge.svg)](https://codecov.io/gh/alipay/sofa-rpc)
![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)
[![Maven](https://img.shields.io/github/release/alipay/sofa-rpc.svg)](https://github.com/alipay/sofa-rpc/releases)

SOFARPC 是一个高可扩展性、高性能、生产级的 Java RPC 框架。在蚂蚁金服 SOFARPC 已经经历了十多年及五代版本的发展。SOFARPC 致力于简化应用之间的 RPC 调用，为应用提供方便透明、稳定高效的点对点远程服务调用方案。为了用户和开发者方便的进行功能扩展，SOFARPC 提供了丰富的模型抽象和可扩展接口，包括过滤器、路由、负载均衡等等。同时围绕 SOFARPC 框架及其周边组件提供丰富的微服务治理方案。

## 功能特性

- 透明化、高性能的远程服务调用
- 支持多种服务路由及负载均衡策略
- 支持多种注册中心的集成
- 支持多种协议，包括 Bolt、Rest、Dubbo 等
- 支持同步、单向、回调、泛化等多种调用方式
- 支持集群容错、服务预热、自动故障隔离
- 强大的扩展功能，可以按需扩展各个功能组件

## 关联项目

- [sofa-rpc-boot-project](https://github.com/alipay/sofa-rpc-boot-projects) SOFABoot 扩展项目，包括 starter 工程及使用示例。

## 需要

编译需要 JDK 8 及以上、Maven 3.2.5 及以上。

运行需求 JDK 8 及以上。

## 文档

- [快速开始](http://www.sofastack.tech/sofa-rpc/docs/Getting-Started-With-SOFA-Boot)
- [用户手册](http://www.sofastack.tech/sofa-rpc/docs/Programming)
- [开发者指南](http://www.sofastack.tech/sofa-rpc/docs/How-To-Build)
- [发布历史](http://www.sofastack.tech/sofa-rpc/docs/ReleaseNotes)
- [发展路线](http://www.sofastack.tech/sofa-rpc/docs/RoadMap)

## 贡献

[如何参与 SOFARPC 代码贡献](http://www.sofastack.tech/sofa-rpc/docs/Contributing)


## 联系我们

- **钉钉群**

  <img src="https://gw.alipayobjects.com/mdn/rms_aefe75/afts/img/A*3UUmQrZkwz0AAAAAAAAAAABjARQnAQ"  height="300" width="300">

## 致谢

SOFARPC 最早源于阿里内部的 HSF，非常感谢毕玄创造了 HSF，使 SOFARPC 的发展有了良好的基础，也非常感谢寒泉子，独明，世范在 SOFARPC 发展过程中作出的贡献，😄。

## 开源许可

SOFARPC 基于 [Apache License 2.0](https://github.com/alipay/sofa-rpc/blob/master/LICENSE) 协议，SOFARPC 依赖了一些三方组件，它们的开源协议参见[依赖组件版权说明](http://www.sofastack.tech/sofa-rpc/docs/NOTICE)。
