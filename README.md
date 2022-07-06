# netty-nat ![image](https://img.shields.io/github/v/release/Likedan130/netty-nat.svg)

基于netty的TCP/Http请求转发代理程序

## 简介
　在特定的网络环境或安全审计要求下，我们可能会面临网络被限定为单向访问的情况，本工具可以实现在单向网络中设置代理从而实现双向访问的目的。可简单用作内网穿透工具。也可以配置成LVS负载均衡服务  

## 依赖/知识准备
- netty-高性能NIO通信框架
- tcp/ip通信基础知识

## 工具原理说明
1. [通信协议](doc/代理程序通信协议.docx)
-----
2. 程序运行时序图![image](doc/netty-nat%E6%97%B6%E5%BA%8F%E5%9B%BE.png)
-----
3. 网络拓扑图![image](doc/netty-nat%E7%BD%91%E8%B7%AF%E6%8B%93%E6%89%91%E5%9B%BE.png)
   
## 使用方式
1. **将项目克隆到本地**  
2. **修改配置文件**  
    项目包含两个独立配置文件，分别为  
    &nbsp;netty-nat  
    &nbsp;|--netty-client  
    &nbsp;|----properties.yml  
    &nbsp;|--netty-server  
    &nbsp;|----properties.yml  
    
    - client对应properties配置:  
    
      ```yaml
      #内部连接池大小，内部连接只需转发外部与被代理服务间业务数据，可重用通道
      internal:
        channel:
          init:
            num: 10
      #服务端ip和端口
        server:
          host: 127.0.0.1
          port: 8083
      #隧道信息，一条完整的外部>>服务端>>客户端>>被代理服务间的通路称为隧道
      tunnel:
      #tunnel示例，代理本地的mysql数据库服务和nacos服务
          #服务端监听端口
        - serverPort: 9000
          #客户端连接的被代理服务端口
          clientPort: 3306
          #客户端连接的被代理服务端口
          clientHost: 127.0.0.1
        - serverPort: 9001
          clientPort: 8848
          clientHost: 127.0.0.1
      #接入请求的接入密码
      password: '123456' 
      ```
    
    - server对应properties配置:  
    
      ```yaml
      #内部通信使用的端口，需要与客户端的internal.server.port值保持一致
      internal:
        server:
          port: 8083
      #接入请求的接入密码
      password: '123456'
      ```
3. **打包**  
    项目中使用maven管理第三方依赖，打包使用maven-jar-plugin，自定义打包行为定义在项目根目录的assembly.xml中，打包时执行：  
    ```mvn clean package -Dmaven.test.skip=true```
4. **部署**  
 项目打包后获得  项目名称-版本号.zip  
     解压后获得  
    
    - 项目主运行jar  项目名称-版本号.jar  
    - 项目第三方包依赖目录  libs  
 - 项目配置文件目录  config  
 将解压后文件及目录保持当前层级关系上传至服务器
5. **启动**  
    - 直接运行jar  
    调整resources目录下的log4j2.xml和properties.yml为当前服务器相关配置，执行打包编译流程，获得target目录下的tar文件并解压
    运行```java -jar nat-server-1.0-SNAPSHOT.jar & ```启动服务端  
    观察到 InternalServer started on port xxxx......即表示服务启动成功  
    运行```java -jar nat-client-1.0-SNAPSHOT.jar & ```启动客户端  
    - docker启动
    调整resources目录下的log4j2.xml和properties.yml为当前服务器相关配置，执行打包编译流程
    命令行执行docker-compose up -d，在docker中确认容器的运行情况
