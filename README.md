# PrintScan

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

![](https://img.shields.io/badge/Maintained-yes-green.svg)
![](https://img.shields.io/badge/Ask%20me-anything-1abc9c.svg)

![](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![](https://img.shields.io/badge/Amazon_AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)


## 목차
[1. **프로젝트 소개**](#프로젝트-소개)

[2. **개발 환경**](#개발-환경)

[3. **설계 구조**](#설계-구조)

[4. **상세 기능**](#상세-기능)

[5. **업데이트 진행**](#업데이트-진행)


## 프로젝트 소개
* 해당 프로젝트는 스프링부트 기반 웹 어플리케이션이며, 다음 목표를 달성하기 위하여 제작되었습니다.  
  * Zebra 프린트, 스캐너 연동
  * Mobile Based Application(Flutter Based) 연계
  * 기타 소규모 MES 출력, 생산 관리
* 개발 효율성을 향상시키기 위하여 다음과 같은 인프라가 활용, 구축되었습니다.  
  * Docker 를 이용한 컨테이너 어플리케이션 형식으로 구성되었습니다.  
  * Amazon Web Service 환경을 이용한 클라우딩 기술을 활용하여 구축하였습니다.  
* 해당 프로젝트는 [MIT 라이센스](https://choosealicense.com/licenses/mit/) 기반 배포 및 운영됩니다. 



## 개발 환경

- 코드 작성 환경 구성
  * VSCode
    * 유료화 툴 사용을 가급적 배제 및 사용자 커스터마이징 포퍼먼스에 맞는 에디트 툴 선택
  * DBeaver
    * 이클립스 기반 무료 툴 중 가장 Reference 다수 보유한 DB 에디트 툴 선택

- 어플리케이션 환경 구성
  * OpenJDK17
    * Java 8 EOL 예정에 따라 이후 LTS 버전 중 가장 최신 버전을 선택
  * Spring Boot 3.3.10
    * 웹 프레임워크 초기 구축 효율화를 위해 SpringBoot 활용
    * 3.0 버전의 개발 및 운영 환경 확대에 따라 3.0 버전으로 소스 마이그레이션 및 개발 진행
  * Maven 4.0.0
    * 빌드 툴 안정성 및 하위 호환성 고려 최신 버전 4.0.0 선택

  
- 운영 환경 구성
  * Amazon Web Service (Ubuntu 20.04 LTS)
    * AWS EC2 인스턴스를 활용하여 서버 구축 및 어플리케이션 배포 
      * Local 환경에서 구축시 상시 서버 기기 구동이 필요하므로 클라우드 서버로 대체
      * GCP, Heroku, Naver Cloud, Gabia 등 여러 서비스 확인하였으나,  
         실제 운영 서버 구축시 추가적으로 발생할 인프라 확장성을 고려 AWS 기반 인프라로 선택
    * AWS RDS 를 활용하여 클라우딩 환경에서 데이터베이스 구축
  * Springboot Embded Apache Tomcat
    * 소규모 어플리케이션 구동시 가용성 및 추가적인 설정 고려 해당 WAS 선택

## 설계 구조

- 코드 구조  
  
  * 프로젝트 구조는 메이븐에서 제시하는 권장 템플릿 구조로 진행하였습니다.

  
- 운영 환경 구조
  * 기본적으로 AWS IaaS 기반으로 구성되며 이를 통하여 어플리케이션을 설정하였습니다.  
  * 어플리케이션 서버는 Small 기반으로 구성하였습니다.(Docker 기반 빌드 시 가용성 고려)  
  * AWS Route 53 을 활용한 DNS 도메인을 적용하였으며, 이를 활용하기 위하여 ELB 를 통한 라우팅 기능을 구성하였습니다.  
  * HTTPS 적용하여 일반적인 사이트에서 제공하는 사용자 보안 편의성을 제공합니다.
  * 내부적으로 Spring MVC 구조를 채택하여 맵핑 후 RESTFul 한 서비스 URL 제공이 가능하도록 구성하였습니다.  
  * 서버에 접근 가능한 대상은 포트로 구분하여 AWS VPC 를 세팅, 구성하였습니다.   

## 상세 기능
- 작성 진행중

## 업데이트 진행
  - Ver 1.000(25.03.24)
    - 깃 배포