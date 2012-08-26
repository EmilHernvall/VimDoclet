package at.quench.vimdoclet;

import java.io.*;
import java.util.*;

import com.sun.javadoc.*;

public class DocGenerator
{
    private static class TagFileEntry implements Comparable<TagFileEntry>
    {
        String file;
        String name;
        String excmd;

        public int compareTo(TagFileEntry b)
        {
            return name.compareTo(b.name);
        }
    }

    private Set<TagFileEntry> tagFile;
    private File outDir;
    private CommentParser parser;

    public DocGenerator(File outDir)
    {
        this.outDir = outDir;

        this.tagFile = new TreeSet<TagFileEntry>();
        this.parser = new CommentParser();
    }

    public void generateTagsFile()
    throws IOException
    {
        File tagsFile = new File(outDir, "tags");
        PrintWriter tagsWriter = new PrintWriter(new FileWriter(tagsFile));

        for (TagFileEntry entry : tagFile) {
            tagsWriter.println(entry.name + "\t" + entry.file + "\t" + entry.excmd);
        }

        tagsWriter.flush();
        tagsWriter.close();
    }

    public void processClass(ClassDoc currentClass)
    throws IOException
    {
        String fileName = 
            currentClass.qualifiedName().replaceAll("\\.", "_") + ".txt";

        String classSearchName = 
            "*" + currentClass.qualifiedName().replaceAll("\\.","_");

        TagFileEntry classTagEntry = new TagFileEntry();
        classTagEntry.file = fileName;
        classTagEntry.name = currentClass.name();
        classTagEntry.excmd = "/" + classSearchName;
        tagFile.add(classTagEntry);

        File outFile = new File(outDir, fileName);

        PrintWriter writer = new PrintWriter(new FileWriter(outFile));

        writer.println(rightPad(classSearchName));
        writer.println();

        writer.println(currentClass.qualifiedName());
        writer.println();

        writer.println(parser.parse(currentClass.commentText()));
        writer.println();

        writer.println("==============================================================================");
        writer.println("Index");
        writer.println();

        ClassDoc[] innerClasses = currentClass.innerClasses();
        if (innerClasses.length > 0) {
            for (ClassDoc childClass : currentClass.innerClasses()) {
                writer.println("class " + childClass.name());
            }
            writer.println();
        }

        for (FieldDoc field : currentClass.fields()) {
            writer.println(field.type() + " " + field.name() + " = " + field.constantValue());
        }
        writer.println();

        for (ConstructorDoc constructor : currentClass.constructors()) {
            writer.println(constructor.name() + constructor.signature());
        }
        writer.println();

        for (MethodDoc method : currentClass.methods()) {
            writer.println(method.returnType() + " " + method.name() + method.signature());
        }
        writer.println();

        writer.println("==============================================================================");
        writer.println("Constructors");
        writer.println();

        for (ConstructorDoc constructor : currentClass.constructors()) {
            writer.println(constructor.name() + constructor.signature());

            Type[] thrownEx = constructor.thrownExceptions();
            if (thrownEx.length > 0) {
                writer.print("throws ");
                String delim = "";
                for (Type type : thrownEx) {
                    writer.print(delim + type);
                    delim = ", ";
                }
                writer.println();
            }

            writer.println();
            writer.println(parser.parse(constructor.commentText()));
            writer.println();
        }
        writer.println();

        writer.println("==============================================================================");
        writer.println("Methods");
        writer.println();

        int i = 0;
        for (MethodDoc method : currentClass.methods()) {
            String methodSearchName = classSearchName + i;

            //writer.println("==============================================================================");
            writer.println(rightPad(methodSearchName));
            writer.println();

            writer.println(method.returnType() + " " + method.name() + method.signature());

            Type[] thrownEx = method.thrownExceptions();
            if (thrownEx.length > 0) {
                writer.print("throws ");
                String delim = "";
                for (Type type : thrownEx) {
                    writer.print(delim + type);
                    delim = ", ";
                }
                writer.println();
            }

            writer.println();
            writer.println(parser.parse(method.commentText()));
            writer.println();

            TagFileEntry methodTagEntry = new TagFileEntry();
            methodTagEntry.file = fileName;
            methodTagEntry.name = method.name();
            methodTagEntry.excmd = "/" + methodSearchName;
            tagFile.add(methodTagEntry);

            i++;
        }

        writer.flush();
        writer.close();
    }

    private static String rightPad(String data)
    {
        int width = 78;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 78 - data.length(); i++) {
            buf.append(" ");
        }
        buf.append(data);
        return buf.toString();
    }
}
