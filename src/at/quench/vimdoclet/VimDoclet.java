package at.quench.vimdoclet;

import java.io.*;
import java.util.*;

import com.sun.javadoc.*;

public class VimDoclet
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

    public static boolean start(RootDoc root)
    {
        // Options
        String outdir = null;

        String[][] options = root.options();
        for (String[] param : options) {
            if ("-outdir".equals(param[0]) && param.length == 2) {
                outdir = param[1];
                continue;
            }

            System.err.print("Unrecognized parameter: ");
            for (String arg : param) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }

        if (outdir == null) {
            System.err.println("You need to specify -outdir.");
            return false;
        }

        try {
            File mainDir = new File(outdir);
            if (!mainDir.exists()) {
                mainDir.mkdirs();
            }

            if (!mainDir.isDirectory()) {
                System.err.println("-outdir should be a directory.");
                return false;
            }

            ClassDoc[] classes = root.classes();
            List<TagFileEntry> tagFileEntries = new ArrayList<TagFileEntry>();
            for (ClassDoc currentClass : classes) {
                processClass(currentClass, mainDir, tagFileEntries);
            }

            // Tags file
            File tagsFile = new File(mainDir, "tags");
            PrintWriter tagsWriter = new PrintWriter(new FileWriter(tagsFile));

            Collections.sort(tagFileEntries);
            for (TagFileEntry entry : tagFileEntries) {
                tagsWriter.println(entry.name + "\t" + entry.file + "\t" + entry.excmd);
            }

            tagsWriter.flush();
            tagsWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public static int optionLength(String option)
    {
        if ("-outdir".equals(option)) {
            return 2;
        }

        return 0;
    }

    private static void processClass(ClassDoc currentClass, File outDir, List<TagFileEntry> tagFile)
    throws IOException
    {
        String fileName = currentClass.qualifiedName().replaceAll("\\.", "_") + ".txt";

        String classSearchName = "*" + currentClass.qualifiedName().replaceAll("\\.","_");

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

        writer.println(currentClass.commentText());
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
                for (Type type : method.thrownExceptions()) {
                    writer.print(delim + type);
                    delim = ", ";
                }
                writer.println();
            }

            writer.println();
            writer.println(method.commentText());
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
