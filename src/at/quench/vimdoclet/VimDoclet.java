package at.quench.vimdoclet;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.sun.javadoc.*;

public class VimDoclet
{
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

            DocGenerator generator = new DocGenerator(mainDir);

            ClassDoc[] classes = root.classes();
            for (ClassDoc currentClass : classes) {
                generator.processClass(currentClass);
            }

            generator.generateTagsFile();
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

}
