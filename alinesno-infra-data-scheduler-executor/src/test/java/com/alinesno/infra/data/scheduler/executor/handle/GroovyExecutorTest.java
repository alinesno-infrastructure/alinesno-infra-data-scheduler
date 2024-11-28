package com.alinesno.infra.data.scheduler.executor.handle;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class GroovyExecutorTest {

    @Test
    public void execute() {
        String rawScript = """
                @Grapes([
                    @Grab(group='org.jsoup', module='jsoup', version='1.14.3')
                ])
                
                import groovy.util.logging.Slf4j
                import org.jsoup.Jsoup
                import org.jsoup.nodes.Document
                import org.jsoup.nodes.Element
                import org.jsoup.select.Elements
                
                @Slf4j
                class WebCrawler {

                               
                    Set<String> visitedLinks = new HashSet<>()
                    int maxDepth
                    String targetDomain
                    List<PageInfoBean> pageInfoList = new ArrayList<>()

                               
                    WebCrawler(String startUrl, int maxDepth) {
                        this.maxDepth = maxDepth
                        try {
                            URL url = new URL(startUrl)
                            this.targetDomain = url.host
                        } catch (IOException e) {
                            log.error("无效的起始URL: $startUrl", e)
                        }
                    }

                               
                    void crawl(String url, int currentDepth) {
                        if (url.lastIndexOf("#") != -1) {
                            url = url.substring(0, url.lastIndexOf("#"))
                        }

                               
                        if (currentDepth > maxDepth || !visitedLinks.add(url)) {
                            return
                        }

                               
                        URL linkUrl = new URL(url)
                        if (!linkUrl.host.equals(targetDomain)) {
                            return
                        }

                               
                        Document doc = Jsoup.connect(url).timeout(120 * 1000).get()
                        processDocument(doc, currentDepth, url)

                               
                        Elements linksOnPage = doc.select("a[href]")
                        for (Element link : linksOnPage) {
                            String nextLink = link.absUrl("href")
                            crawl(nextLink, currentDepth + 1)
                        }
                    }

                               
                    private void processDocument(Document doc, int currentDepth, String url) {
                        String title = doc.title()
                        String pageText = doc.body().text()

                               
                        log.debug("正在抓取: {} , 标题: {} , depth: {}", url, title, currentDepth)

                               
                        pageInfoList.add(new PageInfoBean(url, title, pageText, currentDepth))
                    }
                }

                               
                class PageInfoBean {

                               
                    String title
                    String link
                    int depth
                    String content

                               
                    PageInfoBean(String link, String title, String content, int depth) {
                        this.link = link
                        this.title = title
                        this.content = content
                        this.depth = depth
                    }

                               
                    @Override
                    String toString() {
                        "PageInfoBean{title=$title, link=$link, depth=$depth, content=$content}"
                    }
                }

                               
                // 主程序入口
                def startUrl = "http://portal.infra.linesno.com"
                def maxDepth = 10

                           
                WebCrawler crawler = new WebCrawler(startUrl, maxDepth)
                crawler.crawl(startUrl, 0)

                           
                // 输出爬取的结果
                crawler.pageInfoList.each { pageInfo ->
                    println log.info("--->> {}" , pageInfo.toString())
                }
                """;

        // 创建 Binding 对象，用于绑定变量到 Groovy 脚本
        Binding binding = new Binding();

        binding.setVariable("log", log);

        // 创建 GroovyShell 实例
        GroovyShell shell = new GroovyShell(GroovyExecutor.class.getClassLoader(), binding);

        // 执行 Groovy 脚本
        shell.evaluate(rawScript) ;
    }

}
