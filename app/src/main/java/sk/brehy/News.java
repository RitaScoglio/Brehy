package sk.brehy;

import java.util.Comparator;

public class News{

    String title, href, content_list, content, date;
    int node;

    News(String node, String title, String href, String date, String content_list, String content){
        this.node = Integer.parseInt(node.replaceAll("\\D+",""));
        this.title = title;
        this.href = href;
        this.date = date;
        this.content_list = content_list;
        this.content = content;
    }

    public int getNode() {
        return node;
    }

    public String getTitle() {
        return title;
    }

    public String getHref() {
        return href;
    }

    public String getDate() {
        return date;
    }

    public String getContent_list() {
        return content_list;
    }

    public String getContent() {
        return content;
    }

    public static Comparator<News> nodeComparator = new Comparator<News>() {
        @Override
        public int compare(News jc1, News jc2) {
            return (Integer.compare(jc2.getNode(), jc1.getNode()));
        }
    };
}
