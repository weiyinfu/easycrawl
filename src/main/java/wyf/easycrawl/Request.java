package wyf.easycrawl;

import java.util.Map;

public class Request {
String url;
Map<String, Object> meta;
ResponseHandler callback;

public String getUrl() {
    return url;
}

public void setUrl(String url) {
    this.url = url;
}

public Map<String, Object> getMeta() {
    return meta;
}

public void setMeta(Map<String, Object> meta) {
    this.meta = meta;
}

public ResponseHandler getCallback() {
    return callback;
}

public void setCallback(ResponseHandler callback) {
    this.callback = callback;
}
}
