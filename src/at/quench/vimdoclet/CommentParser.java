package at.quench.vimdoclet;

import java.util.*;
import java.util.regex.*;

import org.htmlparser.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.*;

public class CommentParser extends NodeVisitor
{
    private static class ParserExpression
    {
        Pattern pattern;
        String replace;

        public ParserExpression(String pattern, String replace)
        {
            this.pattern = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
            this.replace = replace;
        }

        public String replace(String in)
        {
            try {
                Matcher matcher = pattern.matcher(in);
                return matcher.replaceAll(replace);
            } catch (Exception e) {
                throw new RuntimeException("Pattern: " + pattern, e);
            }
        }
    }

    private static class TableCell
    {
        String[] lines;
        int width;
        int height;

        public TableCell(String data)
        {
            lines = data.split("\n");
            height = lines.length;
            width = 0;
            for (String line : lines) {
                width = Math.max(width, line.length());
            }
        }
    }

    private static class TableRow
    {
        List<TableCell> cells;
        int height;

        public TableRow()
        {
            cells = new ArrayList<TableCell>();
            height = 0;
        }

        public void add(StringBuilder buffer)
        {
            String data = buffer.toString().trim();
            TableCell cell = new TableCell(data);
            height = Math.max(height, cell.height);
            cells.add(cell);
        }

        public int getCellCount() { return cells.size(); }
    }

    private final static ParserExpression[] expressions
        = new ParserExpression[] {
            new ParserExpression("^ ", ""),
            new ParserExpression("\\{@link\\s+([^ \n}]+?)\\s+([^}]+?)\\}", "$2 (*$1)"),
            new ParserExpression("\\{@link ([^ \n}]+?)\\}", "*$1")
        };

    private final static ParserExpression[] htmlExpr
        = new ParserExpression[] {
            new ParserExpression("&nbsp;", "&"),
            new ParserExpression("&amp;", "&"),
            new ParserExpression("&lt;", "<"),
            new ParserExpression("&gt;", ">"),
            new ParserExpression("&#92;", "\\\\")
        };

    private StringBuilder buffer;
    private Set<String> tagStack;

    private List<TableRow> table;
    private TableRow row;
    private StringBuilder cell;

    public CommentParser()
    {
    }

    public String parse(String html)
    {
        for (ParserExpression expression : expressions) {
            html = expression.replace(html);
        }

        try { 
            Parser parser = Parser.createParser(html, "UTF-8"); 
            NodeList nodes = parser.parse(null);

            buffer = new StringBuilder();
            tagStack = new HashSet<String>();
            nodes.visitAllNodesWith(this);

            return buffer.toString();
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visitTag(Tag tag)
    {
        String tagName = tag.getTagName().toLowerCase();
        tagStack.add(tagName);

        if ("table".equals(tagName)) {
            table = new ArrayList<TableRow>();
        }
        else if ("tr".equals(tagName)) {
            row = new TableRow();
        }
        else if ("td".equals(tagName) || "th".equals(tagName)) {
            cell = new StringBuilder();
        }
    }

    @Override
    public void visitEndTag(Tag tag)
    {
        try {
            String tagName = tag.getTagName().toLowerCase();

            tagStack.remove(tagName);
            /*String stackTag = tagStack.pop();
            if (!stackTag.equals(tag.getTagName().toLowerCase())) {
                tagStack.push(stackTag);
            }*/

            if ("tr".equals(tagName)) {
                table.add(row);
            }
            else if ("td".equals(tagName) || "th".equals(tagName)) {
                row.add(cell);
            }
            else if ("table".equals(tagName)) {

                // calculate max number of columns per row
                int cols = 0;
                for (TableRow row : table) {
                    cols = Math.max(cols, row.getCellCount());
                }

                // calculate maximum width of a column across all rows
                int[] colWidth = new int[cols];
                for (int i = 0; i < cols; i++) {
                    colWidth[i] = 0;
                }

                for (TableRow row : table) {
                    int i = 0;
                    for (TableCell cell : row.cells) {
                        colWidth[i] = Math.max(colWidth[i], cell.width);
                        i++;
                    }
                }

                // print the table
                for (TableRow row : table) {

                    // process the row one line at a time
                    int totalWidth = 0;
                    for (int i = 0; i < row.height; i++) {
                        int j = 0;
                        buffer.append("| ");
                        totalWidth = 1;
                        for (TableCell cell : row.cells) {
                            totalWidth += colWidth[j] + 3;
                            String line = i < cell.lines.length ? cell.lines[i] : "";
                            line = leftPad(line, colWidth[j++]+1);
                            buffer.append(line);
                            buffer.append("| ");
                        }
                        buffer.append("\n");
                    }

                    for (int i = 0; i < totalWidth; i++) {
                        buffer.append("-");
                    }
                    buffer.append("\n");
                }

                /*for (TableRow row : table) {
                    buffer.append("row:");
                    for (TableCell cell : row.cells) {
                        buffer.append("cell:");
                        for (String line : cell.lines) {
                            buffer.append("line:");
                            buffer.append(line);
                            buffer.append("\n");
                        }
                    }
                }*/
            }
        } catch (EmptyStackException e) {
        }
    }

    @Override
    public void visitStringNode(Text text)
    {
        StringBuilder activeBuffer;
        if (tagStack.contains("td") || tagStack.contains("th")) {
            activeBuffer = cell;
        } else if (tagStack.contains("table")) {
            return;
        } else {
            activeBuffer = buffer;
        }

        String textStr = clean(text.getText());
        if (tagStack.contains("blockquote")) {
            String[] lines = textStr.split("\n");
            for (String line : lines) {
                activeBuffer.append("    ");
                activeBuffer.append(line);
                activeBuffer.append("\n");
            }
        }
        else {
            activeBuffer.append(textStr);
        }
    }

    private static String clean(String html)
    {
        for (ParserExpression expression : htmlExpr) {
            html = expression.replace(html);
        }

        return html;
    }

    private static String leftPad(String data, int width)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(data);
        for (int i = 0; i < width-data.length(); i++) {
            buf.append(" ");
        }
        return buf.toString();
    }
}
