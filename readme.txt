这是一个代码自动生成工具，目前支持DAO层代码生成。后续也可以加上其他需要代码生成的地方，以解放工程师的重复劳动。
1、工具使用：
工程使用先决条件：安装Java、Maven、Eclipse、Spring环境。
下载代码后，到工程根目录（即此文件所在目录），运行eclipse.bat（为MVN命令，系统需要安装Maven），然后导入到Eclipse工程中。
2、工程结构为：
a.src/main/java 为工程Java代码
	gencode.dao表示的是DAO层的生成代码工具，其中dao/Gen.java为主运行文件，输入参数设置见main函数。
	gencode.util为一些通用的方法。
b.src/main/resources 为工程代码生成模版配置文件。
	dao 为一级目录，表示生成DAO数据库访问层的代码模版。目前有两套模版:ibatisdao为通用的Ibatis模版，taobaodao为淘宝DAO层模版。也可以自己设定想要的模版。
c.gendir 为工程代码生成输出目录，根据模版可直接导入Eclipse运行DAO工程。
d.logdir 为工程日志输出目录。
3、DAO层代码生成工具使用：
首先，到dao根目录中的*db.properties属性文件中设置好数据库连接参数。
其次，到Gen主运行程序中。（目前仅支持Mysql和Oracle两种类型数据库。）
也可以自己制作模版文件(VM文件采用UTF-8编码)。可参考resources中的dao/ibatisdao例子。
4、工程设计:
通过Velocity模版批量生成类似的代码，例如：DAO访问。
根据DB中获取的表结构信息生成XML类型的DB结构及DDL语句。
dao/Gen.java 为主入口，根据由dao/GenTable.java生成List<TableBean>对象生成DAO全套代码，同时调用dao/GenDB.java可生成db信息(如果是DB链接则生成xml和ddl,如果是XML配置则生成ddl)。
dao/GenDB.java 根据由dao/GenTable.java生成List<TableBean>对象生成DBXML结构配置和ddl)。
dao/GenTable.java 根据DB链接配置或DBXML结构配置生成List<TableBean>对象。
dao/DbConn.java 提供数据库链接操作类。
dao/SysInit.java 系统参数初始化类，主要是初始化db.properties。
dao/FieldMapping.java 字段映射类，处理不同数据库之间与Java的类型转换。
dao/bean/GlobalBean.java 全局变量类，Put到Velocity模版中的Context中使用。
dao/bean/TableBean.java 表结构对象变量类，Put到Velocity模版中的Context中使用。
dao/bean/ColBean.java 表字段对象变量类，Put到Velocity模版中的Context中使用。
dao/bean/IndexBean.java 表索引对象变量类，Put到Velocity模版中的Context中使用。

附录1：需要做什么？
1、需要按规范设计表结构及注释。查看：http://code.google.com/p/codeauto/wiki/DataBaseDesign
2、需要设置好DB配置信息。查看：src\main\resources\dao\db.properties

附录2：还不能做什么？
1、暂不支持外键。
2、暂不支持分库分表。
3、暂不支持事务。

附录3：正在完成什么？
1、表结构的反向设计。
2、Eclipse插件嵌入。
3、Maven插件嵌入。



数据库设计规范

目的：
1、规范数据库的表结构设计，便于通过自动DAO生成工具，标准的生成代码。
2、合理规范的设计数据库，同时便于交流。

规范内容：

一、设计要素
1、数据库名
通常有登录名(schema)和数据库实例名(name)组合成一个逻辑数据库，例如：dbuser 表示用户数据库。
数据库名后不加复数"s"。所有的表名，字段名也都不加复数。因为本身就是存储记录集合的数据库。
2、表名
一般表名根据逻辑数据库的前缀（也可以取个有意义的前缀，如：user）。
Oracle中表名一般用大写，如：USER_INFO，Mysql中表名一般用小写，如：user_info。
3、表注释
表需要有注释，一般是说明表中存储的记录实体名词，如：用户信息。（后面不要加“表”字，如不可写为：用户信息表。）
表注释用于自动生成DAO时的注释用。
4、字段
主键一般用ID作为命名，自动增长的数据类型。不表示其他意义。注释：主键ID
每张表都需要gmt_create,gmt_modified字段，表示记录创建时间和记录最后更新时间。
status为int型，表示记录的有效状态。-1 表示逻辑删除。其他>0 的整数可表示其他需要的意义。
5、字段注释
一般是说明表中存储的字段实体意义名词，如：用户名。
如果表示类型的字段写上类型意义，如：有效状态：1: 有效，-1: 无效。
表注释用于自动生成DO时的注释用。


附录：
数据库连接信息：数据库机器IP,数据库实例名(name),登录名(schema),登录密码。
数据库修改需要及时反馈补充到数据库设计文档中来（如：PowerDesign文档）。