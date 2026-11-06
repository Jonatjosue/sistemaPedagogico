package jonathan.desarrollo.sistemaPedagogico.service;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class MarkdownService {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)      // el HTML crudo del markdown se muestra como texto
            .sanitizeUrls(true)    // bloquea javascript: y similares
            .build();

    public String aHtml(String markdown, Long leccionId) {
        Node doc = parser.parse(markdown);
        doc.accept(new AbstractVisitor() {
            @Override
            public void visit(Image image) {
                String d = image.getDestination();
                boolean relativa = d != null && !d.isBlank()
                        && !d.startsWith("/") && !d.matches("(?i)^[a-z][a-z0-9+.-]*:.*");
                if (relativa) {
                    if (d.startsWith("./")) d = d.substring(2);
                    String nombre = URLEncoder.encode(d, StandardCharsets.UTF_8).replace("+", "%20");
                    image.setDestination("/lecciones/" + leccionId + "/recursos/" + nombre);
                }
                visitChildren(image);
            }
        });
        return renderer.render(doc);
    }
}