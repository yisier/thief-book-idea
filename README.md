![b](https://s2.ax1x.com/2019/12/25/lFCd41.jpg "b")

### 之前看到有网友开发了一款PC端和VS Code插件版的小说阅读器（摸鱼神器Thief-Book），原创[链接地址](https://github.com/cteams/Thief-Book "链接地址")，但是很可惜的是没有IDEA版的，最近刚好比较闲，按照他的原型开发出了类似功能的IDEA插件。

------------
# 2020-09-04日更新记录
一年多没有更新，我胡汉三又回来了，哈哈哈，本次更新主要有一下几点内容，感谢 https://github.com/mine-riko/ 小哥的创意

**1. 界面优化**

(1) 设置界面 `File | Settings | Other Settings | Thief-Book Config`

![set](https://github.com/yisier/thief-book-idea/blob/master/img/setting.png?raw=true "set")

(2) 主界面

![main](https://raw.githubusercontent.com/yisier/thief-book-idea/master/img/main.png "main")

**2.功能优化**

1) 更新setting设置后，如切换书本、修改字体等，不需要重启，点击主页面的刷新按钮即可生效;

2) 优化翻页速度;

3) 新增老板键，Ctrl + Shift + ↓，即可隐藏主页面;

4) 新增了每页行数、行间距设置;

5) 移除了自动翻页功能;

6) 新增的刷新按钮，在切换书本或更新了设置后，点击此按钮立即生效设置（以前的版本需要重启）。


**如果出现书本乱码的问题，可以创建一个空的 "utf-8" 编码的txt文件，然后将你的书本内容复制进来，注意字体一定要选择系统中存在的，windows推荐使用微软雅黑**


------------
# 2019-07-26日

# 效果图
![t](https://s2.ax1x.com/2019/12/25/lFC6De.gif "t")

![2](https://s2.ax1x.com/2019/12/25/lFChCt.jpg "2")

# 下载地址
[链接](https://github.com/yisier/thief-book-idea/releases/download/V0.1.1/thief-book-idea.jar "链接")


------------

## 使用教程
1).到release中下载jar包;

2).打开IDEA,找到setting 中的plugin，点击Install Plugin from disk选择下载的jar包，安装并重启IDEA;

3).进入IDEA的Setting页面，找到Other Settings\Thief-Book Config选项，选择txt文件;

4).愉快的摸鱼吧！！！！


## 未截图演示的功能:
1).阅读进度是实时保存的；

2).进度栏是可以输入的，回车跳转到输入的行数；

3).精简模式下会隐藏上下翻页按钮；

4).当你不小心将窗口关闭时，可以在WIndow菜单下选择show thief重新打开;


## 目前存在的问题：
~~1).设置页面的配置项必须在重启 IDEA 后才会生效；~~ 已解决

~~2).点击上页按钮的速度比较慢(还没找到原因)；~~ 已解决

~~3).单线程的程序，书本体积较大时，会出现卡顿；~~ 已优化

~~4).精简模式下，上页翻页按钮的热键会失效；~~ 已删除

5).热键不可以自定义(后面有空再改进)；

6).部分书本可能会出现乱码，解决方式:将书本以 utf-8 格式编码；

