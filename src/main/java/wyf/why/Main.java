package wyf.why;

import org.jsoup.Jsoup;
import wyf.easycrawl.*;
import wyf.easycrawl.handler.HttpClientRequestHandler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    static class CallbackHandler implements ResponseHandler {
        private final Scheduler scheduler;
        int x = 0;

        CallbackHandler(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void handle(Request req, Response resp) {
            var s = new String(resp.getData(), Charset.forName("utf8"));
            System.out.println(s);
            if (x++ < 10) {
                System.out.println("now puting a task");
                Request q = new Request();
                q.setCallback(this);
                q.setUrl(req.getUrl());
                scheduler.pushRequest(q);
            }
        }
    }

    static class CallbackHandler2 implements ResponseHandler {
        @Override
        public void handle(Request req, Response resp) {
            var s = new String(resp.getData(), Charset.forName("utf8"));
            System.out.println(s);
        }
    }

    static void one() {
        Scheduler scheduler = new Scheduler(new HttpClientRequestHandler());
        var url = "https://www.suxieban.com/page/note/statement.html";
        var handler = new CallbackHandler(scheduler);
        Request req = new Request();
        req.setCallback(handler);
        req.setUrl(url);
        scheduler.pushRequest(req);
        scheduler.run();
    }

    static void three() {
        var x = new HttpClientRequestHandler();
        var url = "https://www.suxieban.com/page/note/statement.html";
        var handler = new CallbackHandler2();
        Request req = new Request();
        req.setCallback(handler);
        req.setUrl(url);
        for (int i = 0; i < 100; i++) {
            x.handle(req);
        }

    }

    public static void four(RequestHandler requestHandler, String url) {
        var handler = new CallbackHandler2();
        Request req = new Request();
        req.setCallback(handler);
        req.setUrl(url);
        requestHandler.handle(req);
    }

    public static void main(String[] args) throws IOException {
        three();
    }

    void two() throws IOException {
        var html = Files.readString(Paths.get("./bug.html"));
        var doc = Jsoup.parse(html);
        System.out.println(doc.toString());
    }
}
