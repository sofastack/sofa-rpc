# SOFARPC

[![Build Status](https://travis-ci.org/alipay/sofa-rpc.svg?branch=master)](https://travis-ci.org/alipay/sofa-rpc)
[![Coverage Status](https://codecov.io/gh/alipay/sofa-rpc/branch/master/graph/badge.svg)](https://codecov.io/gh/alipay/sofa-rpc)
![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)
[![Maven](https://img.shields.io/github/release/alipay/sofa-rpc.svg)](https://github.com/alipay/sofa-rpc/releases)

## Overview

SOFARPC is a high-performance, high-extensibility, production-level Java RPC framework. In Ant Financial, SOFARPC has been used for more than ten years and developing for five generations. SOFARPC is dedicated to simplify RPC calls between applications, and provide convenient, no code intrusion, stable, and efficient point-to-point remote service invocation solutions for applications. For user and developer easy to improve features, SOFARPC provides a wealth of model abstraction and extensible interfaces, including filter, routing, load balancing, and so on. At the same time, it provides a rich MicroService governance solution around the SOFARPC framework and its surrounding components.

## Features

- No code intrusion, high-performance remote service call
- Supports multiple service routing and load balancing policies
- Supports multiple service registries
- Supports multiple protocols
- Supports multiple invoke type, such as synchronous, oneway, callback, generalized and more.
- Support cluster failover, service warm-up, automatic fault tolerance
- High extensibility for easy to improve features as needed

## Related Projects

- [sofa-rpc-boot-project](https://github.com/alipay/sofa-rpc-boot-projects) SOFABoot projects for SOFARPC, include starter and samples.

## Requirements

Build-time requirement: JDK 7 or above and Maven 3.2.5 or above.

Runtime requirement: JDK 6 or above.


## Documents

- [Getting Started](http://www.sofastack.tech/sofa-rpc/docs/Getting-Started-With-SOFA-Boot)
- [User Guide](http://www.sofastack.tech/sofa-rpc/docs/Programming)
- [Developer Guide](http://www.sofastack.tech/sofa-rpc/docs/How-To-Build)
- [Release Notes](http://www.sofastack.tech/sofa-rpc/docs/ReleaseNotes)
- [Road Map](http://www.sofastack.tech/sofa-rpc/docs/RoadMap)

## Contribution 

[How to Contributing](http://www.sofastack.tech/sofa-rpc/docs/Contributing)

## License

SOFARPC is licensed under the [Apache License 2.0](https://github.com/alipay/sofa-rpc/blob/master/LICENSE), and SOFARPC uses some third-party components, you can view their open source license here [NOTICE](https://github.com/alipay/sofa-rpc/wiki/NOTICE).
