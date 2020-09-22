# netty-nat

基于netty的TCP请求转发程序

## 简介
    在特定的网络环境或安全审计要求下，我们可能会面临网络被限定为单向访问的情况，本工具可以实现在单向网网络中设置代理从而实现双向访问的目的。

## 依赖/知识准备
- netty-高性能NIO通信框架
- tcp/ip通信基础知识

## 内部通信协议
    [通信协议](www.baidu.com)
    
## 使用方式
1. 使用git克隆项目到本地
2. 修改配置文件
    项目包含两个独立配置文件，分别为
+netty-nat
|__netty-client
    |__properties.properties
|__netty-server
    |__properties.properties
- client对应properties配置:
    internal.server.port            代理程序服务端端口
    internal.server.host            代理程序服务端地址
    proxy.client.port               被代理程序的端口
    proxy.client.host               被代理程序的地址
    internal.channel.init.num       代理程序内部连接池初始大小
    internal.channel.max.idle.num   代理程序内部连接池最大空闲连接数
    internal.channel.min.idle.num   代理程序内部连接池最小空闲连接数
- client对应properties配置:
    proxy.server.port               代理程序开发服务端口
    internal.server.port            代理程序内部连通端口(对应client配置中internal.server.port)
3. 打包
    项目中使用maven管理第三方依赖，打包使用maven-jar-plugin，自定义打包行为定义在项目根目录的assembly.xml中，打包时执行：
```mvn clean package -Dmaven.test.skip=ture```
4. 部署
    项目打包后获得
> 项目名称-版本号.zip
    解压后获得
- 项目主运行jar >  项目名称-版本号.jar
- 项目第三方包依赖目录 > libs
- 项目配置文件目录 > config
    将解压后文件及目录保持当前层级关系上传至服务器
5. 启动
    运行```java -Dlog4j.configuration=file:./config/log4j.properties -jar nat-server-1.0-SNAPSHOT.jar & ```启动服务端
    观察到 InternalServer started on port xxxx......即表示服务启动成功
    运行```java -Dlog4j.configuration=file:./config/log4j.properties -jar nat-client-1.0-SNAPSHOT.jar & ```启动客户端

