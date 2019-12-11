package wyf.easycrawl;
/**
 * 继承此接口实现自己的下载器
 * */
public interface RequestHandler {
void handle(Request req);
}
