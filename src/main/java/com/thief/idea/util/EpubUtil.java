package com.thief.idea.util;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Range;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EPUB 电子书解析：基于 epublib 解析容器/OPF/spine 与目录（EPUB2 NCX、EPUB3 nav 均支持），
 * 用 jsoup 把各章 XHTML 转成纯文本行（段落转行、实体解码）。
 * 章节内的 &lt;img&gt; 会把图片字节解包到临时目录，并在正文中插入 [[IMG:文件名]] 占位行，
 * 由阅读界面把占位渲染成行内图片。
 * 同时把每个目录项解析成正文行号，供主阅读界面展示目录并点击跳转。
 **/
public class EpubUtil {

    private EpubUtil() {
    }

    /**
     * 一本书的解析结果：纯文本（UTF-8）+ 目录（已解析成正文行号）+ 图片临时目录（可为 null）
     **/
    public static class EpubBook {
        public final String text;
        public final List<TocEntry> toc;

        /**
         * 解包出的图片临时目录（无图片时为 null），目录内文件名与正文中的 [[IMG:文件名]] 占位对应
         **/
        public final File imageDir;

        public EpubBook(String text, List<TocEntry> toc) {
            this(text, toc, null);
        }

        public EpubBook(String text, List<TocEntry> toc, File imageDir) {
            this.text = text;
            this.toc = toc;
            this.imageDir = imageDir;
        }
    }

    /**
     * 目录项：标题、源 href（含片段，用于提示）、层级深度、在正文中的起始行号
     **/
    public static class TocEntry {
        public final String title;
        public final String href;
        public final int depth;
        public final int line;

        public TocEntry(String title, String href, int depth, int line) {
            this.title = title;
            this.href = href;
            this.depth = depth;
            this.line = line;
        }
    }

    /**
     * 解析 epub，返回正文纯文本与目录
     **/
    public static EpubBook extract(File epub) throws IOException {
        Book book;
        try (InputStream in = new FileInputStream(epub)) {
            book = new EpubReader().readEpub(in);
        }
        List<SpineReference> refs = book.getSpine().getSpineReferences();
        if (refs == null || refs.isEmpty()) {
            throw new IOException("无效的 EPUB 文件：未读取到任何正文");
        }

        // 按 spine 顺序提取正文，记录每章起始行号与文本行（含源偏移，供片段锚点定位）
        StringBuilder sb = new StringBuilder();
        int lineCount = 0;
        String firstKey = null;
        Map<String, Integer> chapterStartLine = new HashMap<>();
        Map<String, List<ExtractedLine>> chapterLines = new HashMap<>();
        Map<String, String> chapterHtml = new HashMap<>();
        // 全书的图片资源（按 href 归一化索引），供章节内 <img> 按相对路径解析
        Map<String, Resource> resourceByHref = new HashMap<>();
        for (Resource r : book.getResources().getAll()) {
            String key = normalize(r.getHref());
            if (key != null && !resourceByHref.containsKey(key)) {
                resourceByHref.put(key, r);
            }
        }
        ImageExport images = new ImageExport(resourceByHref);
        for (SpineReference ref : refs) {
            Resource res = ref.getResource();
            if (res == null) {
                continue;
            }
            String href = res.getHref();
            if (href == null) {
                continue;
            }
            String html;
            try (Reader reader = res.getReader()) {
                html = readAll(reader);
            }
            List<ExtractedLine> lines = htmlToLines(html, images, href);
            if (lines.isEmpty()) {
                continue;
            }
            String key = normalize(href);
            if (firstKey == null) {
                firstKey = key;
            }
            chapterStartLine.put(key, lineCount);
            chapterLines.put(key, lines);
            chapterHtml.put(key, html);
            for (ExtractedLine line : lines) {
                if (lineCount > 0) {
                    sb.append('\n');
                }
                sb.append(line.text);
                lineCount++;
            }
        }
        if (lineCount == 0) {
            throw new IOException("无效的 EPUB 文件：未读取到任何正文");
        }

        List<TocEntry> toc = extractToc(book, chapterStartLine, chapterLines, chapterHtml, firstKey);
        return new EpubBook(sb.toString(), toc, images.dir);
    }

    /**
     * 把解析结果写入临时 txt 文件（UTF-8），返回临时文件
     **/
    public static File writeTempFile(EpubBook book) throws IOException {
        File tmp = File.createTempFile("thief-book-", ".txt");
        tmp.deleteOnExit();
        Files.write(tmp.toPath(), book.text.getBytes(StandardCharsets.UTF_8));
        return tmp;
    }

    /**
     * 把 epub 解包成纯文本并写入临时 txt 文件，返回临时文件
     **/
    public static File convertToText(File epub) throws IOException {
        return writeTempFile(extract(epub));
    }

    /**
     * 提取目录（epublib 已统一 EPUB2 NCX / EPUB3 nav），并解析每项的行号
     **/
    private static List<TocEntry> extractToc(Book book, Map<String, Integer> chapterStartLine,
                                             Map<String, List<ExtractedLine>> chapterLines,
                                             Map<String, String> chapterHtml, String firstKey) {
        List<TocEntry> result = new ArrayList<>();
        List<TOCReference> refs = book.getTableOfContents().getTocReferences();
        if (refs != null) {
            collectToc(refs, 0, chapterStartLine, chapterLines, chapterHtml, firstKey, result);
        }
        return result;
    }

    private static void collectToc(List<TOCReference> refs, int depth, Map<String, Integer> chapterStartLine,
                                   Map<String, List<ExtractedLine>> chapterLines,
                                   Map<String, String> chapterHtml, String firstKey, List<TocEntry> out) {
        for (TOCReference ref : refs) {
            String href = ref.getCompleteHref();
            if (href == null && ref.getResource() != null) {
                href = ref.getResource().getHref();
            }
            String title = ref.getTitle();
            if (title == null) {
                title = "";
            }
            int line = resolveTocLine(href, chapterStartLine, chapterLines, chapterHtml, firstKey);
            if (line >= 0) {
                out.add(new TocEntry(title.trim(), href != null ? href : "", depth, line));
            }
            if (ref.getChildren() != null && !ref.getChildren().isEmpty()) {
                collectToc(ref.getChildren(), depth + 1, chapterStartLine, chapterLines, chapterHtml, firstKey, out);
            }
        }
    }

    /**
     * 把目录项 href（相对 OPF 目录，可能带 #片段）解析成正文行号
     **/
    private static int resolveTocLine(String href, Map<String, Integer> chapterStartLine,
                                      Map<String, List<ExtractedLine>> chapterLines,
                                      Map<String, String> chapterHtml, String firstKey) {
        if (href == null || href.isEmpty()) {
            return -1;
        }
        String path = href;
        String fragment = null;
        int hash = href.indexOf('#');
        if (hash >= 0) {
            path = href.substring(0, hash);
            fragment = href.substring(hash + 1);
        }
        String key = path.isEmpty() ? firstKey : normalize(path);
        if (key == null || !chapterStartLine.containsKey(key)) {
            return -1;
        }
        int line = chapterStartLine.get(key);
        if (fragment == null || fragment.isEmpty()) {
            return line;
        }
        // 片段锚点：用 jsoup 定位 id/name 元素在源码中的字符位置，再映射到最近的文本行
        int anchor = findAnchorPos(chapterHtml.get(key), fragment);
        if (anchor < 0) {
            return line;
        }
        List<ExtractedLine> lines = chapterLines.get(key);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).offset >= anchor) {
                return line + i;
            }
        }
        return line + lines.size() - 1;
    }

    private static int findAnchorPos(String html, String fragment) {
        if (html == null) {
            return -1;
        }
        Document doc = Parser.htmlParser().setTrackPosition(true).parseInput(html, "");
        Element el = doc.getElementById(fragment);
        if (el == null) {
            for (Element e : doc.getElementsByAttribute("name")) {
                if (fragment.equals(e.attr("name"))) {
                    el = e;
                    break;
                }
            }
        }
        if (el == null) {
            return -1;
        }
        Range range = el.sourceRange();
        return range.isTracked() ? range.start().pos() : -1;
    }

    /**
     * XHTML → 纯文本行：用 jsoup 解析，块级元素/br 断行，跳过 head/script/style 内容，
     * 实体由 jsoup 解码；每行记录第一个文本节点在源码中的偏移（供锚点定位）。
     * &lt;img&gt; 会被导出成临时图片文件，并输出一行 [[IMG:文件名]] 占位。
     **/
    static List<ExtractedLine> htmlToLines(String html, ImageExport images, String chapterHref) {
        Document doc = Parser.htmlParser().setTrackPosition(true).parseInput(html, "");
        List<ExtractedLine> lines = new ArrayList<>();
        LineBuffer buffer = new LineBuffer();
        walkLines(doc.body(), buffer, lines, images, chapterHref);
        buffer.flush(lines);
        return lines;
    }

    private static void walkLines(Node node, LineBuffer buffer, List<ExtractedLine> out,
                                  ImageExport images, String chapterHref) {
        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            int pos = textNode.sourceRange().start().pos();
            buffer.append(textNode.text(), pos);
            return;
        }
        if (!(node instanceof Element)) {
            return;
        }
        Element el = (Element) node;
        String name = el.tagName();
        if (name.equals("br")) {
            buffer.flush(out);
            return;
        }
        if (name.equals("img")) {
            // 图片独立成行：导出图片字节并输出占位行
            buffer.flush(out);
            String fileName = images.export(el, chapterHref);
            if (fileName != null) {
                Range range = el.sourceRange();
                int pos = range.isTracked() ? range.start().pos() : 0;
                out.add(new ExtractedLine("[[IMG:" + fileName + "]]", Math.max(pos, 0)));
            }
            return;
        }
        if (isBlock(name)) {
            buffer.flush(out);
        }
        for (Node child : el.childNodes()) {
            walkLines(child, buffer, out, images, chapterHref);
        }
    }

    private static boolean isBlock(String name) {
        return name.equals("p") || name.equals("div") || name.equals("li") || name.equals("tr")
                || name.equals("blockquote") || name.equals("section") || name.equals("article")
                || name.equals("table") || name.equals("ul") || name.equals("ol") || name.equals("dl")
                || name.equals("dt") || name.equals("dd") || name.equals("figure") || name.equals("figcaption")
                || name.equals("h1") || name.equals("h2") || name.equals("h3")
                || name.equals("h4") || name.equals("h5") || name.equals("h6");
    }

    /**
     * 章节内 &lt;img&gt; 的导出：把 src 解析到全书资源，图片字节写入临时目录，
     * 返回临时文件名（正文中以 [[IMG:文件名]] 占位，供阅读界面渲染）。
     * 解析失败（外链/资源缺失/写入失败）返回 null，该图片按无图处理。
     **/
    static class ImageExport {
        private final Map<String, Resource> resourceByHref;
        private File dir;

        ImageExport(Map<String, Resource> resourceByHref) {
            this.resourceByHref = resourceByHref;
        }

        /**
         * 导出 &lt;img&gt; 对应的图片资源，成功返回临时文件名，失败返回 null
         **/
        String export(Element img, String chapterHref) {
            String src = img.attr("src");
            if (src == null || src.isEmpty()) {
                return null;
            }
            String resolved = resolveHref(chapterHref, src);
            if (resolved == null) {
                return null;
            }
            Resource res = resourceByHref.get(normalize(resolved));
            if (res == null) {
                return null;
            }
            byte[] data;
            try {
                data = res.getData();
            } catch (Exception e) {
                return null;
            }
            if (data == null || data.length == 0) {
                return null;
            }
            if (dir == null) {
                try {
                    dir = Files.createTempDirectory("thief-book-img-").toFile();
                } catch (IOException e) {
                    return null;
                }
            }
            String name = safeFileName(normalize(resolved));
            File out = new File(dir, name);
            try {
                if (!out.exists()) {
                    Files.write(out.toPath(), data);
                    out.deleteOnExit();
                }
                return name;
            } catch (IOException e) {
                return null;
            }
        }
    }

    /**
     * 把章节内相对 src 解析成相对 OPF 的路径（处理 ./ 与 ../ 与绝对路径），
     * 外链（http/https/data:）或解析失败返回 null
     **/
    private static String resolveHref(String chapterHref, String src) {
        String s = src.trim();
        if (s.isEmpty() || s.startsWith("http://") || s.startsWith("https://") || s.startsWith("data:")) {
            return null;
        }
        try {
            s = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
        }
        int slash = chapterHref.lastIndexOf('/');
        String combined = (slash >= 0 ? chapterHref.substring(0, slash + 1) : "") + s;
        Deque<String> stack = new ArrayDeque<>();
        for (String part : combined.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
            } else {
                stack.addLast(part);
            }
        }
        if (stack.isEmpty()) {
            return null;
        }
        return String.join("/", stack);
    }

    /**
     * 资源 href → 安全文件名：目录分隔符与非法字符替换成下划线，保留扩展名
     **/
    private static String safeFileName(String key) {
        String name = key.replace('/', '_').replace('\\', '_')
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isEmpty() ? "img" : name;
    }

    /**
     * 按块收集文本行：记录行内第一个文本节点的源码偏移
     **/
    private static class LineBuffer {
        private final StringBuilder text = new StringBuilder();
        private int offset = -1;

        void append(String chunk, int pos) {
            if (offset < 0) {
                offset = pos;
            }
            text.append(chunk);
        }

        void flush(List<ExtractedLine> out) {
            String line = text.toString().trim().replaceAll("\\s+", " ");
            if (!line.isEmpty()) {
                out.add(new ExtractedLine(line, Math.max(offset, 0)));
            }
            text.setLength(0);
            offset = -1;
        }
    }

    static class ExtractedLine {
        final String text;
        final int offset;

        ExtractedLine(String text, int offset) {
            this.text = text;
            this.offset = offset;
        }
    }

    /**
     * 目录 href → 统一格式：URL 解码、反斜杠转正斜杠、去 ./ 前缀、小写（用于匹配章节）
     **/
    private static String normalize(String path) {
        if (path == null) {
            return null;
        }
        String p = path;
        try {
            p = URLDecoder.decode(p, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
        }
        p = p.replace('\\', '/');
        while (p.startsWith("./")) {
            p = p.substring(2);
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p.toLowerCase();
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
