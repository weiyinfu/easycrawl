package wyf.clonesite;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wyf.easycrawl.Request;
import wyf.easycrawl.Response;
import wyf.easycrawl.ResponseHandler;
import wyf.easycrawl.Scheduler;
import wyf.easycrawl.handler.HttpClientRequestHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CloneSite {

    Logger logger = LoggerFactory.getLogger(CloneSite.class);

    List<String> seed;//爬虫种子URL
    //html网页前缀
    List<String> prefix;//爬取网址的前缀
    List<String> prefixBlackList;//爬取网址的前缀黑名单
    Path targetFolder;//存储的目标文件夹
    Scheduler scheduler;//调度器
    Charset charset;//保存文本文件的字体
    boolean forceDownload = false;//是否强制下载


    public CloneSite() {
        init();
    }

    public CloneSite(List<String> seed, List<String> prefix, List<String> prefixBlackList, Path targetFolder, Charset charset, boolean forceDownload) {
        this.seed = seed;
        this.prefix = prefix;
        this.prefixBlackList = prefixBlackList;
        this.targetFolder = targetFolder;
        this.charset = charset;
        this.forceDownload = forceDownload;
    }

    void start() {
        scheduler = new Scheduler(new HttpClientRequestHandler());
//    scheduler = new Scheduler(new UrlConnectionRequestHandler(true));
        for (String seedUrl : seed) {
            Request req = new Request();
            req.setCallback(handler);
            req.setUrl(seedUrl);
            scheduler.pushRequest(req);
        }
        scheduler.run();
    }

    List<String> urlList(String s) {
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> x.length() > 0)
                .collect(Collectors.toList());
    }

    void init() {
        Properties p = new Properties();
        try {
            BufferedReader cin = Files.newBufferedReader(Paths.get("clonesite.properties"), StandardCharsets.UTF_8);
            p.load(cin);
            var configFolder = p.getProperty("target");
            if (configFolder.startsWith("~/")) {
                configFolder = configFolder.replaceFirst("^~", System.getProperty("user.home"));
            }
            this.targetFolder = Paths.get(configFolder);
            this.prefix = urlList(p.getProperty("prefix"));
            this.prefixBlackList = urlList(p.getProperty("forbidPrefix"));
            this.seed = urlList(p.getProperty("seed"));
            this.charset = Charset.forName(p.getProperty("charset"));
            this.forceDownload = Boolean.parseBoolean(p.getProperty("forceDownload"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean isInvalidPathChar(char var0) {
        return var0 < ' ' || "<>:\"|?*".indexOf(var0) != -1;
    }

    boolean isInvalidPath(String path) {
        for (int i = 0; i < path.length(); i++) if (isInvalidPathChar(path.charAt(i))) return true;
        return false;
    }

    /**
     * 将URL转化为本地的path，用于将网页内容保存到本地
     *
     * @param url：绝对路径url
     * @param type:文件类型，用于决定保存成的后缀名称
     */

    Path url2path(String url, String type) {
        int beg = url.indexOf(":") + 3;
        String path = url.substring(beg);

        //如果文件名包含不合法字符，那么使用hashcode
        if (isInvalidPath(path)) {
            path = path.hashCode() + "";
        }
        if (type != null && !path.endsWith("." + type)) {
            path += '.' + type;
        }
        return targetFolder.resolve(path);
    }

    /**
     * now表示当前html网页url，resource表示资源文件url，返回二者的相对位置
     * resourceType表示是否强制resourceURL发生变化
     */
    void show() {
        try {
            throw new Exception("baga");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(-1);
    }

    String path2relative(String htmlUrl, String resourceUrl, String resourceType) {
        var shouldVisitResource = shouldVisit(resourceUrl);
        if (!shouldVisitResource) {
            return resourceUrl;
        }
        var htmlLocalPath = url2path(htmlUrl, "html");
        var resourceLocalPath = url2path(resourceUrl, resourceType);
        return htmlLocalPath.getParent().relativize(resourceLocalPath).toString().replace('\\', '/');
    }

    /**
     * 递归创建目录，用于创建文件，这里必须要同步，否则会
     * 导致多线程爬虫同时创建文件夹，从而抛出异常
     */
    synchronized void mkdir(Path p) {
        p = p.toAbsolutePath();
        if (Files.exists(p)) return;
        if (Files.notExists(p.getParent())) mkdir(p.getParent());
        try {
            Files.createDirectory(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存文本文件
     */
    void writeFile(Path path, String content, Charset encoding) {
        if (forceDownload || Files.notExists(path)) {
            mkdir(path.getParent());
            try (BufferedWriter cout = Files.newBufferedWriter(path, encoding)) {
                cout.write(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存二进制文件
     */
    void writeFile(Path path, byte[] data) {
        if (forceDownload || Files.notExists(path)) {
            mkdir(path.getParent());
            try (OutputStream cout = Files.newOutputStream(path)) {
                cout.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class RequestMaterialType {
        public static final String binary = "binary";
        public static final String html = "html";
    }

    void pushRequest(String url, String type) {
        if (shouldVisit(url)) {
            Request req = new Request();
            req.setUrl(url);
            Map<String, Object> ma = new HashMap<>();
            ma.put("type", type);
            req.setMeta(ma);
            req.setCallback(handler);
            pushRequest(req);
        }
    }

    void pushRequest(Request req) {
        Path p = url2path(req.getUrl(), (String) req.getMeta().get("type"));
        if (!shouldVisit(req.getUrl())) return;
        //如果强制更新或者不存在该文件，那么就下载该文件
        if (forceDownload || Files.notExists(p)) {
            scheduler.pushRequest(req);
        } else {
            if (scheduler.hasVisited(req.getUrl())) return;
            scheduler.putVisited(req.getUrl());
            logger.info(req.getUrl() + "已经存在，无需请求" + p.getFileName().toAbsolutePath());
            Response resp = new Response();
            try {
                resp.setData(Files.readAllBytes(p));
            /*
            大坑预警：这里不能在单线程里面直接handle
            对于已经处理的文件，都无需下载，直接在此函数中处理
            会导致递归太深
            使用异步处理的方式可以解决递归陷阱
            * req.getCallback().handle(req, resp);
            * */
                scheduler.pushRunnable(() -> {
                    req.getCallback().handle(req, resp);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析doc里面的包含src元素的资源文件
     */
    void src(String url, Document doc) {
        String src[] = new String[]{"script", "svg", "img"};
        for (String j : src) {
            for (Element i : doc.select(j)) {
                if (!i.hasAttr("src")) continue;
                String s = i.absUrl("src");
                if (s.trim().length() == 0) continue;
                i.attr("src", path2relative(url, s, null));
                pushRequest(s, "binary");
            }
        }
    }

    int getLayerCount(String s) {
        int ans = 0;
        while (true) {
            if (s.startsWith("../", ans)) {
                ans += 3;
            } else {
                break;
            }
        }
        return ans / 3;
    }

    /**
     * 解析doc里面包含href的资源文件
     */
    void hrefOfResource(String url, Document doc) {
        String href[] = new String[]{"link"};
        for (String j : href) {
            for (Element i : doc.select(j)) {
                if (!i.hasAttr("href")) continue;
                String s = i.absUrl("href");
                if (s.trim().length() == 0) continue;
                i.attr("href", path2relative(url, s, null));
                pushRequest(s, RequestMaterialType.binary);
            }
        }
    }

    /**
     * 是否应该访问某个链接
     * 访问条件：命中白名单且未命中黑名单
     */
    boolean shouldVisit(String url) {
        for (String should : prefix) {
            if (url.startsWith(should)) {
                for (String forbid : prefixBlackList) {
                    if (url.startsWith(forbid)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析doc中的超链接
     */
    void hrefOfHtml(String url, Document doc) {
        for (Element i : doc.select("a")) {
            if (i.hasAttr("href")) {
                String s = i.absUrl("href");
                if (s.trim().length() == 0) continue;
                i.attr("href", path2relative(url, s, "html"));
                pushRequest(s, RequestMaterialType.html);
            }
        }
    }

    ResponseHandler handler = new ResponseHandler() {
        @Override
        public void handle(Request req, Response resp) {
            logger.info(req.getUrl() + "请求结束");
            if (req.getMeta() != null && req.getMeta().get("type").equals("binary")) {
                writeFile(url2path(req.getUrl(), null), resp.getData());
            } else {
                String html = new String(resp.getData(), charset);
                Document doc = Jsoup.parse(html, req.getUrl());
                {
                    src(req.getUrl(), doc);
                    hrefOfResource(req.getUrl(), doc);
                    hrefOfHtml(req.getUrl(), doc);
                }
                var filepath = url2path(req.getUrl(), "html");
                writeFile(filepath, doc.html(), doc.charset());
                logger.info(String.format("saving %s=>%s", req.getUrl(), filepath));
            }
        }
    };

    public static void main(String[] args) throws Exception {
        CloneSite ha = new CloneSite();
        ha.start();
    }
}
