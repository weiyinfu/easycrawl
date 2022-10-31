# This Repo is deprecated.

[项目地址](https://github.com/weiyinfu/easycrawl)

本项目包括：

* 一个多线程Java爬虫框架
* 一个全站克隆工具

项目特点：

* 保证文件能够静态访问，html文件中的路径都是相对路径。 涉及到html文件的链接改写过程，因为不改写链接，本地访问时还是会加载网站上的资源。
* 只能够克隆小型站点，克隆大型站点会导致复杂度太高，内存溢出
* 支持断点续传。
  在爬取得过程中难免会因为关机等原因导致程序终止，下次启动的时候如果只能重新开始，那就太费事了。所以需要实现断点续传，下载一部分之后，随时停止。配置文件中的forceDownload参数控制，如果为true，则启用强制更新，如果为false，则不重复下载已经下载了的内容。
* 充分利用已经下载了的内容体现在两个地方：对于html虽然已经下载过了，但是还是需要解析一遍的，因为这对于克隆其它链接至关重要；对于资源文件，已经下载过了，既没有必要请求，也没有必要重新写入。
* 禁用掉重定向  
  一个大坑：如果没有禁用重定向，会产生死循环。如访问a页面，a包含b，b被重定向到a，那么就会产生a/a/a/a/a...应对这种情况，最简单的解决方法是禁用掉重定向，麻烦点的方法是检测出循环来，如果出现路径循环则停止。
* URL自动去掉hash 如果两个页面路径相同但是hash不同，则认为这两个页面是同一内容。

TODO:

* ignoreHash:两个hash不同的网页是否应该是为同一个网页，否则会下载大量重复页面
* ignoreQuery:两个query不同的网页是否应该视为同一个网页，否则会下载大量重复页面
* 依赖weiyinfu.util,使用自己的property loader加载配置

# jdom的一处问题

resourceUrl=../../../cdn.staticfile.org/twitter-bootstrap/4.3.1/js/bootstrap.min.js
htmlUrl=https://www.suxieban.com/page/note/statement.html
absoluteResourceUrl=https://www.suxieban.com/cdn.staticfile.org/twitter-bootstrap/4.3.1/js/bootstrap.min.js