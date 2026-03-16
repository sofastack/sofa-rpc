# Changelog

All notable changes to SOFARPC will be documented in this file.

---

## [v5.14.2](https://github.com/sofastack/sofa-rpc/releases/tag/v5.14.2) - 2026-03-11

**SOFARPC v5.14.2** is an enhancement release that focuses on fixing several issues in the Triple protocol, including POJO generic calls, Tracer context propagation, and optimizing the remote IP retrieval logic. This release improves the stability and reliability of the framework. (Requires JDK 8)

### Enhancement
- Enhanced remote IP retrieval logic — improved the way remote IP addresses are obtained in the Triple protocol ([#1529](https://github.com/sofastack/sofa-rpc/pull/1529))

### Fix
- Fixed Triple POJO overload issue — resolved method overload matching problems when calling generics in POJO mode with the Triple protocol ([#1523](https://github.com/sofastack/sofa-rpc/pull/1523))
- Fixed Triple generic error — fixed issues with Triple generic calls and unit test errors ([#1531](https://github.com/sofastack/sofa-rpc/pull/1531))
- Fixed Triple Tracer context cross-thread propagation issue — solved the problem of tracer context being lost in cross-thread scenarios ([#1539](https://github.com/sofastack/sofa-rpc/pull/1539))

### Chore
- Fixed two flaky CI tests in test-integration ([#1534](https://github.com/sofastack/sofa-rpc/pull/1534))

**Full Changelog**: [v5.14.1...v5.14.2](https://github.com/sofastack/sofa-rpc/compare/v5.14.1...v5.14.2)

---

## [v5.14.1](https://github.com/sofastack/sofa-rpc/releases/tag/v5.14.1) - 2025-11-17

Enhanced the sofa-rpc framework and fixed some bugs (requires JDK 8).

### Feature
- Support deadline when RPC call ([#1503](https://github.com/sofastack/sofa-rpc/pull/1503))

### Fix
- Fix triple shared channel remove when RPC sendMsg issue ([#1521](https://github.com/sofastack/sofa-rpc/pull/1521))
- Fix biz exception cast issue ([#1522](https://github.com/sofastack/sofa-rpc/pull/1522))

**Full Changelog**: [v5.14.0...v5.14.1](https://github.com/sofastack/sofa-rpc/compare/v5.14.0...v5.14.1)

---

## [v5.14.0](https://github.com/sofastack/sofa-rpc/releases/tag/v5.14.0) - 2025-10-18

Enhanced the sofa-rpc framework and fixed some bugs (requires JDK 8).

### Feature
- Optimize dynamic config: integrate Zookeeper & Nacos, support interface-level dynamic config ([#1430](https://github.com/sofastack/sofa-rpc/pull/1430))
- Support custom `UserThreadPool` for interface method ([#1500](https://github.com/sofastack/sofa-rpc/pull/1500))
- Support custom triple header size & modify default max header size to 64 KB ([#1509](https://github.com/sofastack/sofa-rpc/pull/1509), [#1510](https://github.com/sofastack/sofa-rpc/pull/1510))

### Fix
- Fix Hessian deserialize to support `sofa.serialize.dynamic.load.enable` ([#1463](https://github.com/sofastack/sofa-rpc/pull/1463))
- Fix exception status code mapping in `FailFastCluster` trace logging ([#1504](https://github.com/sofastack/sofa-rpc/pull/1504))
- Fix sharedChannel concurrent destroy problem ([#1513](https://github.com/sofastack/sofa-rpc/pull/1513))

### Chore
- Bump `org.apache.cxf:cxf-core` from 3.5.8 to 3.5.11 ([#1497](https://github.com/sofastack/sofa-rpc/pull/1497))
- Update nexusUrl ([#1515](https://github.com/sofastack/sofa-rpc/pull/1515), [#1516](https://github.com/sofastack/sofa-rpc/pull/1516), [#1518](https://github.com/sofastack/sofa-rpc/pull/1518))

**Full Changelog**: [v5.13.5...v5.14.0](https://github.com/sofastack/sofa-rpc/compare/v5.13.5...v5.14.0)

---

## [v5.13.5](https://github.com/sofastack/sofa-rpc/releases/tag/v5.13.5) - 2025-06-02

Enhanced the sofa-rpc framework and fixed some bugs (requires JDK 8). Recommended upgrade for versions between v5.13.0 and v5.13.4.

### Enhancement
- Support triple stream event tracer ([#1488](https://github.com/sofastack/sofa-rpc/pull/1488))

**Full Changelog**: [v5.13.4...v5.13.5](https://github.com/sofastack/sofa-rpc/compare/v5.13.4...v5.13.5)

---

## [v5.13.4](https://github.com/sofastack/sofa-rpc/releases/tag/v5.13.4) - 2025-04-28

Enhanced the sofa-rpc framework and fixed some bugs (requires JDK 8). Recommended upgrade for versions between v5.13.0 and v5.13.3.

### Fix
- Fix triple service uninstall does not clean completely issue in serverless scene ([#1487](https://github.com/sofastack/sofa-rpc/pull/1487))

**Full Changelog**: [v5.13.3...v5.13.4](https://github.com/sofastack/sofa-rpc/compare/v5.13.3...v5.13.4)

---

## [v5.13.3](https://github.com/sofastack/sofa-rpc/releases/tag/v5.13.3) - 2025-03-13

Enhanced the sofa-rpc framework and fixed some bugs (requires JDK 8). Recommended upgrade for versions between v5.13.0 and v5.13.2.

### Enhancement
- Enhance triple stream tracer ([#1477](https://github.com/sofastack/sofa-rpc/pull/1477))
- Add a cache for missing classes to improve the performance of RPC deserialization ([#1479](https://github.com/sofastack/sofa-rpc/pull/1479))

**Full Changelog**: [v5.13.2...v5.13.3](https://github.com/sofastack/sofa-rpc/compare/v5.13.2...v5.13.3)

---

## [v5.13.2](https://github.com/sofastack/sofa-rpc/releases/tag/v5.13.2) - 2024-10-17

Added some features, enhanced the sofa-rpc framework, and fixed some bugs (requires JDK 8).

### Enhancement
- Update Hessian version to 3.5.5 ([#1445](https://github.com/sofastack/sofa-rpc/pull/1445))
- Remove lookout dependency ([#1447](https://github.com/sofastack/sofa-rpc/pull/1447))
- Enhance code quality ([#1453](https://github.com/sofastack/sofa-rpc/pull/1453))
- Bump `commons-io:commons-io` from 2.7 to 2.14.0 ([#1457](https://github.com/sofastack/sofa-rpc/pull/1457))

**Full Changelog**: [v5.13.1...v5.13.2](https://github.com/sofastack/sofa-rpc/compare/v5.13.1...v5.13.2)

---

## [v5.13.1](https://github.com/sofastack/sofa-rpc/releases/tag/v5.13.1) - 2024-08-20

Added some features, enhanced the sofa-rpc framework, and fixed some bugs (requires JDK 8).

### Enhancement
- Jackson serialization support multi-classloader ([#1438](https://github.com/sofastack/sofa-rpc/pull/1438))
- Enhance log format ([#1436](https://github.com/sofastack/sofa-rpc/pull/1436))
- Bump Hessian from 3.5.3 to 3.5.4 and Tracer from 3.0.8 to 3.1.6 ([#1439](https://github.com/sofastack/sofa-rpc/pull/1439))

**Full Changelog**: [v5.13.0...v5.13.1](https://github.com/sofastack/sofa-rpc/compare/v5.13.0...v5.13.1)

---

## [v5.13.0](https://github.com/sofastack/sofa-rpc/releases/tag/v5.13.0) - 2024-05-22

Added some features, enhanced the sofa-rpc framework, and fixed some bugs (requires JDK 8).

### Feature
- Support Triple POJO mode stream call ([#1360](https://github.com/sofastack/sofa-rpc/pull/1360))
- Support Kubernetes extension registry ([#1395](https://github.com/sofastack/sofa-rpc/pull/1395))
- Support bzip2 and gzip compression ([#1400](https://github.com/sofastack/sofa-rpc/pull/1400))

**Full Changelog**: [v5.12.0...v5.13.0](https://github.com/sofastack/sofa-rpc/compare/v5.12.0...v5.13.0)

---

## [v5.12.0](https://github.com/sofastack/sofa-rpc/releases/tag/v5.12.0) - 2024-01-22

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.11.1.

### Feature
- Add Fury serializer ([#1348](https://github.com/sofastack/sofa-rpc/pull/1348), [#1387](https://github.com/sofastack/sofa-rpc/pull/1387))
- Support Jackson configured by env or spring application properties ([#1371](https://github.com/sofastack/sofa-rpc/pull/1371))

**Full Changelog**: [v5.11.1...v5.12.0](https://github.com/sofastack/sofa-rpc/compare/v5.11.1...v5.12.0)

---

## [v5.11.1](https://github.com/sofastack/sofa-rpc/releases/tag/v5.11.1) - 2023-09-11

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.10.1.

### Enhancement
- Update security dependencies ([#1366](https://github.com/sofastack/sofa-rpc/pull/1366))

**Full Changelog**: [v5.11.0...v5.11.1](https://github.com/sofastack/sofa-rpc/compare/v5.11.0...v5.11.1)

---

## [v5.11.0](https://github.com/sofastack/sofa-rpc/releases/tag/v5.11.0) - 2023-08-29

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8).

### Enhancement
- Update security dependencies ([#1354](https://github.com/sofastack/sofa-rpc/pull/1354))
- SOFABoot 4.0 support ([#1356](https://github.com/sofastack/sofa-rpc/pull/1356))
- Fix macOS aarch64 compilation ([#1357](https://github.com/sofastack/sofa-rpc/pull/1357))

**Full Changelog**: [v5.10.1...v5.11.0](https://github.com/sofastack/sofa-rpc/compare/v5.10.1...v5.11.0)

---

## [v5.10.1](https://github.com/sofastack/sofa-rpc/releases/tag/5.10.1) - 2023-06-16

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.10.0.

### Feature
- Support changing gRPC `maxInboundMessageSize` ([#1333](https://github.com/sofastack/sofa-rpc/pull/1333))

### Enhancement
- Bump Hessian from 3.3.13 to 3.4.0 ([#1338](https://github.com/sofastack/sofa-rpc/pull/1338))

**Full Changelog**: [v5.10.0...5.10.1](https://github.com/sofastack/sofa-rpc/compare/v5.10.0...5.10.1)

---

## [v5.10.0](https://github.com/sofastack/sofa-rpc/releases/tag/v5.10.0) - 2023-04-20

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.9.2.

### Compatibility Note
- Bump Javassist to `3.28.0-GA`; `3.24.0-GA` is required at minimum.

### Enhancement
- Custom serializer registration ([#1296](https://github.com/sofastack/sofa-rpc/pull/1296))
- Modify the parsing header method to facilitate expansion ([#1302](https://github.com/sofastack/sofa-rpc/pull/1302))

**Full Changelog**: [v5.9.2...v5.10.0](https://github.com/sofastack/sofa-rpc/compare/v5.9.2...v5.10.0)

---

## [v5.9.2](https://github.com/sofastack/sofa-rpc/releases/tag/v5.9.2) - 2023-03-02

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.9.0.

### Enhancement
- Bump `commons-fileupload` from 1.3.3 to 1.5 ([#1309](https://github.com/sofastack/sofa-rpc/pull/1309))

### Fix
- Fix JSON serialization and deserialization bugs ([#1311](https://github.com/sofastack/sofa-rpc/pull/1311))

**Full Changelog**: [v5.9.1...v5.9.2](https://github.com/sofastack/sofa-rpc/compare/v5.9.1...v5.9.2)

---

## [v5.9.1](https://github.com/sofastack/sofa-rpc/releases/tag/v5.9.1) - 2023-01-06

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.9.0.

### Feature
- Support Prometheus metrics ([#1280](https://github.com/sofastack/sofa-rpc/pull/1280))

### Enhancement
- Triple async call support trace log ([#1282](https://github.com/sofastack/sofa-rpc/pull/1282))

**Full Changelog**: [v5.9.0...v5.9.1](https://github.com/sofastack/sofa-rpc/compare/v5.9.0...v5.9.1)

---

## [v5.9.0](https://github.com/sofastack/sofa-rpc/releases/tag/v5.9.0) - 2022-11-13

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.8.7.

### Feature
- Triple invoke support callback & future ([#1249](https://github.com/sofastack/sofa-rpc/pull/1249))
- Add RPC generic throw exception support ([#1259](https://github.com/sofastack/sofa-rpc/pull/1259))

### Enhancement
- Bump Netty version ([#1261](https://github.com/sofastack/sofa-rpc/pull/1261))

**Full Changelog**: [v5.8.8...v5.9.0](https://github.com/sofastack/sofa-rpc/compare/v5.8.8...v5.9.0)

---

## [v5.8.8](https://github.com/sofastack/sofa-rpc/releases/tag/v5.8.8) - 2022-10-26

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.8.7.

### Enhancement
- Improve list difference ([#1266](https://github.com/sofastack/sofa-rpc/pull/1266))

### Fix
- Fix DomainRegistry init problem ([#1269](https://github.com/sofastack/sofa-rpc/pull/1269))

**Full Changelog**: [v5.8.7...v5.8.8](https://github.com/sofastack/sofa-rpc/compare/v5.8.7...v5.8.8)

---

## [v5.8.7](https://github.com/sofastack/sofa-rpc/releases/tag/v5.8.7) - 2022-08-24

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.8.5.

### Enhancement
- Add timeout to `SofaRequest` ([#1224](https://github.com/sofastack/sofa-rpc/pull/1224))
- Modify API for expansion ([#1229](https://github.com/sofastack/sofa-rpc/pull/1229))
- Enable Maven cache to speed up CI workflow ([#1233](https://github.com/sofastack/sofa-rpc/pull/1233))

**Full Changelog**: [v5.8.6...v5.8.7](https://github.com/sofastack/sofa-rpc/compare/v5.8.6...v5.8.7)

---

## [v5.8.6](https://github.com/sofastack/sofa-rpc/releases/tag/v5.8.6) - 2022-08-11

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.8.5.

### Enhancement
- Allow custom caller app name ([#1215](https://github.com/sofastack/sofa-rpc/issues/1215), [#1226](https://github.com/sofastack/sofa-rpc/pull/1226))
- Add DomainRegistry to support direct URL ([#1206](https://github.com/sofastack/sofa-rpc/pull/1206))

### Fix
- Fix Triple class loader problem in multi-classloader environment ([#1216](https://github.com/sofastack/sofa-rpc/pull/1216))

**Full Changelog**: [v5.8.5...v5.8.6](https://github.com/sofastack/sofa-rpc/compare/v5.8.5...v5.8.6)

---

## [v5.8.5](https://github.com/sofastack/sofa-rpc/releases/tag/v5.8.5) - 2022-05-20

Enhancements to the sofa-rpc framework and some bug fixes (requires JDK 8). Recommended upgrade for versions between 5.7.10 and 5.8.4.

### Enhancement
- Bump Dubbo from 2.6.7 to 2.6.9 ([#1184](https://github.com/sofastack/sofa-rpc/pull/1184))

### Fix
- Fix concurrency problem in `DequeLocal` ([#1203](https://github.com/sofastack/sofa-rpc/pull/1203))

**Full Changelog**: [v5.8.4...v5.8.5](https://github.com/sofastack/sofa-rpc/compare/v5.8.4...v5.8.5)

---

For older release notes, please see the [GitHub Releases page](https://github.com/sofastack/sofa-rpc/releases).
