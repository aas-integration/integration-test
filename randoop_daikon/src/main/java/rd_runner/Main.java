package rd_runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.objectweb.asm.ClassReader;

import rd_runner.util.DaikonRunner;
import rd_runner.util.RandoopRunner;

public class Main {

	public static String basePath = "./";
	
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("use with classpath, classdir, and testDir as arguments.");
			return;
		}
		String classPath = args[0];
		final File classDir = new File(args[1]);
		final File testDir = new File(args[2]);

		if (args.length == 4) {
			basePath = args[3];
		}
		
		Set<String> classes = getClasses(classDir);
		// run randoop
		File classListFile = null;
		final String randoopClassPath = classPath + File.pathSeparator + classDir.getAbsolutePath();
		try {
			classListFile = createClassListFile(classes);
			RandoopRunner rr = new RandoopRunner();
			rr.run(randoopClassPath, classListFile, testDir, 10, 20);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (classListFile != null && !classListFile.delete()) {
				throw new RuntimeException("failed to clean up");
			}
		}

		DaikonRunner dr = new DaikonRunner();
		List<String> cp = new LinkedList<String>();
		cp.add(randoopClassPath);
		cp.add(basePath+"lib/daikon.jar");		
		cp.add(testDir.getAbsolutePath());		
		final String daikonClassPath = StringUtils.join(cp, File.pathSeparator);
		dr.run(daikonClassPath, "RegressionTestDriver", classes);

	}

	private static File createClassListFile(Set<String> classes) throws IOException {
		File classListFile = File.createTempFile("clist", "txt");
		try (OutputStream streamOut = new FileOutputStream(classListFile);
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(streamOut, "UTF-8"))) {
			for (String className : classes) {
				writer.println(className);
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		return classListFile;
	}

	public static Set<String> getClasses(final File classDir) {
		Set<String> classes = new HashSet<String>();
		for (Iterator<File> iter = FileUtils.iterateFiles(classDir, new String[] { "class" }, true); iter
				.hasNext();) {
			File classFile = iter.next();
			try (FileInputStream is = new FileInputStream(classFile);) {
				ClassReader cr = new ClassReader(is);
				classes.add(cr.getClassName().replace('/', '.'));
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		return classes;
	}

}